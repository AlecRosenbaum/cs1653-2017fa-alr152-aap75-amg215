import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import javax.crypto.SecretKey;
import java.lang.RuntimeException;

public class Envelope implements java.io.Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -7726335089122193103L;
	private String msg;
	private ArrayList<Object> objContents = new ArrayList<Object>();
	private int n;
	private byte[] hmac;

	public Envelope(String text) {
		msg = text;
	}

	public String getMessage() {
		return msg;
	}

	public ArrayList<Object> getObjContents() {
		return objContents;
	}

	public void addObject(Object object) {
		objContents.add(object);
	}

	public void setN(int n) {
		this.n = n;
	}

	public int getN() {
		return this.n;
	}

	public void setHMAC(byte[] hmac) {
		this.hmac = hmac;
	}

	public byte[] calcHMAC(SecretKey key) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(msg.getBytes());
			outputStream.write(new byte[] {
	            (byte)(n >>> 24),
	            (byte)(n >>> 16),
	            (byte)(n >>> 8),
	            (byte)n});
			for (Object obj : objContents) {
				outputStream.write(EncryptionUtils.serialize(obj));
			}
			return EncryptionUtils.calcHMAC(key, outputStream.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] getHMAC() {
		return this.hmac;
	}

}
