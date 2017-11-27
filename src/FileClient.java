/* FileClient provides all the client functionality regarding the file server */

import java.io.File;
import java.io.*;
import java.io.IOException;
import java.util.List;
import java.security.PublicKey;
import java.net.Socket;

public class FileClient extends Client implements FileClientInterface {

	public boolean delete(String filename, UserToken token) {
		String remotePath;
		if (filename.charAt(0) == '/') {
			remotePath = filename.substring(1);
		} else {
			remotePath = filename;
		}
		Envelope env = new Envelope("DELETEF"); //Success
		env.addObject(remotePath);
		env.addObject(token);
		this.writeObjectToOutput(env);
		env = (Envelope) this.readObjectFromInput();

		if (env.getMessage().compareTo("OK") == 0) {
			System.out.printf("File %s deleted successfully\n", filename);
		} else {
			System.out.printf("Error deleting file %s (%s)\n", filename, env.getMessage());
			return false;
		}

		return true;
	}



    public PublicKey initialConnect(String server, int port)
    {
		try {
			sock = new Socket(server, port);		
			output = new ObjectOutputStream(sock.getOutputStream());
			input = new ObjectInputStream(sock.getInputStream());
	
			output.writeObject("key request");
			return (PublicKey)input.readObject();
			
		} catch(Exception e) {
			return null;
		}			
    }

	public byte[] download(String sourceFile, String destFile, UserToken token) {
		if (sourceFile.charAt(0) == '/') {
			sourceFile = sourceFile.substring(1);
		}
		byte [] returnAr = new byte[1];

		File file = new File(destFile);
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file);

			Envelope env = new Envelope("DOWNLOADF"); //Success
			env.addObject(sourceFile);
			env.addObject(token);
			this.writeObjectToOutput(env);

			env = (Envelope) this.readObjectFromInput();

			while (env.getMessage().compareTo("CHUNK") == 0) {
				fos.write((byte[])env.getObjContents().get(0), 0, (Integer)env.getObjContents().get(1));
				System.out.printf(".");
				env = new Envelope("DOWNLOADF"); //Success
				this.writeObjectToOutput(env);
				env = (Envelope) this.readObjectFromInput();
			}
			fos.close();

			if (env.getMessage().compareTo("EOF") == 0) {
				fos.close();
				returnAr = (byte[])env.getObjContents().get(0);
				System.out.printf("\nTransfer successful file %s\n", sourceFile);
				env = new Envelope("OK"); //Success
				this.writeObjectToOutput(env);
			} else {
				System.out.printf("Error reading file %s (%s)\n", sourceFile, env.getMessage());
				file.delete();
			}
		} catch (IOException e1) {
			System.out.printf("Error couldn't create file %s\n", destFile);
		}
		return returnAr;
	}

	@SuppressWarnings("unchecked")
	public List<String> listFiles(UserToken token) {
		try {
			Envelope message = null, e = null;
			//Tell the server to return the member list
			message = new Envelope("LFILES");
			message.addObject(token); //Add requester's token
			this.writeObjectToOutput(message);

			e = (Envelope) this.readObjectFromInput();

			//If server indicates success, return the member list
			if (e.getMessage().equals("OK")) {
				return (List<String>)e.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
			}

			return null;

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean upload(String sourceFile, String destFile, String group, UserToken token, FileKey fk) {

		if (destFile.charAt(0) != '/') {
			destFile = "/" + destFile;
		}

		try {

			Envelope message = null, env = null;
			//Tell the server to return the member list
			message = new Envelope("UPLOADF");
			message.addObject(destFile);
			message.addObject(group);
			message.addObject(token); //Add requester's token
			this.writeObjectToOutput(message);

			File file = new File(sourceFile);
			FileInputStream fis = new FileInputStream(file);
			byte[] plaintext = new byte[(int) file.length()];
			fis.read(plaintext);
			//enrypt file to be sent to fileserver
			byte[] ciphertext = EncryptionUtils.encrypt(fk.getKey(), plaintext);
			InputStream inputstream = new ByteArrayInputStream(ciphertext);

			env = (Envelope) this.readObjectFromInput();

			//If server indicates success, return the member list
			if (env.getMessage().equals("READY")) {
				System.out.printf("Meta data upload successful\n");

			} else {

				System.out.printf("Upload failed: %s\n", env.getMessage());
				return false;
			}


			do {
				byte[] buf = new byte[4096];
				if (env.getMessage().compareTo("READY") != 0) {
					System.out.printf("Server error: %s\n", env.getMessage());
					return false;
				}
				message = new Envelope("CHUNK");
				int n = inputstream.read(buf);
				if (n > 0) {
					System.out.printf(".");
				} else if (n < 0) {
					System.out.println("Read error");
					return false;
				}

				message.addObject(buf);
				message.addObject(new Integer(n));

				this.writeObjectToOutput(message);


				env = (Envelope) this.readObjectFromInput();


			} while (fis.available() > 0);

			//If server indicates success, return the member list
			if (env.getMessage().compareTo("READY") == 0) {

				message = new Envelope("EOF");
				message.addObject(fk.getEKey());
				message.addObject(new Integer(fk.getEKey().length));
				this.writeObjectToOutput(message);

				env = (Envelope) this.readObjectFromInput();
				if (env.getMessage().compareTo("OK") == 0) {
					System.out.printf("\nFile data upload successful\n");
				} else {

					System.out.printf("\nUpload failed: %s\n", env.getMessage());
					return false;
				}

			} else {

				System.out.printf("Upload failed: %s\n", env.getMessage());
				return false;
			}

		} catch (Exception e1) {
			System.err.println("Error: " + e1.getMessage());
			e1.printStackTrace(System.err);
			return false;
		}
		return true;
	}

}

