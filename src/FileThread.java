/* File worker thread handles the business of uploading, downloading, and removing files for clients with valid tokens */

import java.lang.Thread;
import java.net.Socket;
import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.math.BigInteger;

public class FileThread extends Thread {
	private final Socket socket;
	private final FileServer my_fs;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private SecretKey DH_Key;

	public FileThread(Socket _socket, FileServer _fs) {
		socket = _socket;
		my_fs = _fs;
	}

	public void run() {
		boolean proceed = true;
		try {
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			input = new ObjectInputStream(socket.getInputStream());
			output = new ObjectOutputStream(socket.getOutputStream());
			Envelope response;

			// negotiate diffie hellman

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
			DHParameterSpec dhParamSpec = new DHParameterSpec(p, g, 224);

			// Exchange information for DH
			KeyFactory clientKeyFac = KeyFactory.getInstance("DH");
			Object inputMessage = input.readObject();
			byte[] DHinfo = null;
			try {
				DHinfo = (byte[]) inputMessage;
			}
			catch (Exception e)
			{				
				if("key request".equals((String)inputMessage)) {
					output.writeObject(my_fs.getPublicKey());
					System.out.println("Providing Public Key for Initial Authentication.");
					return;
				}
			}

			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(DHinfo);
			PublicKey bobDHPub = clientKeyFac.generatePublic(x509KeySpec);

			// Create Bob DH Keys
			KeyPairGenerator bobKpGen = KeyPairGenerator.getInstance("DH");
			bobKpGen.initialize(dhParamSpec);
			KeyPair bobsKeys = bobKpGen.generateKeyPair();

			KeyAgreement bobKeyAgreement = KeyAgreement.getInstance("DH");
			bobKeyAgreement.init(bobsKeys.getPrivate());
			bobKeyAgreement.doPhase(bobDHPub, true);

			Signature privateSignature = Signature.getInstance("SHA256withRSA");
			privateSignature.initSign(my_fs.getPrivateKey());
			privateSignature.update(bobsKeys.getPublic().getEncoded());		
			byte[] signature = privateSignature.sign();

			// Send Bob's DH Parameters to Alice
			output.writeObject(bobsKeys.getPublic().getEncoded());
			output.writeObject(signature);

			// Generate AES Secret Keys
			this.DH_Key = bobKeyAgreement.generateSecret("AES");
			// System.out.println(Base64.getEncoder().encodeToString(this.DH_Key.getEncoded()));

			do {
				Envelope e = (Envelope) readObjectFromInput();
				System.out.println("Request received: " + e.getMessage());

				// Handler to list files that this user is allowed to see
				if (e.getMessage().equals("LFILES")) {
					/* TODO: Write this handler */
					if (e.getObjContents().size() < 1) {
						response = new Envelope("FAIL-BADCONTENTS");
					} else {
						if (e.getObjContents().get(0) == null) {
							response = new Envelope("FAIL-BADTOKEN");
						} else {
							// token contains user, server, and groups
							Token yourToken = (Token)e.getObjContents().get(0); //Extract token
							if (!EncryptionUtils.verify(yourToken.getSignature(), yourToken.stringify(), my_fs.trustedPubKey)) {
								response = new Envelope("FAIL-INVALID-TOKEN");
								writeObjectToOutput(response);
								continue;
							}

							// check files for files in groups that user is a part of
							ArrayList<String> fileNames = new ArrayList<String>();
							for (ShareFile f : FileServer.fileList.getFiles()) {
								if (yourToken.getGroups().contains(f.getGroup())) {
									fileNames.add(f.getPath());
								}
							}

							// respond with a list of files: List<String>
							response = new Envelope("OK"); //Success
							response.addObject(fileNames);
							writeObjectToOutput(response);
						}
					}
				}
				if (e.getMessage().equals("UPLOADF")) {

					if (e.getObjContents().size() < 3) {
						response = new Envelope("FAIL-BADCONTENTS");
					} else {
						if (e.getObjContents().get(0) == null) {
							response = new Envelope("FAIL-BADPATH");
						}
						if (e.getObjContents().get(1) == null) {
							response = new Envelope("FAIL-BADGROUP");
						}
						if (e.getObjContents().get(2) == null) {
							response = new Envelope("FAIL-BADTOKEN");
						} else {
							String remotePath = (String)e.getObjContents().get(0);
							String group = (String)e.getObjContents().get(1);
							Token yourToken = (Token)e.getObjContents().get(2); //Extract token
							if (!EncryptionUtils.verify(yourToken.getSignature(), yourToken.stringify(), my_fs.trustedPubKey)) {
								response = new Envelope("FAIL-INVALID-TOKEN");
								writeObjectToOutput(response);
								continue;
							}

							if (FileServer.fileList.checkFile(remotePath)) {
								System.out.printf("Error: file already exists at %s\n", remotePath);
								response = new Envelope("FAIL-FILEEXISTS"); //Success
							} else if (!yourToken.getGroups().contains(group)) {
								System.out.printf("Error: user missing valid token for group %s\n", group);
								response = new Envelope("FAIL-UNAUTHORIZED"); //Success
							} else  {
								File file = new File("shared_files/" + remotePath.replace('/', '_'));
								file.createNewFile();
								FileOutputStream fos = new FileOutputStream(file);
								System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));

								response = new Envelope("READY"); //Success
								writeObjectToOutput(response);

								e = (Envelope) readObjectFromInput();
								while (e.getMessage().compareTo("CHUNK") == 0) {
									fos.write((byte[])e.getObjContents().get(0), 0, (Integer)e.getObjContents().get(1));
									response = new Envelope("READY"); //Success
									writeObjectToOutput(response);
									e = (Envelope) readObjectFromInput();
								}

								if (e.getMessage().compareTo("EOF") == 0) {
									System.out.printf("Transfer successful file %s\n", remotePath);
									FileServer.fileList.addFile(yourToken.getSubject(), group, remotePath);
									response = new Envelope("OK"); //Success
								} else {
									System.out.printf("Error reading file %s from client\n", remotePath);
									response = new Envelope("ERROR-TRANSFER"); //Success
								}
								fos.close();
							}
						}
					}

					writeObjectToOutput(response);
				} else if (e.getMessage().compareTo("DOWNLOADF") == 0) {

					String remotePath = (String)e.getObjContents().get(0);
					Token t = (Token)e.getObjContents().get(1);
					if (!EncryptionUtils.verify(t.getSignature(), t.stringify(), my_fs.trustedPubKey)) {
						response = new Envelope("FAIL-INVALID-TOKEN");
						writeObjectToOutput(response);
						continue;
					}

					ShareFile sf = FileServer.fileList.getFile("/" + remotePath);
					if (sf == null) {
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
						e = new Envelope("ERROR_FILEMISSING");
						writeObjectToOutput(e);

					} else if (!t.getGroups().contains(sf.getGroup())) {
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
						e = new Envelope("ERROR_PERMISSION");
						writeObjectToOutput(e);
					} else {

						try {
							File f = new File("shared_files/_" + remotePath.replace('/', '_'));
							if (!f.exists()) {
								System.out.printf("Error file %s missing from disk\n", "_" + remotePath.replace('/', '_'));
								e = new Envelope("ERROR_NOTONDISK");
								writeObjectToOutput(e);

							} else {
								FileInputStream fis = new FileInputStream(f);

								do {
									byte[] buf = new byte[4096];
									if (e.getMessage().compareTo("DOWNLOADF") != 0) {
										System.out.printf("Server error: %s\n", e.getMessage());
										break;
									}
									e = new Envelope("CHUNK");
									int n = fis.read(buf); //can throw an IOException
									if (n > 0) {
										System.out.printf(".");
									} else if (n < 0) {
										System.out.println("Read error");

									}


									e.addObject(buf);
									e.addObject(new Integer(n));

									writeObjectToOutput(e);

									e = (Envelope) readObjectFromInput();


								} while (fis.available() > 0);

								//If server indicates success, return the member list
								if (e.getMessage().compareTo("DOWNLOADF") == 0) {

									e = new Envelope("EOF");
									writeObjectToOutput(e);

									e = (Envelope) readObjectFromInput();
									if (e.getMessage().compareTo("OK") == 0) {
										System.out.printf("File data upload successful\n");
									} else {

										System.out.printf("Upload failed: %s\n", e.getMessage());

									}

								} else {

									System.out.printf("Upload failed: %s\n", e.getMessage());

								}
							}
						} catch (Exception e1) {
							System.err.println("Error: " + e.getMessage());
							e1.printStackTrace(System.err);

						}
					}
				} else if (e.getMessage().compareTo("DELETEF") == 0) {

					String remotePath = (String)e.getObjContents().get(0);
					Token t = (Token)e.getObjContents().get(1);
					if (!EncryptionUtils.verify(t.getSignature(), t.stringify(), my_fs.trustedPubKey)) {
						response = new Envelope("FAIL-INVALID-TOKEN");
						writeObjectToOutput(response);
						continue;
					}
					ShareFile sf = FileServer.fileList.getFile("/" + remotePath);
					if (sf == null) {
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
						e = new Envelope("ERROR_DOESNTEXIST");
					} else if (!t.getGroups().contains(sf.getGroup())) {
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
						e = new Envelope("ERROR_PERMISSION");
					} else {

						try {


							File f = new File("shared_files/" + "_" + remotePath.replace('/', '_'));

							if (!f.exists()) {
								System.out.printf("Error file %s missing from disk\n", "_" + remotePath.replace('/', '_'));
								e = new Envelope("ERROR_FILEMISSING");
							} else if (f.delete()) {
								System.out.printf("File %s deleted from disk\n", "_" + remotePath.replace('/', '_'));
								FileServer.fileList.removeFile("/" + remotePath);
								e = new Envelope("OK");
							} else {
								System.out.printf("Error deleting file %s from disk\n", "_" + remotePath.replace('/', '_'));
								e = new Envelope("ERROR_DELETE");
							}


						} catch (Exception e1) {
							System.err.println("Error: " + e1.getMessage());
							e1.printStackTrace(System.err);
							e = new Envelope(e1.getMessage());
						}
					}
					writeObjectToOutput(e);

				} else if (e.getMessage().equals("DISCONNECT")) {
					socket.close();
					proceed = false;
				}
			} while (proceed);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}


	/**
	 * Reads an object from input. Will handle all encryption.
	 *
	 * @return     object from input, null if errored
	 */
	public Object readObjectFromInput() {
		try {
			return EncryptionUtils.decrypt(DH_Key, (byte[]) input.readObject());
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return null;
	}


	/**
	 * Writes an object to output. Will handle all encryption.
	 *
	 * @param      obj   The object
	 *
	 * @return     true if successful, false otherwise
	 */
	public boolean writeObjectToOutput(Serializable obj) {
		try {
			this.output.writeObject(EncryptionUtils.encrypt(DH_Key, obj));
			return true;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return false;
	}

}
