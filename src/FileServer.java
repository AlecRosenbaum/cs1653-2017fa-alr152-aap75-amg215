/* FileServer loads files from FileList.bin.  Stores files in shared_files directory. */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class FileServer extends Server {

	public static final int SERVER_PORT = 4321;
	public static FileList fileList;
	public PublicKey trustedPubKey;

	public FileServer() {
		super(SERVER_PORT, "FilePile");
	}

	public FileServer(int _port) {
		super(_port, "FilePile");
	}

	public void start() {
		String fileFile = "FileList.bin";
		ObjectInputStream fileStream;

		//This runs a thread that saves the lists on program exit
		Runtime runtime = Runtime.getRuntime();
		Thread catchExit = new Thread(new ShutDownListenerFS());
		runtime.addShutdownHook(catchExit);

		//Open user file to get user list
		try {
			FileInputStream fis = new FileInputStream(fileFile);
			fileStream = new ObjectInputStream(fis);
			fileList = (FileList)fileStream.readObject();
		} catch (FileNotFoundException e) {
			System.out.println("FileList Does Not Exist. Creating FileList...");
			fileList = new FileList();
		} catch (IOException e) {
			System.out.println("Error reading from FileList file");
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			System.out.println("Error reading from FileList file");
			System.exit(-1);
		}

		File file = new File("shared_files");
		if (file.mkdir()) {
			System.out.println("Created new shared_files directory");
		} else if (file.exists()) {
			System.out.println("Found shared_files directory");
		} else {
			System.out.println("Error creating shared_files directory");
		}

		// load or generate RSA key
		try {
			PublicKey pubKey = EncryptionUtils.getRSAPublicKeyFromFile("FS_Pubkey");
			PrivateKey privKey = EncryptionUtils.getRSAPrivateKeyFromFile("FS_PrivKey");
			this.setKeys(new KeyPair(pubKey, privKey));
		} catch (FileNotFoundException e) {
			try {
				// generate new keypair
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
				keyGen.initialize(2048);
				this.setKeys(keyGen.genKeyPair());

				// write keypairs to file
				FileOutputStream fos = new FileOutputStream("FS_Pubkey");
			    fos.write(this.getPublicKey().getEncoded());
			    fos.close();
				
				fos = new FileOutputStream("FS_PrivKey");
			    fos.write(this.getPrivateKey().getEncoded());
			    fos.close();
			} catch (IOException | NoSuchAlgorithmException ex) {
				System.err.println("Couldn't generate new keys.");
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}

		// try to load trusted groupsever public key (fail if none found)
		try {
			this.trustedPubKey = EncryptionUtils.getRSAPublicKeyFromFile("FS_Trusted_Pubkey");
		} catch (FileNotFoundException e) {
			System.err.println("No trusted groupserver publickey found.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		//Autosave Daemon. Saves lists every 5 minutes
		AutoSaveFS aSave = new AutoSaveFS();
		aSave.setDaemon(true);
		aSave.start();

		boolean running = true;

		try {
			final ServerSocket serverSock = new ServerSocket(port);
			System.out.printf("%s up and running\n", this.getClass().getName());

			Socket sock = null;
			Thread thread = null;

			while (running) {
				sock = serverSock.accept();
				thread = new FileThread(sock, this);
				thread.start();
			}

			System.out.printf("%s shut down\n", this.getClass().getName());
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

//This thread saves user and group lists
class ShutDownListenerFS implements Runnable {
	public void run() {
		System.out.println("Shutting down server");
		ObjectOutputStream outStream;

		try {
			outStream = new ObjectOutputStream(new FileOutputStream("FileList.bin"));
			outStream.writeObject(FileServer.fileList);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

class AutoSaveFS extends Thread {
	public void run() {
		do {
			try {
				Thread.sleep(300000); //Save group and user lists every 5 minutes
				System.out.println("Autosave file list...");
				ObjectOutputStream outStream;
				try {
					outStream = new ObjectOutputStream(new FileOutputStream("FileList.bin"));
					outStream.writeObject(FileServer.fileList);
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
