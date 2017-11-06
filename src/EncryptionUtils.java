import java.io.*;
import javax.crypto.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;

public abstract class EncryptionUtils {

	public static byte[] encrypt(SecretKey key, Serializable obj) {
		try {
			return serialize(obj);
		} catch (Exception e) {
			return null;
		}
	}

	public static Object decrypt(SecretKey key, byte[] cypherText) {
		try {
			return deserialize(cypherText);
		} catch (Exception e) {
			return null;
		}
	}

	public static byte[] serialize(Object obj) throws IOException {
		try(ByteArrayOutputStream b = new ByteArrayOutputStream()) {
			try(ObjectOutputStream o = new ObjectOutputStream(b)) {
				o.writeObject(obj);
			}
			return b.toByteArray();
		}
	}

	public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
			try(ObjectInputStream o = new ObjectInputStream(b)) {
				return o.readObject();
			}
		}
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
	public static byte[] sign(final String plaintext, final PrivateKey key) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException  {
		Signature sig = Signature.getInstance("SHA256withRSA");
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
			Signature sig = Signature.getInstance("SHA256withRSA");
			sig.initVerify(key);
			sig.update(plaintext.getBytes());
			return sig.verify(signature);
		} catch (Exception e) {
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
			KeyFactory kf = KeyFactory.getInstance("RSA");
			pk = kf.generatePublic(spec);
			return pk;
		} catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
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
			KeyFactory kf = KeyFactory.getInstance("RSA");
			pk = kf.generatePrivate(spec);
			return pk;
		}  catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

}