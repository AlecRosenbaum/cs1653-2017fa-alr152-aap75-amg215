/* Implements the GroupClient Interface */

import java.util.ArrayList;
import java.util.List;
import java.net.Socket;
import javax.crypto.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;

public class GroupClient extends Client implements GroupClientInterface {

	public UserToken getToken(String username, String password, byte[] fingerprint) {
		try {
			UserToken token = null;
			Envelope message = null, response = null;

			//Tell the server to return a token.
			message = new Envelope("GET");
			message.addObject(username); //Add user name string
			message.addObject(password); //Add password string
			message.addObject(fingerprint); //Add fileserver fingerprint
			this.writeObjectToOutput(message);

			//Get the response from the server
			response = (Envelope) this.readObjectFromInput();

			//Successful response
			if (response.getMessage().equals("OK")) {
				//If there is a token in the Envelope, return it
				ArrayList<Object> temp = null;
				temp = response.getObjContents();

				if (temp.size() == 1) {
					token = (UserToken)temp.get(0);
					return token;
				}
			}

			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			return null;
		}

	}

	public String createUser(String username, UserToken token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to create a user
			message = new Envelope("CUSER");
			message.addObject(username); //Add user name string
			message.addObject(token); //Add the requester's token
			this.writeObjectToOutput(message);


			response = (Envelope) this.readObjectFromInput();

			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return (String)response.getObjContents().get(0); // the random password string
			}

			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean deleteUser(String username, UserToken token) {
		try {
			Envelope message = null, response = null;

			//Tell the server to delete a user
			message = new Envelope("DUSER");
			message.addObject(username); //Add user name
			message.addObject(token);  //Add requester's token
			this.writeObjectToOutput(message);

			response = (Envelope) this.readObjectFromInput();

			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}

			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean createGroup(String groupname, UserToken token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to create a group
			message = new Envelope("CGROUP");
			message.addObject(groupname); //Add the group name string
			message.addObject(token); //Add the requester's token
			this.writeObjectToOutput(message);

			response = (Envelope) this.readObjectFromInput();

			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}

			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteGroup(String groupname, UserToken token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to delete a group
			message = new Envelope("DGROUP");
			message.addObject(groupname); //Add group name string
			message.addObject(token); //Add requester's token
			this.writeObjectToOutput(message);

			response = (Envelope) this.readObjectFromInput();
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}

			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> listMembers(String group, UserToken token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to return the member list
			message = new Envelope("LMEMBERS");
			message.addObject(group); //Add group name string
			message.addObject(token); //Add requester's token
			this.writeObjectToOutput(message);

			response = (Envelope) this.readObjectFromInput();

			//If server indicates success, return the member list
			if (response.getMessage().equals("OK")) {
				return (List<String>)response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
			}

			return null;

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
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

	public boolean addUserToGroup(String username, String groupname, UserToken token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to add a user to the group
			message = new Envelope("AUSERTOGROUP");
			message.addObject(username); //Add user name string
			message.addObject(groupname); //Add group name string
			message.addObject(token); //Add requester's token
			this.writeObjectToOutput(message);

			response = (Envelope) this.readObjectFromInput();
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}

			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteUserFromGroup(String username, String groupname, UserToken token) {
		try {
			Envelope message = null, response = null;
			//Tell the server to remove a user from the group
			message = new Envelope("RUSERFROMGROUP");
			message.addObject(username); //Add user name string
			message.addObject(groupname); //Add group name string
			message.addObject(token); //Add requester's token
			this.writeObjectToOutput(message);

			response = (Envelope) this.readObjectFromInput();
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}

			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean changePassword(UserToken token, String username, String oldPass, String newPass) {
		try {
			Envelope message = null, response = null;
			message = new Envelope("CPASSWORD");
			message.addObject(token); //Add token
			message.addObject(username); //Add user name string
			message.addObject(oldPass); //Add group name string
			message.addObject(newPass); //Add requester's token
			this.writeObjectToOutput(message);

			response = (Envelope) this.readObjectFromInput();
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}

			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public FileKey uploadFile(UserToken token, String group) {
		try {
			Envelope message = null, response = null;
			message = new Envelope("UFILE");
			message.addObject(token); //Add token
			message.addObject(group); //Add group string
			this.writeObjectToOutput(message);

			response = (Envelope) this.readObjectFromInput();
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return (FileKey)response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
			}
			System.out.println("ERROR getting key for group");
			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public SecretKey downloadFile(UserToken token, String group, byte[] encryptedKey) {
		try {
			Envelope message = null, response = null;
			message = new Envelope("DFILE");
			message.addObject(token); //Add token
			message.addObject(group); //Add group string
			message.addObject(encryptedKey);
			this.writeObjectToOutput(message);

			response = (Envelope) this.readObjectFromInput();
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return (SecretKey)response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
			}

			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

}
