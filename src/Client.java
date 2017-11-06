import java.net.Socket;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import java.util.*;
import javax.crypto.spec.DHParameterSpec;
import java.security.spec.*;
import java.math.BigInteger;

public abstract class Client {

    /* protected keyword is like private but subclasses have access
     * Socket and input/output streams
     */
    protected Socket sock;
    protected ObjectOutputStream output;
    protected ObjectInputStream input;
    protected SecretKey DH_Key;

    public boolean connect(final String server, final int port, PublicKey serverPublicKey) {

        try {

            sock = new Socket(server, port);

            // Set up I/O streams with the server
            output = new ObjectOutputStream(sock.getOutputStream());
            input = new ObjectInputStream(sock.getInputStream());

            // Generate DH specs (2048-bit p, 224 bit key)
            String p_hex = 
                "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
                "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
                "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
                "15728E5A8AACAA68FFFFFFFFFFFFFFFF";            
            BigInteger p = new BigInteger(p_hex, 16);
            BigInteger g = BigInteger.valueOf(2);
            DHParameterSpec paramSpec = new DHParameterSpec(p, g, 224);

            // Generate Key Pair
            KeyPairGenerator aliceKpGen = KeyPairGenerator.getInstance("DH");
            aliceKpGen.initialize(paramSpec);
            KeyPair aliceKp = aliceKpGen.generateKeyPair();
            KeyAgreement aKeyAgreement = KeyAgreement.getInstance("DH");
            aKeyAgreement.init(aliceKp.getPrivate());

            output.writeObject(aliceKp.getPublic().getEncoded());

            // Recieve Bob's DH Info
            byte[] bobDH = (byte[]) input.readObject();
            if(serverPublicKey != null) {
                byte[] signature = (byte[]) input.readObject();
                Signature publicSignature = Signature.getInstance("SHA256withRSA");
                publicSignature.initVerify(serverPublicKey);
                publicSignature.update(bobDH);            
                if(!publicSignature.verify(signature))
                {
                    System.out.println("Invalid signature, aborting connection.");
                    return false;
                }
                else {
                    System.out.println("Accepted Signature");
                }

            }

            KeyFactory clientKeyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bobDH);
            PublicKey bobsDHPubKey = clientKeyFac.generatePublic(x509KeySpec);
            aKeyAgreement.doPhase(bobsDHPubKey, true);

            // Generate AES Secret Keys
            this.DH_Key = aKeyAgreement.generateSecret("AES");
            // System.out.println(Base64.getEncoder().encodeToString(this.DH_Key.getEncoded()));

            // success!
            return true;
        } catch (Exception e) {
            // oh no....
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    public boolean isConnected() {
        if (sock == null || !sock.isConnected()) {
            return false;
        } else {
            return true;
        }
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                Envelope message = new Envelope("DISCONNECT");
                this.writeObjectToOutput(message);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }


    /**
     * Writes an object to output. Will handle all encryption.
     *
     * @param      obj   The object
     *
     * @return     true if successful, false otherwise
     */
    public boolean writeObjectToOutput(Serializable obj) {
        if (isConnected()) {
            try {
                output.writeObject(EncryptionUtils.encrypt(DH_Key, obj));
                return true;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
        return false;
    }

    /**
     * Writes an object to output. Will handle all encryption.
     *
     * @return     the object read from input, or null if an error occurred
     */
    public Object readObjectFromInput() {
        if (isConnected()) {
            try {
                return EncryptionUtils.decrypt(DH_Key, (byte[]) input.readObject());
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
        return null;
    }
}
