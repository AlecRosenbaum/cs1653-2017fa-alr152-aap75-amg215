/* This list represents the users on the server */
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


	public class UserList implements java.io.Serializable {
	
		/**
		 * 
		 */
		private static final long serialVersionUID = 7600343803563417992L;
		private Hashtable<String, User> list = new Hashtable<String, User>();
		
		public synchronized void addUser(String username, String salt, String password)
		{
			User newUser = new User(salt, password);
			list.put(username, newUser);
		}
		
		public synchronized void deleteUser(String username)
		{
			list.remove(username);
		}
		
		public synchronized boolean checkUser(String username)
		{
			if(list.containsKey(username))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		
		public synchronized ArrayList<String> getUserGroups(String username)
		{
			return list.get(username).getGroups();
		}
		
		public synchronized ArrayList<String> getUserOwnership(String username)
		{
			return list.get(username).getOwnership();
		}
		
		public synchronized ArrayList<String> getGroupMembers(String groupname)
		{
			ArrayList<String> groupmembers = new ArrayList<>(list.keySet());
			ArrayList<User> groupUsers = new ArrayList<>(list.values());
			ArrayList<String> listmembers = new ArrayList<>();
			for(int i = 0; i < groupUsers.size(); i++) {
				
				if(groupUsers.get(i).getGroups().contains(groupname)) {
					
					listmembers.add(groupmembers.get(i));
				}
			}
			
			return listmembers;
		}
		
		public synchronized void removeGroupMembers(String groupname){
			
			ArrayList<String> groupmembers = getGroupMembers(groupname);
			for(int i = 0; i < groupmembers.size(); i++) {
				
				removeGroup(groupmembers.get(i), groupname);
			}
		}
		
		public synchronized void addGroup(String user, String groupname)
		{
			list.get(user).addGroup(groupname);
		}
		
		public synchronized void removeGroup(String user, String groupname)
		{
			list.get(user).removeGroup(groupname);
		}
		
		public synchronized void addOwnership(String user, String groupname)
		{
			list.get(user).addOwnership(groupname);
		}
		
		public synchronized void removeOwnership(String user, String groupname)
		{
			list.get(user).removeOwnership(groupname);
		}
		public synchronized void setPassword(String user, String password) {
			
			list.get(user).setPassword(password.toCharArray());
		}
		public synchronized boolean checkPassword(String user, String password) {
			
			if(list.get(user).checkPassword(password.toCharArray())) {
				
				return true;
			}
			else
			{
				return false;
			}
		}
		
	
	class User implements java.io.Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6699986336399821598L;
		private ArrayList<String> groups;
		private ArrayList<String> ownership;
		private boolean locked = false;
		private byte[] salt = null;
		private byte[] derivedKey = null;
		
		public User(String newSalt, String password)
		{
			salt = newSalt.getBytes();
			groups = new ArrayList<String>();
			ownership = new ArrayList<String>();
			setPassword(password.toCharArray());
		}
		
		public void setPassword(char[] password) {
			
			PBEKeySpec spec = new PBEKeySpec(password, salt, 1024, 256);
			SecretKeyFactory secretKey = null;
			try {
				secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
				
			} catch (NoSuchAlgorithmException e1) {
	
				e1.printStackTrace();
			}
		     try {
		    	 
				derivedKey = secretKey.generateSecret(spec).getEncoded();
				
			} catch (InvalidKeySpecException e) {
	
				e.printStackTrace();
			}
		}
		public boolean checkPassword(char[] password) {
			
			byte[] dk = null;
			PBEKeySpec spec = new PBEKeySpec(password, salt, 1024, 256);
			SecretKeyFactory secretKey = null;
			try {
				secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
				
			} catch (NoSuchAlgorithmException e1) {
	
				e1.printStackTrace();
			}
		     try {
		    	 
				dk = secretKey.generateSecret(spec).getEncoded();
				
			} catch (InvalidKeySpecException e) {
	
				e.printStackTrace();
			}
		    if(Arrays.equals(derivedKey, dk)){
		    	
		    	return true;
		    }
		    return false;
		}
		
		public ArrayList<String> getGroups()
		{
			return new ArrayList<>(groups);
		}
		
		public ArrayList<String> getOwnership()
		{
			return ownership;
		}
		
		public void addGroup(String group)
		{
			groups.add(group);
		}
		
		public void removeGroup(String group)
		{
			if(!groups.isEmpty())
			{
				if(groups.contains(group))
				{
					groups.remove(groups.indexOf(group));
				}
			}
		}
		
		public void addOwnership(String group)
		{
			ownership.add(group);
		}
		
		public void removeOwnership(String group)
		{
			if(!ownership.isEmpty())
			{
				if(ownership.contains(group))
				{
					ownership.remove(ownership.indexOf(group));
				}
			}
		}
		
	}
	
}	
