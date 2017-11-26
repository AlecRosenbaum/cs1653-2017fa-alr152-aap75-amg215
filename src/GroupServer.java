/* Group server. Server loads the users from UserList.bin.
 * If user list does not exists, it creates a new list and makes the user the server administrator.
 * On exit, the server saves the user list to file.
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.*;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;

public class GroupServer extends Server {

	public static final int SERVER_PORT = 8765;
	public UserList userList;
	public ArrayList<String> groups;
	public GroupFileKeys gfk;

	public GroupServer() {
		super(SERVER_PORT, "ALPHA");
	}

	public GroupServer(int _port) {
		super(_port, "ALPHA");
	}

	@SuppressWarnings("unchecked")
	public void start() {
		// Overwrote server.start() because if no user file exists, initial admin account needs to be created

		String userFile = "UserList.bin";
		String groupFile = "GroupList.bin";
		String groupKeyFile = " ";
		Scanner console = new Scanner(System.in);
		ObjectInputStream userStream;
		ObjectInputStream groupStream;
		ObjectInputStream groupKeyStream;

		//This runs a thread that saves the lists on program exit
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new ShutDownListener(this));

		//Open user file to get user list
		try {
			FileInputStream fis = new FileInputStream(userFile);
			userStream = new ObjectInputStream(fis);
			userList = (UserList)userStream.readObject();
		} catch (FileNotFoundException e) {
			System.out.println("UserList File Does Not Exist. Creating UserList...");
			System.out.println("No users currently exist. Your account will be the administrator.");
			System.out.print("Enter your username: ");
			String username = console.next();
			System.out.println("The default administrator password is 'admin'");

			//Create a new list, add current user to the ADMIN group. They now own the ADMIN group.
			userList = new UserList();
			userList.addUser(username, "admin");
			userList.addGroup(username, "ADMIN");
			userList.addOwnership(username, "ADMIN");
		} catch (IOException e) {
			System.out.println("Error reading from UserList file");
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			System.out.println("Error reading from UserList file");
			System.exit(-1);
		}

		try {
			FileInputStream fis = new FileInputStream(groupKeyFile);
			groupKeyStream = new ObjectInputStream(fis);
			gfk = (GroupFileKeys)groupKeyStream.readObject();
		} catch (FileNotFoundException e) {
			System.out.println("Group file keys not found, creating new object...");
			//Create new GroupFileKeys			
			gfk = new GroupFileKeys(this);
		} catch (ClassNotFoundException e) {
			System.out.println("Error reading from Group File Keys file");
			System.exit(-1);
		} catch (IOException e) {
			System.out.println("Error reading from Group File Keys file");
			System.exit(-1);
		}

		//Open group file to get group list
		try {
			FileInputStream fis = new FileInputStream(groupFile);
			groupStream = new ObjectInputStream(fis);
			groups = (ArrayList<String>)groupStream.readObject(); //This cast creates compiler warnings.
		} catch (FileNotFoundException e) {
			System.out.println("GroupList File Does Not Exist. Creating GroupList...");

			//Create a new group list, add the ADMIN group (always present default).
			groups = new ArrayList<String>();
			groups.add("ADMIN");
		} catch (IOException e) {
			System.out.println("Error reading from GroupList file");
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			System.out.println("Error reading from GroupList file");
			System.exit(-1);
		}

		// load or generate RSA key
		try {
			PublicKey pubKey = EncryptionUtils.getRSAPublicKeyFromFile("GS_Pubkey");
			PrivateKey privKey = EncryptionUtils.getRSAPrivateKeyFromFile("GS_PrivKey");
			this.setKeys(new KeyPair(pubKey, privKey));
		} catch (FileNotFoundException e) {
			try {
				// generate new keypair
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
				keyGen.initialize(2048);
				this.setKeys(keyGen.genKeyPair());

				// write keypairs to file
				FileOutputStream fos = new FileOutputStream("GS_Pubkey");
				fos.write(this.getPublicKey().getEncoded());
				fos.close();

				fos = new FileOutputStream("GS_PrivKey");
				fos.write(this.getPrivateKey().getEncoded());
				fos.close();
			} catch (Exception ex) {
				System.err.println("Couldn't generate new keys.");
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}

		System.out.println("GroupServer up and running");

		//Autosave Daemon. Saves lists every 5 minutes
		AutoSave aSave = new AutoSave(this);
		aSave.setDaemon(true);
		aSave.start();

		//This block listens for connections and creates threads on new connections
		try {

			final ServerSocket serverSock = new ServerSocket(port);

			Socket sock = null;
			GroupThread thread = null;

			while (true) {
				sock = serverSock.accept();
				thread = new GroupThread(sock, this);
				thread.start();
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}

	}

	public byte[] sign(final String plaintext) throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
		return EncryptionUtils.sign(plaintext, this.getPrivateKey());
	}

}

//This thread saves the user list
class ShutDownListener extends Thread {
	public GroupServer my_gs;

	public ShutDownListener (GroupServer _gs) {
		my_gs = _gs;
	}

	public void run() {
		System.out.println("Shutting down server");
		ObjectOutputStream outStream;
		try {
			outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
			outStream.writeObject(my_gs.userList);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

class AutoSave extends Thread {
	public GroupServer my_gs;

	public AutoSave (GroupServer _gs) {
		my_gs = _gs;
	}

	public void run() {
		do {
			try {
				Thread.sleep(300000); //Save group and user lists every 5 minutes
				System.out.println("Autosave group and user lists...");
				ObjectOutputStream outStream;
				try {
					outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
					outStream.writeObject(my_gs.userList);
				} catch (Exception e) {
					System.err.println("Error: " + e.getMessage());
					e.printStackTrace(System.err);
				}

				try {
					outStream = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
					outStream.writeObject(my_gs.groups);
				} catch (Exception e) {
					System.err.println("Error: " + e.getMessage());
					e.printStackTrace(System.err);
				}
			} catch (Exception e) {
				System.out.println("Autosave Interrupted");
			}
		} while (true);
	}
}
