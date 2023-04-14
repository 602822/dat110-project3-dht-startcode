/**
 *
 */
package no.hvl.dat110.chordoperations;

import java.math.BigInteger;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.middleware.Message;
import no.hvl.dat110.middleware.Node;
import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.Util;

/**
 * @author tdoy
 */
public class ChordLookup {

	private static final Logger logger = LogManager.getLogger(ChordLookup.class);
	private final Node node;

	public ChordLookup(Node node) {
		this.node = node;
	}



	//Skjer ein infinite loop som gjør at det blir Stackoverflow
	public NodeInterface findSuccessor(BigInteger key) throws RemoteException, NotBoundException, NoSuchAlgorithmException {
		// ask this node to find the successor of key
		//  NodeInterface successorForKey = this.node.findSuccessor(key);
		// get the successor of the node
		NodeInterface successorForNode = node.getSuccessor();

		NodeInterface stub = Util.getProcessStub(successorForNode.getNodeName(), successorForNode.getPort());


		// check that key is a member of the set {nodeid+1,...,succID} i.e. (nodeid+1 <= key <= succID) using the checkInterval
		//Veit ikkje om eg skal sammenligne med successoren til key eller sucessoren til node????????

		boolean inInterval = Util.checkInterval(key, node.getNodeID().add(new BigInteger("1")), successorForNode.getNodeID());
		if (stub != null && inInterval) {
			return successorForNode;  // veit ikkje kas successor eg skal returnere
		} else {
			NodeInterface highestPredecessor = findHighestPredecessor(key);
			return highestPredecessor.findSuccessor(key);
		}


		// if logic returns t
		// rue, then return the successor

		// if logic returns false; call findHighestPredecessor(key)

		// do highest_pred.findSuccessor(key) - This is a recursive call until logic returns true

	}

	/*
	 * This method makes a remote call. Invoked from a local client
	 *
	 * @param ID BigInteger
	 * @return
	 * @throws RemoteException
	 */
	private NodeInterface findHighestPredecessor(BigInteger ID) throws RemoteException, NotBoundException, NoSuchAlgorithmException {

		// collect the entries in the finger table for this node
		List<NodeInterface> fingerTable = this.node.getFingerTable();

		// starting from the last entry, iterate over the finger table


		ListIterator<NodeInterface> iterator = fingerTable.listIterator(fingerTable.size());
		while (iterator.hasPrevious()) {
			NodeInterface finger = iterator.previous();
			NodeInterface stub = Util.getProcessStub(finger.getNodeName(), finger.getPort());
			assert stub != null;
			boolean inInterval = Util.checkInterval(finger.getNodeID(),node.getNodeID().add(new BigInteger("1")), ID.subtract(new BigInteger("1")));
			if (inInterval) {
				return stub;
			}
		}
		// for each finger, obtain a stub from the registry

		// check that finger is a member of the set {nodeID+1,...,ID-1} i.e. (nodeID+1 <= finger <= key-1) using the ComputeLogic

		// if logic returns true, then return the finger (means finger is the closest to key)

		return node;
	}

	public void copyKeysFromSuccessor(NodeInterface succ) {

		Set<BigInteger> filekeys;
		try {
			// if this node and succ are the same, don't do anything
			if (succ.getNodeName().equals(node.getNodeName()))
				return;

			logger.info("copy file keys that are <= " + node.getNodeName() + " from successor " + succ.getNodeName() + " to " + node.getNodeName());

			filekeys = new HashSet<>(succ.getNodeKeys());
			BigInteger nodeID = node.getNodeID();

			for (BigInteger fileID : filekeys) {

				if (fileID.compareTo(nodeID) <= 0) {
					logger.info("fileID=" + fileID + " | nodeID= " + nodeID);
					node.addKey(fileID);                                                            // re-assign file to this successor node
					Message msg = succ.getFilesMetadata().get(fileID);
					node.saveFileContent(msg.getNameOfFile(), fileID, msg.getBytesOfFile(), msg.isPrimaryServer());            // save the file in memory of the newly joined node
					succ.removeKey(fileID);                                                                                    // remove the file key from the successor
					succ.getFilesMetadata().remove(fileID);                                                                    // also remove the saved file from memory
				}
			}

			logger.info("Finished copying file keys from successor " + succ.getNodeName() + " to " + node.getNodeName());
		} catch (RemoteException e) {
			logger.error(e.getMessage());
		}
	}

	public void notify(NodeInterface pred_new) throws RemoteException, NoSuchAlgorithmException {

		NodeInterface pred_old = node.getPredecessor();

		// if the predecessor is null accept the new predecessor
		if (pred_old == null) {
			node.setPredecessor(pred_new);        // accept the new predecessor

		} else if (pred_new.getNodeName().equals(node.getNodeName())) {
			node.setPredecessor(null);

		} else {
			BigInteger nodeID = node.getNodeID();
			BigInteger pred_oldID = pred_old.getNodeID();

			BigInteger pred_newID = pred_new.getNodeID();

			// check that pred_new is between pred_old and this node, accept pred_new as the new predecessor
			// check that ftsuccID is a member of the set {nodeID+1,...,ID-1}
			boolean cond = Util.checkInterval(pred_newID, pred_oldID.add(BigInteger.ONE), nodeID.add(BigInteger.ONE));
			if (cond) {
				node.setPredecessor(pred_new);        // accept the new predecessor
			}
		}
	}

}
