import java.net.Socket;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import java.util.*;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.*;
import java.math.BigInteger;

public class FileKey implements java.io.Serializable {
    private static final long serialVersionUID = -8911161283900298136L;
    private SecretKey k;
    private byte[] ek;
    public FileKey(SecretKey key, byte[] encryptedKey) {
        k = key;
        ek = encryptedKey;
    }

    public SecretKey getKey() {
        return k;
    }

    public byte[] getEKey() {
        return ek;
    }
}