/* Driver program for Client */
import java.util.ArrayList;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import java.util.*;
import java.lang.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class RunClient {

    private static String fileServersPath = "APPROVED_FILE_SERVERS.bin";    
    private static String groupServersPath = "APPROVED_GROUP_SERVER.bin";

    private static Scanner console;
    private static String group_server_url;
    private static int group_server_port;
    private static String file_server_url;
    private static int file_server_port;                
    private static GroupClient group_client;  
    private static GroupClient new_group_client;
    private static FileClient file_client;
    private static FileClient new_file_client;
    private static PublicKey fileServerPublicKey;    
    private static PublicKey groupServerPublicKey;
    private static ApprovedServerList approvedFileServers;    
    private static ApprovedServerList approvedGroupServers;    

    public static boolean fconnect(String url, int port) {
        new_file_client = new FileClient();
        fileServerPublicKey = approvedFileServers.checkServer(url, port);
        if(fileServerPublicKey == null) {
            fileServerPublicKey = new_file_client.initialConnect(url, port);
            try {
                System.out.print("File Server Provided Public Key Fingerprint For Authentication:\n\n\t" + prettify(EncryptionUtils.hash(fileServerPublicKey.getEncoded())) + "\n\n" +
                "Entery 'Y' to accept or 'N' to reject: ");
            } catch (Exception e)
            {
                return false;
            }     
            if(console.nextLine().toUpperCase().equals("Y")) {
                approvedFileServers.addServer(fileServerPublicKey, url, port);
                new_file_client = new FileClient();
            }else {
                System.out.println("File Server Public Key NOT approved.");
                return false;
            }
        }
        if (new_file_client.connect(url, port, fileServerPublicKey)) {
            System.out.println("Connected to file server " + url + ":" + port);
            file_client = new_file_client;
            file_server_url = url;
            file_server_port = port;
            return true;
        } else {
            System.out.println("Unable to connect to file server " + url + ":" + port);
            return false;
        }
    }

    public static boolean gconnect(String url, int port) {
        new_group_client = new GroupClient();
        groupServerPublicKey = approvedGroupServers.checkServer(url, port);
        if(groupServerPublicKey == null) {
            groupServerPublicKey = new_group_client.initialConnect(url, port);
            try {
                System.out.print(" Group Server Provided Public Key Fingerprint For Authentication:\n\n\t" + prettify(EncryptionUtils.hash(groupServerPublicKey.getEncoded())) + "\n\n" +
                "Entery 'Y' to accept or 'N' to reject: ");
            } catch (Exception e)
            {
                return false;
            }     
            if(console.nextLine().toUpperCase().equals("Y")) {
                approvedGroupServers.addServer(groupServerPublicKey, url, port);
                new_group_client = new GroupClient();
            }else {
                System.out.println("Group Server Public Key NOT approved.");
                return false;
            }
        }
        if (new_group_client.connect(url, port, groupServerPublicKey)) {
            System.out.println("Connected to group server " + url + ":" + port);
            group_client = new_group_client;
            group_server_url = url;
            group_server_port = port;
            return true;
        } else {
            System.out.println("Unable to connect to group server " + url + ":" + port);
            return false;
        }
    }

    public static void main(String[] args) {
        // args:
        //  - group server url
        //  - group server port
        //  - file server url
        //  - file server port
        //  
        Security.addProvider(new BouncyCastleProvider());

        if (args.length != 4 && args.length != 5) { //allow 5 for optional test arg
            System.out.println("Arguments are incorrect.");
            return;
        }

        // parse args
        console = new Scanner(System.in);
        group_server_url = args[0];
        file_server_url = args[2];                
        group_client = new GroupClient();        
        new_group_client = new GroupClient();
        file_client = new FileClient();
        new_file_client = new FileClient();
        fileServerPublicKey = null;
        approvedFileServers = new ApprovedServerList(true);
        try {
            File approvedFileServersFile = new File(fileServersPath);
            if(approvedFileServersFile.exists() && !approvedFileServersFile.isDirectory()) { 
                FileInputStream fis = new FileInputStream(fileServersPath);
                ObjectInputStream fileStream = new ObjectInputStream(fis);
                approvedFileServers = (ApprovedServerList)fileStream.readObject();
            }
        } catch(Exception e) {
            System.out.println("Error loading approved file servers.");            
            System.out.println(e.getCause());
            return;
        }

        approvedGroupServers = new ApprovedServerList(false);
        try {
            File approvedGroupServersFile = new File(groupServersPath);
            if(approvedGroupServersFile.exists() && !approvedGroupServersFile.isDirectory()) { 
                FileInputStream fis = new FileInputStream(groupServersPath);
                ObjectInputStream fileStream = new ObjectInputStream(fis);
                approvedGroupServers = (ApprovedServerList)fileStream.readObject();
                System.out.println("Loaded approved GS");
            }
        } catch(Exception e) {
            System.out.println("Error loading approved group servers.");            
            System.out.println(e.getCause());
            return;
        }
        
        try  {
            group_server_port = Integer.parseInt(args[1]);
            file_server_port = Integer.parseInt(args[3]);
        } catch (Exception e) {
            System.out.println("Arguments are incorrect.");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            return;
        }

        
        // instantiate file client and group client


        System.out.println("Connecting to group server...");
        boolean success = false;
        boolean firstTime = true;        
        do {
            if(!firstTime) {
                System.out.print("Enter a group server url: ");
                group_server_url = console.nextLine();
                System.out.print("Enter a group server port: ");
                group_server_port = Integer.valueOf(console.nextLine());
            }
            firstTime = false;
            if(gconnect(group_server_url, group_server_port)) {
                success = true;
            }
        } while (!success);  

        System.out.println("Connecting to file server...");
        success = false;
        firstTime = true;        
        do {
            if(!firstTime) {
                System.out.print("Enter a file server url: ");
                file_server_url = console.nextLine();
                System.out.print("Enter a file server port: ");
                file_server_port = Integer.valueOf(console.nextLine());
            }
            firstTime = false;            
            if(fconnect(file_server_url, file_server_port)) {
                success = true;
            }
        } while (!success);
        
        System.out.print("Enter user name: ");
        String u = console.nextLine();
        System.out.print("Enter password: ");
        String p = console.nextLine();
        UserToken mytoken = group_client.getToken(u, p, EncryptionUtils.hash(fileServerPublicKey.getEncoded()));
        if (mytoken == null) {
            System.out.println("Login Unsuccessful");
            return;
        } else {
            System.out.println("Login sucessful: " + mytoken.getSubject());
        }

        //Client Code In Here
        if (args.length != 5 || !args[4].equals("test")) {
            System.out.print("Welcome to Aadu, Alec, and Alex's File Server.\n\n" +
                             "Connected to the Group Server at " + group_server_url + ":" + group_server_port + "\n" +
                             "Connected to the File Server at " + file_server_url + ":" + file_server_port + "\n" +
                             "Connected as " + mytoken.getSubject() + "\n" +
                             "Enter 'help' for help. \n\n"
                            );

            // define common variables, then run main loop
            String input, groupName, userName, url, localFile, remoteFile;
            String[] inputArray;
            int port;
            do {
                System.out.print("<" + mytoken.getSubject() + "> Enter command: ");
                input = console.nextLine();
                inputArray = input.split(" ");

                try {

                    switch (inputArray[0].toLowerCase()) {
                    case "help":
                        System.out.print("These are the available commands: \n\n" +
                                         "File Server Commands:\n" +
                                         "\tfconnect [url] [port]\n" +
                                         "\t\tConnects to new file server.\n" +
                                         "\tfdisconnect\n" +
                                         "\t\tDisconnects from current file server.\n" +
                                         "\tlistfiles\n" +
                                         "\t\tLists file in current file server.\n" +
                                         "\tupload [sourcefile] [destinationfile] [group]\n" +
                                         "\t\tUpload file to file server.\n" +
                                         "\tdownload [remoteFile] [localFile] [groupName]\n" +
                                         "\t\tDownload file from file server.\n" +
                                         "\tdelete [filename]\n" +
                                         "\t\tDelete file from file server.\n" +
                                         "\nGroup Server Commands\n" +
                                         "\tgconnect [url] [port]\n" +
                                         "\t\tConnects to new group server.\n" +
                                         "\tgdisconnect\n" +
                                         "\t\tDisconnects from current group server.\n" +
                                         "\tchangeuser [username] [password]\n" +
                                         "\t\tChanges current user.\n" +
                                         "\tcreateuser [username]\n" +
                                         "\t\tCreates new user, does NOT switch to that user.\n" +
                                         "\tdeleteuser [username]\n" +
                                         "\t\tDeletes existing user.\n" +
                                         "\tchangepassword [user] [current password] [new password]\n" +
                                         "\t\tChanges a user's password.\n" +
                                         "\tcreategroup [groupname]\n" +
                                         "\t\tCreates new group.\n" +
                                         "\tdeletegroup [groupname]\n" +
                                         "\t\tDeletes an existing groups.\n" +
                                         "\taddgroupuser [username] [groupname]\n" +
                                         "\t\tAdds user to an existing group.\n" +
                                         "\tdeletegroupuser [username] [groupname]\n" +
                                         "\t\tDeletes user from a group.\n" +
                                         "\tlistmembers [groupname]\n" +
                                         "\t\tLists all users in given group.\n\n"
                                        );
                        break;
                    case "fconnect":                        
                        url = inputArray[1];
                        port = Integer.parseInt(inputArray[2]);
                        fconnect(url, port);
                        break;
                    case "fdisconnect":
                        file_client.disconnect();
                        System.out.println("Disconnected");
                        break;
                    case "listfiles":
                        List<String> fileList = file_client.listFiles(mytoken);
                        System.out.println("Files in " + file_server_url + ":" + file_server_port);
                        for (String file : fileList) {
                            System.out.println(file);
                        }
                        break;
                    case "upload":
                        localFile = inputArray[1];
                        remoteFile = inputArray[2];
                        groupName = inputArray[3];
                        FileKey fk = group_client.uploadFile(mytoken, groupName);
                        if(fk == null) {
                            System.out.println("Unauthorized");
                            break;
                        }
                        file_client.upload(localFile, remoteFile, groupName, mytoken, fk);
                        break;
                    case "download":
                        remoteFile = inputArray[1];
                        localFile = inputArray[2];
                        groupName = inputArray[3];
                        byte [] metadata = file_client.download(remoteFile, localFile, mytoken);
                        SecretKey s = group_client.downloadFile(mytoken, groupName, metadata);
                        if(s == null) {
                            System.out.println("Unauthorized");
                            break;
                        }
                        File local = new File(localFile);
                        byte[] ciphertext = new byte[(int) local.length()];            
                        FileInputStream fis = new FileInputStream(local);
                        fis.read(ciphertext);            
                        EncryptionUtils.decryptToFile(s, ciphertext, new File(localFile));
                        fis.close();
                        System.out.println("Downloaded " + remoteFile + " to " + localFile + ".");
                        break;
                    case "delete":
                        String file = inputArray[1];
                        file_client.delete(file, mytoken);
                        System.out.println("Deleted " + file);
                        break;
                    case "gconnect":
                        url = inputArray[1];
                        port = Integer.parseInt(inputArray[2]);
                        GroupClient new_group_client = new GroupClient();
                        if (new_group_client.connect(url, port, null)) {
                            System.out.println("Connected to group server " + url + ":" + port);
                            group_client = new_group_client;
                            group_server_url = url;
                            group_server_port = port;
                        } else {
                            System.out.println("Unable to connect to group server " + url + ":" + port);
                        }
                        break;
                    case "gdisconnect":
                        group_client.disconnect();
                        System.out.println("Disconnected");
                        break;
                    case "changeuser":
                        u = inputArray[1];
                        p = inputArray[2];
                        UserToken newToken = group_client.getToken(u, p,  EncryptionUtils.hash(fileServerPublicKey.getEncoded()));
                        if (newToken == null) {
                            System.out.println("User change failed.");
                        } else {
                            System.out.println("Switched to user " + u);
                            mytoken = newToken;
                        }
                        mytoken = group_client.getToken(mytoken.getSubject(), p, EncryptionUtils.hash(fileServerPublicKey.getEncoded())); //refresh token
                        break;
                    case "changepassword":
                        u = inputArray[1];
                        p = inputArray[2];
                        String newP = inputArray[3];
                        if (group_client.changePassword(mytoken, u, p, newP)) {
                            System.out.println("Password change sucessful.");
                        } else {
                            System.out.println("Password change failed.");
                        }
                        break;
                    case "createuser":
                        userName = inputArray[1];
                        String newPass = group_client.createUser(userName, mytoken);
                        if (newPass != null) {
                            System.out.println("Created user " + userName);
                            System.out.println("Initial Password: " + newPass);
                        } else {
                            System.out.println("User creation failed.");
                        }
                        break;
                    case "deleteuser":
                        userName = inputArray[1];
                        if (group_client.deleteUser(userName, mytoken)) {
                            System.out.println("Deleted user " + userName);
                        } else {
                            System.out.println("Unable to delete user");
                        }
                        break;
                    case "creategroup":
                        groupName = inputArray[1];
                        if (group_client.createGroup(groupName, mytoken)) {
                            System.out.println("Created group " + groupName);
                        } else {
                            System.out.println("Unable to create group.");
                        }
                        mytoken = group_client.getToken(mytoken.getSubject(), p, EncryptionUtils.hash(fileServerPublicKey.getEncoded())); //refresh token
                        break;
                    case "deletegroup":
                        groupName = inputArray[1];
                        if (group_client.deleteGroup(groupName, mytoken)) {
                            System.out.println("Deleted group " + groupName);
                        } else {
                            System.out.println("Unable to delete group.");
                        }
                        mytoken = group_client.getToken(mytoken.getSubject(), p, EncryptionUtils.hash(fileServerPublicKey.getEncoded())); //refresh token
                        break;
                    case "addgroupuser":
                        userName = inputArray[1];
                        groupName = inputArray[2];
                        if (group_client.addUserToGroup(userName, groupName, mytoken)) {
                            System.out.println("Added user " + userName + " to " + groupName);
                        } else {
                            System.out.println("Unable to add user to group");
                        }
                        mytoken = group_client.getToken(mytoken.getSubject(), p, EncryptionUtils.hash(fileServerPublicKey.getEncoded())); //refresh token
                        break;
                    case "deletegroupuser":
                        userName = inputArray[1];
                        groupName = inputArray[2];
                        if (group_client.deleteUserFromGroup(userName, groupName, mytoken)) {
                            System.out.println("Deleted user " + userName + " from " + groupName);
                        } else {
                            System.out.println("Unable to delete user from group");
                        }
                        mytoken = group_client.getToken(mytoken.getSubject(), p, EncryptionUtils.hash(fileServerPublicKey.getEncoded())); //refresh token
                        break;
                    case "listmembers":
                        groupName = inputArray[1];
                        List<String> groupList = group_client.listMembers(groupName, mytoken);
                        System.out.println("Members in " + groupName);
                        for (String group : groupList) {
                            System.out.println(group);
                        }
                        break;
                    case "exit":
                        // teardown
                        group_client.disconnect();
                        file_client.disconnect();
                        return;
                    default:
                        System.out.println("That command wasn't recognized.");
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    System.out.println("Error. Try again.");
                }

                // reconnect to groupserver, refresh token
                // mytoken = group_client.getToken(mytoken.getSubject(), password); //refresh token
            } while (true);
        }
        // Test Code In Here
        else {
            // get a token
            System.out.println("Running Tests");
            mytoken = group_client.getToken("aadu" ,"admin", EncryptionUtils.hash(fileServerPublicKey.getEncoded()));
            if (mytoken == null) {
                System.out.println("Token creation unsucessful.");
            } else {
                System.out.println("Token creation sucessful: " + mytoken.getSubject());
            }


            // upload a file
            //file_client.upload(".gitignore", "test", "ADMIN", mytoken);

            // list files
            ArrayList<String> files = (ArrayList<String>)file_client.listFiles(mytoken);
            for (String file : files) {
                System.out.println("File: " + file);
            }
            System.out.println("---------------");

            // create group
            if (group_client.createGroup("test_group", mytoken)) {
                System.out.println("Group Created");
            } else {
                System.out.println("Group not created.");
                return;
            }

            // list members
            ArrayList<String> members = (ArrayList<String>)group_client.listMembers("test_group", mytoken);
            for (String member : members) {
                System.out.println("Member: " + member);
            }
            System.out.println("---------------");

            // add user to group
            group_client.createUser("new_user", mytoken);
            if (group_client.addUserToGroup("new_user", "test_group", mytoken)) {
                System.out.println("User added to group.");
            } else {
                System.out.println("User not added to group.");
                return;
            }

            // list members
            members = (ArrayList<String>)group_client.listMembers("test_group", mytoken);
            for (String member : members) {
                System.out.println("Member: " + member);
            }
            System.out.println("---------------");

            // remove user from group
            if (group_client.deleteUserFromGroup("new_user", "test_group", mytoken)) {
                System.out.println("User deleted from group.");
            } else {
                System.out.println("User not deleted from group.");
                return;
            }

            // list members
            members = (ArrayList<String>)group_client.listMembers("test_group", mytoken);
            for (String member : members) {
                System.out.println("Member: " + member);
            }
            System.out.println("---------------");

            // delete group
            if (group_client.deleteGroup("test_group", mytoken)) {
                System.out.println("Group deleted.");
            } else {
                System.out.println("Group not deleted.");
                return;
            }

            // teardown
            group_client.disconnect();
            file_client.disconnect();
        }
    }
    
    public static String prettify(byte[] input) {
        StringBuilder sb = new StringBuilder(input.length * 2);
        for(byte b : input) {
            sb.append(String.format("%02x", b));
        }
        
        String output = "";
        int counter = 0;
        for(char c : sb.toString().toCharArray())
        {
            counter++;
            output = output + c;
            if(counter == 2) {
                counter = 0;
                output = output + ":"; 
            }
        }
        if(output.charAt(output.length() - 1) == ':')
        {
            sb = new StringBuilder(output);
            sb.deleteCharAt(output.length() - 1);
            output = sb.toString();
        }
        return output.toUpperCase();
    }
}

class ApprovedServerList implements java.io.Serializable {
    private static final long serialVersionUID = -8911161283900345136L;
    ArrayList<PublicKey> pks;
    ArrayList<String> urls;
    ArrayList<Integer> ports;
    boolean isFile;
    private static String fileServersPath = "APPROVED_FILE_SERVERS.bin";    
    private static String groupServersPath = "APPROVED_GROUP_SERVER.bin";

    public ApprovedServerList (boolean ifi) {
        pks = new ArrayList<PublicKey>();
        urls = new ArrayList<String>();
        ports = new ArrayList<Integer>();
        isFile = ifi;
    }

    public void addServer(PublicKey new_pk, String new_url, int new_port) {
        pks.add(new_pk);
        urls.add(new_url);
        ports.add(new Integer(new_port));
        backup();
    }

    public PublicKey checkServer(String url, int port) {
        if(urls.contains(url) && ports.contains(new Integer(port)))
        {
            if(urls.indexOf(url) == ports.indexOf(new Integer(port))) {
                return pks.get(urls.indexOf(url));
            }
        }
        return null;
    }

    public void backup() {
        try {
            ObjectOutputStream outStream;
            if(isFile)
                outStream = new ObjectOutputStream(new FileOutputStream(fileServersPath));
            else 
                outStream = new ObjectOutputStream(new FileOutputStream(groupServersPath));
            outStream.writeObject(this);
            outStream.close();
        } catch(Exception e) {
            System.out.println("Error backing up approved servers.");
        }
        
    }
}
