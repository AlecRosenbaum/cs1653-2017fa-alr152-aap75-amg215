/* This list represents the users on the server */
import java.util.*;


	public class UserList implements java.io.Serializable {
	
		/**
		 * 
		 */
		private static final long serialVersionUID = 7600343803563417992L;
		private Hashtable<String, User> list = new Hashtable<String, User>();
		
		public synchronized void addUser(String username, String password)
		{
			User newUser = new User(password);
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

		public synchronized boolean validate(String user, String password) {
			return list.get(user).checkPassword(password);
		}
		
	
	class User implements java.io.Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6699986336399821598L;
		private ArrayList<String> groups;
		private ArrayList<String> ownership;
		private String password;
		
		public User(String password)
		{
			groups = new ArrayList<String>();
			ownership = new ArrayList<String>();
			this.password = password;
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

		public boolean checkPassword(String password) {
			return this.password.equals(password);
		}
		
		public void setPassword(String password) {
			this.password = password;
		}

	}
	
}	
