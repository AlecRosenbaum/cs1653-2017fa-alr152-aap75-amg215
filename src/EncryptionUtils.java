import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public abstract class EncryptionUtils {

	public static byte[] encrypt(SecretKey key, Serializable obj) {
		try {
			final Cipher c = Cipher.getInstance("AES", "BC");
            c.init(Cipher.ENCRYPT_MODE, key);
			return c.doFinal(serialize(obj));
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public static Object decrypt(SecretKey key, byte[] cypherText) {
		try {
			final Cipher c = Cipher.getInstance("AES", "BC");
            c.init(Cipher.DECRYPT_MODE, key);
			return deserialize(c.doFinal(cypherText));
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(obj);
		return b.toByteArray();		
	}

	public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		ByteArrayInputStream b = new ByteArrayInputStream(bytes);
		ObjectInputStream o = new ObjectInputStream(b);
		return o.readObject();
	}


	/**
	 * Sign a string. Expected usage: signing a token string:
	 *
	 * {@code EncryptionUtils.sign(mytoken.stringify(), myRSAKeyPair.getPrivate()) }
	 *
	 * @param      plaintext  The plaintext
	 * @param      key        The RSA private key
	 *
	 * @return     the signature
	 */
	public static byte[] sign(final String plaintext, final PrivateKey key) throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException  {
		Signature sig = Signature.getInstance("SHA256withRSA", "BC");
		sig.initSign(key);
		sig.update(plaintext.getBytes());

		return sig.sign();
	}

	/**
	 * Verify a signature. Expected usage: verifying a token string:
	 *
	 * {@code EncryptionUtils.verify(signature, mytoken.stringify(), groupRSAKeyPair.getPublic()) }
	 *
	 * @param      signature  The signature
	 * @param      plaintext  The plaintext
	 * @param      key        The RSA private key
	 *
	 * @return     True if valid, false otherwise
	 */
	public static boolean verify(final byte[] signature, final String plaintext, final PublicKey key) {
		try {
			Signature sig = Signature.getInstance("SHA256withRSA", "BC");
			sig.initVerify(key);
			sig.update(plaintext.getBytes());
			return sig.verify(signature);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return false;
		}
	}

	public static PublicKey getRSAPublicKeyFromFile(String filename) throws FileNotFoundException {
		PublicKey pk = null;
		File f = new File(filename);
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		byte[] keyBytes = new byte[(int)f.length()];
		try {
			dis.readFully(keyBytes);
			dis.close();
			fis.close();

			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
			pk = kf.generatePublic(spec);
			return pk;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}

	}

	public static PrivateKey getRSAPrivateKeyFromFile(String filename) throws FileNotFoundException {
		PrivateKey pk = null;
		File f = new File(filename);
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		byte[] keyBytes = new byte[(int)f.length()];
		try {
			dis.readFully(keyBytes);
			dis.close();
			fis.close();

			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
			pk = kf.generatePrivate(spec);
			return pk;
		}  catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public static byte[] pbkdf2(String password, byte[] salt) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
		return pbkdf2(password.toCharArray(), salt, 4096, 256);
	}

	/**
	 *  Computes the PBKDF2 hash of a password.
	 *
	 * @param   password    the password to hash.
	 * @param   salt        the salt
	 * @param   iterations  the iteration count (slowness factor)
	 * @param   bits        the length of the hash to compute in bits
	 * @return              the PBDKF2 hash of the password
	 */
	public static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bits) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bits);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256", "BC");
		return skf.generateSecret(spec).getEncoded();
	}

	public static String generateRandomString(int length) {
		// this will work up to 32 characters
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, length);
    }

}