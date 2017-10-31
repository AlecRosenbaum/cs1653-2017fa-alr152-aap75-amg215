import java.io.*;
import javax.crypto.*;

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
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
        }
    }

}