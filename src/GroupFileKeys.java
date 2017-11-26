import java.net.Socket;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import java.util.*;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.*;
import java.math.BigInteger;


public class GroupFileKeys implements java.io.Serializable {
    private static final long serialVersionUID = -8911161283900215136L;
    protected ArrayList<String> groups;
    protected ArrayList<SecretKey> keys;
    protected GroupServer gs;
    private static String backupPath = "GroupFileKeys.bin";

    public GroupFileKeys(GroupServer new_gs) {
        groups = new ArrayList<String>();
        keys = new ArrayList<SecretKey>();
        gs = new_gs;
        backup();
    }

    public boolean addGroup(String name) {
        try {
            groups.add(name);
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();
            keys.add(secretKey);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public boolean remove(String group){
        try {
            keys.remove(keys.get(group.indexOf(group)));
            groups.remove(group);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public FileKey upload(String group) {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey fileKey = keyGen.generateKey();
        SecretKey groupKey = keys.get(groups.indexOf(group));
        byte[] encryptedKey = EncryptionUtils.encrypt(groupKey, fileKey);
        return new FileKey(fileKey, encryptedKey);
    }     

    public SecretKey download(String group, byte [] encryptedKey) {
        SecretKey groupKey = keys.get(groups.indexOf(group));
        return (SecretKey)EncryptionUtils.decrypt(groupKey, encryptedKey);
    }

    public void backup() {
        try {
            ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(backupPath));
            outStream.writeObject(this);
        } catch(Exception e) {
            System.out.println("Error backing up approved files");
        }
        
    }
        
}

public class FileKey {
    SecretKey k;
    byte[] ek;
    public FileKey(SecretKey key, byte[] encryptedKey) {
        k = key;
        ek = encryptedKey;
    }

    public SeretKey getKey() {
        return k;
    }

    public byte[] getEKey() {
        return ek;
    }
}


    