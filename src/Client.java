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

    public boolean connect(final String server, final int port) {

        try {

            sock = new Socket(server, port);

            // Set up I/O streams with the server
            output = new ObjectOutputStream(sock.getOutputStream());
            input = new ObjectInputStream(sock.getInputStream());

            // Generate DH specs (2048-bit g and p, 224 bit key)
            int bitLength = 2048;
            SecureRandom rnd = new SecureRandom();
            BigInteger p = BigInteger.probablePrime(bitLength, rnd);
            BigInteger g = BigInteger.probablePrime(bitLength, rnd);
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
