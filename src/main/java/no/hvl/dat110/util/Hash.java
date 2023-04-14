package no.hvl.dat110.util;

/**
 * exercise/demo purpose in dat110
 * @author tdoy
 *
 */

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {


	public static BigInteger hashOf(String entity) throws NoSuchAlgorithmException {

		BigInteger hashint = null;

		// Task: Hash a given string using MD5 and return the result as a BigInteger.


		// we use MD5 with 128 bits digest
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");

		// compute the hash of the input 'entity'
		messageDigest.update(entity.getBytes());
		byte[] digets = messageDigest.digest();

		// convert the hash into hex format
		String entityHexed = toHex(digets);

		// convert the hex into BigInteger
		hashint = new BigInteger(entityHexed,16);

		// return the BigInteger

		return hashint;
	}

	public static BigInteger addressSize() throws NoSuchAlgorithmException {

		// Task: compute the address size of MD5
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");


		// compute the number of bits = bitSize()
		int numberOfBits = bitSize();

		// compute the address size = 2 ^ number of bits
		double addressSize = Math.pow(2,numberOfBits);

		// return the address size
		BigDecimal bigDecimal = new BigDecimal(addressSize);

		return bigDecimal.toBigInteger();
	}

	//Tok MessageDigest i parameteren
	public static int bitSize() {

		int digestlen = 16;

		// find the digest length

		return digestlen*8;
	}

	public static String toHex(byte[] digest) {
		StringBuilder strbuilder = new StringBuilder();
		for(byte b : digest) {
			strbuilder.append(String.format("%02x", b&0xff));
		}
		return strbuilder.toString();
	}

}
