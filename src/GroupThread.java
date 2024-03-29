/* This thread does all the work. It communicates with the client through Envelopes.
 *
 */
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
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;

public class GroupThread extends Thread {
	private final Socket socket;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private GroupServer my_gs;
	private SecretKey DH_Key_Encryption, DH_Key_Integrity;
	private int n;

	public GroupThread(Socket _socket, GroupServer _gs) {
		socket = _socket;
		my_gs = _gs;
	}

	public void run() {
		boolean proceed = true;

		try {
			//Announces connection and opens object streams
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			input = new ObjectInputStream(socket.getInputStream());
			output = new ObjectOutputStream(socket.getOutputStream());

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
			KeyFactory clientKeyFac = KeyFactory.getInstance("DH", "BC");
			Object inputMessage = input.readObject();
			byte[] DHinfo = null;
			try {
				DHinfo = (byte[]) inputMessage;
			} catch (Exception e) {
				if ("key request".equals((String)inputMessage)) {
					output.writeObject(my_gs.getPublicKey());
					System.out.println("Providing Public Key for Initial Authentication.");
					return;
				}
			}
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(DHinfo);
			PublicKey bobDHPub = clientKeyFac.generatePublic(x509KeySpec);

			// Create Bob DH Keys
			KeyPairGenerator bobKpGen = KeyPairGenerator.getInstance("DH", "BC");
			bobKpGen.initialize(dhParamSpec);
			KeyPair bobsKeys = bobKpGen.generateKeyPair();

			KeyAgreement bobKeyAgreement = KeyAgreement.getInstance("DH", "BC");
			bobKeyAgreement.init(bobsKeys.getPrivate());
			bobKeyAgreement.doPhase(bobDHPub, true);

			Signature privateSignature = Signature.getInstance("SHA256withRSA", "BC");
			privateSignature.initSign(my_gs.getPrivateKey());
			privateSignature.update(bobsKeys.getPublic().getEncoded());
			byte[] signature = privateSignature.sign();

			// Send Bob's DH Parameters to Alice
			output.writeObject(bobsKeys.getPublic().getEncoded());
			output.writeObject(signature);

			// Generate AES Secret Keys
			SecretKey DH_Key = bobKeyAgreement.generateSecret("AES");
			// System.out.println(Base64.getEncoder().encodeToString(this.DH_Key.getEncoded()));
			// 
			byte[] base = DH_Key.getEncoded();
            byte[] enc = "E".getBytes();
            byte[] integ = "I".getBytes();
            
            byte[] base_enc = new byte[base.length + enc.length];
            System.arraycopy(base, 0, base_enc, 0, base.length);
            System.arraycopy(enc, 0, base_enc, base.length, enc.length);
            byte[] base_enc_hash = EncryptionUtils.hash(base_enc);

            byte[] base_integ = new byte[base.length + integ.length];
            System.arraycopy(base, 0, base_integ, 0, base.length);
            System.arraycopy(enc, 0, base_integ, base.length, integ.length);
            byte[] base_integ_hash = EncryptionUtils.hash(base_enc);

            this.DH_Key_Encryption = new SecretKeySpec(base_enc_hash, 0, 16, "AES");
            this.DH_Key_Integrity = new SecretKeySpec(base_integ_hash, 0, 16, "AES");

			this.n = 0;
			do {
				Envelope message = (Envelope) EncryptionUtils.decrypt(DH_Key_Encryption, (byte[]) input.readObject());
				System.out.println("Request received: " + message.getMessage());
				Envelope response;

				// validate sequence
				if (message.getN() < this.n) {
					System.err.println(message.getN() + " " + this.n);
					proceed = false;
					continue;
				} else {
					this.n = message.getN() + 1;
				}

				// validate hmac
				if (!Arrays.equals(message.getHMAC(), message.calcHMAC(this.DH_Key_Integrity))) {
                    System.err.print(message.getHMAC());
                    System.err.print(" ");
                    System.err.println(message.calcHMAC(this.DH_Key_Integrity));
                    proceed = false;
					continue;
                }

				try {

					if (message.getMessage().equals("GET")) { //Client wants a token
						String username = (String) message.getObjContents().get(0); //Get the username
						String password = (String) message.getObjContents().get(1); //Get the password
						byte[] fingerprint = (byte[]) message.getObjContents().get(2); //Get the fingerprint
						if (username == null || my_gs.userList.lockedOut(username) || !my_gs.userList.validate(username, password)) {
							response = new Envelope("FAIL");
							response.addObject(null);
							writeObjectToOutput(response);
						} else {
							UserToken yourToken = createToken(username, fingerprint); //Create a token

							//Respond to the client. On error, the client will receive a null token
							response = new Envelope("OK");
							response.addObject(yourToken);
							writeObjectToOutput(response);
						}
					} else if (message.getMessage().equals("CUSER")) { //Client wants to create a user
						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									String username = (String)message.getObjContents().get(0); //Extract the username
									UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

									String startpass = EncryptionUtils.generateRandomString(8);
									if (createUser(username, yourToken, startpass)) {
										response = new Envelope("OK"); //Success
										response.addObject(startpass);
									}
								}
							}
						}

						writeObjectToOutput(response);
					} else if (message.getMessage().equals("DUSER")) { //Client wants to delete a user

						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									String username = (String)message.getObjContents().get(0); //Extract the username
									UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

									if (deleteUser(username, yourToken)) {
										response = new Envelope("OK"); //Success
									}
								}
							}
						}

						writeObjectToOutput(response);
					} else if (message.getMessage().equals("CGROUP")) { //Client wants to create a group
						/* TODO:  Write this handler */
						if (message.getObjContents().size() < 2) {
							System.out.println("CGROUP message not correct size.");
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									String groupname = (String)message.getObjContents().get(0); //Extract the groupname
									UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

									System.out.println("CreateGroup command: " + groupname + " " + yourToken.getSubject());
									if (createGroup(groupname, yourToken)) {
										response = new Envelope("OK"); //Success
									}
								}
							}
						}

						writeObjectToOutput(response);
					} else if (message.getMessage().equals("DGROUP")) { //Client wants to delete a group
						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									String groupname = (String)message.getObjContents().get(0); //Extract the groupname
									UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

									if (deleteGroup(groupname, yourToken)) {
										response = new Envelope("OK"); //Success
									}
								}
							}
						}
						writeObjectToOutput(response);
					} else if (message.getMessage().equals("LMEMBERS")) { //Client wants a list of members in a group
						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									String groupname = (String)message.getObjContents().get(0); //Extract the groupname
									UserToken yourToken = (UserToken)message.getObjContents().get(1); //Extract the token

									ArrayList<String> members = (ArrayList<String>)listMembers(groupname, yourToken);
									response = new Envelope("OK"); //Success
									response.addObject(members);
								}
							}
						}
						writeObjectToOutput(response);
					} else if (message.getMessage().equals("AUSERTOGROUP")) { //Client wants to add user to a group
						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									String username = (String)message.getObjContents().get(0);
									String groupname = (String)message.getObjContents().get(1); //Extract the groupname
									UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token

									if (addUserToGroup(username, groupname, yourToken)) {
										response = new Envelope("OK"); //Success
									}
								}
							}
						}
						writeObjectToOutput(response);

					} else if (message.getMessage().equals("RUSERFROMGROUP")) { //Client wants to remove user from a group
						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null) {
								if (message.getObjContents().get(1) != null) {
									String username = (String)message.getObjContents().get(0);
									String groupname = (String)message.getObjContents().get(1); //Extract the groupname
									UserToken yourToken = (UserToken)message.getObjContents().get(2); //Extract the token

									if (deleteUserFromGroup(username, groupname, yourToken)) {
										response = new Envelope("OK"); //Success
									}
								}
							}
						}
						writeObjectToOutput(response);
					} else if (message.getMessage().equals("CPASSWORD")) { //Client wants to change password
						if (message.getObjContents().size() < 4) {
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null && message.getObjContents().get(1) != null && message.getObjContents().get(2) != null && message.getObjContents().get(3) != null) {
								Token token = (Token)message.getObjContents().get(0); //Extract the token
								String username = (String)message.getObjContents().get(1); //Extract the username
								String oldPass = (String)message.getObjContents().get(2); //Extract the old pass
								String newPass = (String)message.getObjContents().get(3); //Extract the new pass

								if (changePassword(token, username, oldPass, newPass)) {
									response = new Envelope("OK"); //Success
								}
							}
						}
						writeObjectToOutput(response);
					} else if (message.getMessage().equals("UFILE")) {
						if (message.getObjContents().size() < 2) {
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null && message.getObjContents().get(1) != null) {
								Token token = (Token)message.getObjContents().get(0); //Extract the token
								String group = (String)message.getObjContents().get(1); //Extract the username
								FileKey fk = uploadFile(group, token);
								if(fk == null) {
									response = new Envelope("FAIL");
								} else {
									response = new Envelope("OK"); //Success
									response.addObject(fk);
								}
							}
						}
						writeObjectToOutput(response);
					} else if (message.getMessage().equals("DFILE")) {
						if (message.getObjContents().size() < 3) {
							response = new Envelope("FAIL");
						} else {
							response = new Envelope("FAIL");

							if (message.getObjContents().get(0) != null && message.getObjContents().get(1) != null) {
								Token token = (Token)message.getObjContents().get(0); //Extract the token
								String group = (String)message.getObjContents().get(1); //Extract the username
								byte [] ek = (byte [])message.getObjContents().get(2);
								SecretKey s = downloadFile(group, ek, token);
								if(s == null) {
									response = new Envelope("FAIL");
								} else {
									response = new Envelope("OK");
									response.addObject(s);
								}
							}
						}
						
						writeObjectToOutput(response);
					} else if (message.getMessage().equals("DISCONNECT")) { //Client wants to disconnect
						socket.close(); //Close the socket
						proceed = false; //End this communication loop
					} else {
						response = new Envelope("FAIL"); //Server does not understand client request
						output.writeObject(EncryptionUtils.encrypt(DH_Key, response));
					}
				} catch (Exception e) {
					response = new Envelope("FAIL"); //Server does not understand client request
					output.writeObject(EncryptionUtils.encrypt(DH_Key, response));
				}
			} while (proceed);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			Envelope response = new Envelope("FAIL");
			response.addObject(null);
			writeObjectToOutput(response);
		}
	}

	//Method to create tokens
	private UserToken createToken(String username, byte[] fingerprint) {
		//Check that user exists
		if (my_gs.userList.checkUser(username)) {
			//Issue a new token with server's name, user's name, and user's groups
			Token yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username), fingerprint);

			// Sign token
			try {
				yourToken.setSignature(my_gs.sign(yourToken.stringify()));
			} catch (Exception e) {
				System.err.println("Error signing token.");
				e.printStackTrace(System.err);
			}

			return yourToken;
		} else {
			return null;
		}
	}

	//Method to change password
	private boolean changePassword(Token token, String username, String oldPass, String newPass) {
		//Check that user exists
		if (my_gs.userList.checkUser(username)) {
			// check if accound is locked
			if (my_gs.userList.lockedOut(username)) {
				// only an admin can change	
				if (my_gs.userList.getUserGroups(token.getSubject()).contains("ADMIN")) {
					// user is an admin, set new password
					my_gs.userList.changePassword(username, newPass);
					return true;
				}
			} else {
				// validate old password
				if (my_gs.userList.validate(username, oldPass)) {
					// set new password
					my_gs.userList.changePassword(username, newPass);
					return true;
				}
			}
		}

		return false;
	}


	//Method to create a user
	private boolean createUser(String username, UserToken yourToken, String initPass) {
		String requester = yourToken.getSubject();

		//Check if requester exists
		if (my_gs.userList.checkUser(requester)) {
			//Get the user's groups
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administrator
			if (temp.contains("ADMIN")) {
				//Does user already exist?
				if (my_gs.userList.checkUser(username)) {
					return false; //User already exists
				} else {
					my_gs.userList.addUser(username, initPass);
					return true;
				}
			} else {
				return false; //requester not an administrator
			}
		} else {
			return false; //requester does not exist
		}
	}

	//Method to delete a user
	private boolean deleteUser(String username, UserToken yourToken) {
		String requester = yourToken.getSubject();

		//Does requester exist?
		if (my_gs.userList.checkUser(requester)) {
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administer
			if (temp.contains("ADMIN")) {
				//Does user exist?
				if (my_gs.userList.checkUser(username)) {

					if (!requester.equals(username)) {

						// delete groups owned by that user
						for (String group : my_gs.userList.getUserOwnership(username)) {
							my_gs.userList.removeOwnership(username, group);
							my_gs.userList.removeGroupMembers(group);
							my_gs.groups.remove(group);
						}

						//Delete the user from the user list
						my_gs.userList.deleteUser(username);

						return true;
					} else {
						return false;
					}
				} else {
					return false; //User does not exist

				}
			} else {
				return false; //requester is not an administer
			}
		} else {
			return false; //requester does not exist
		}
	}

	// method for creating groups
	public boolean createGroup(String groupname, UserToken token) {
		String requester = token.getSubject();

		// //Check if requester exists
		if (my_gs.userList.checkUser(requester)) {

			// check if group exists
			if (my_gs.groups.contains(groupname)) {
				System.out.println("Group already exists.");
				return false;
			} else if (groupname.contains(",")) {
				System.out.println("Group name isn't valid. It contains a ','.");
				return false;
			} else {
				// add group to list
				my_gs.groups.add(groupname);
				my_gs.gfk.addGroup(groupname);

				// add group to user, make user owner of group
				my_gs.userList.addGroup(requester, groupname);
				my_gs.userList.addOwnership(requester, groupname);

				return true;
			}
		} else {
			System.out.println("Requester does not exist.");
			return false; //requester does not exist
		}

	}


	public boolean deleteGroup(String group, UserToken token) {
		String requester = token.getSubject();

		//Does requester exist?
		if (my_gs.userList.checkUser(requester)) {
			if (my_gs.groups.contains(group)) {
				ArrayList<String> temp = my_gs.userList.getUserOwnership(requester);
				if (temp.contains(group)) {
					my_gs.userList.removeOwnership(requester, group);
					my_gs.userList.removeGroupMembers(group);
					my_gs.groups.remove(group);
					my_gs.gfk.remove(group);
					return true;

				} else {
					return false;//user not group owner
				}
			} else {
				return false;//group does not exist
			}
		} else {
			return false;//user does not exist
		}
	}

	public FileKey uploadFile(String group, UserToken token) {
		String requester = token.getSubject();
		if (my_gs.userList.checkUser(requester)) {
			if (my_gs.groups.contains(group)) {
				return my_gs.gfk.upload(group);
			}
		}
		return null;
	}

	public SecretKey downloadFile(String group, byte[] encryptedKey, UserToken token) {
		String requester = token.getSubject();
		if (my_gs.userList.checkUser(requester)) {
			if (my_gs.groups.contains(group)) {
				return my_gs.gfk.download(group, encryptedKey);
			}
		}
		return null;
	}

	public boolean addUserToGroup(String username, String groupname, UserToken token) {
		String requester = token.getSubject();

		//Does requester exist?
		if (my_gs.userList.checkUser(requester)) {
			if (my_gs.groups.contains(groupname)) {
				ArrayList<String> temp = my_gs.userList.getUserOwnership(requester);
				if (temp.contains(groupname)) {

					my_gs.userList.addGroup(username, groupname);
					return true;
				} else {
					return false;//user not group owner
				}
			} else {
				return false;//group does not exist
			}
		} else {
			return false;//user does not exist
		}


	}
	public boolean deleteUserFromGroup(String username, String groupname, UserToken token) {
		String requester = token.getSubject();

		//Does requester exist?
		if (my_gs.userList.checkUser(requester)) {
			if (my_gs.groups.contains(groupname)) {
				ArrayList<String> temp = my_gs.userList.getUserOwnership(requester);
				if (temp.contains(groupname)) {
					if (username.equals(token.getSubject())) { //If the group manager wants to remove them self from the group, the group is deleted as well
						my_gs.userList.removeOwnership(username, groupname);
						my_gs.userList.removeGroupMembers(groupname);
						my_gs.groups.remove(groupname);
						return true;
					} else {
						my_gs.userList.removeGroup(username, groupname);
						return true;
					}

				} else {
					return false;//user not group owner
				}
			} else {
				return false;//group does not exist
			}
		} else {
			return false;//user does not exist
		}


	}
	public List<String> listMembers(final String group, final UserToken token) {
		String requester = token.getSubject();

		if (my_gs.userList.checkUser(requester)) {
			if (my_gs.groups.contains(group)) {
				ArrayList<String> temp = my_gs.userList.getUserOwnership(requester);
				if (temp.contains(group)) {
					return my_gs.userList.getGroupMembers(group);
				} else {
					return null;//user not group owner
				}
			} else {
				return null;//group does not exist
			}
		} else {
			return null;//user does not exist
		}

	}

	/**
	 * Writes an object to output. Will handle all encryption.
	 *
	 * @param      obj   The object
	 *
	 * @return     true if successful, false otherwise
	 */
	public boolean writeObjectToOutput(Envelope obj) {
		try {
			obj.setN(this.n++);
            obj.setHMAC(obj.calcHMAC(this.DH_Key_Integrity));
			this.output.writeObject(EncryptionUtils.encrypt(this.DH_Key_Encryption, obj));
			return true;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return false;
	}
}
