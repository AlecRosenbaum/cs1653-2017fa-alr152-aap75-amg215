/* Driver program for Client */
import java.util.ArrayList;

public class RunClient {
    
    public static void main(String[] args) {
        // args:
        //  - group server url
        //  - group server port
        //  - file server url
        //  - file server port

        if (args.length != 4 && args.length != 5) { //allow 5 for optional test arg
            System.out.println("Arguments are incorrect.");
            return;
        }

        // parse args
        String group_server_url = args[0];;
        int group_server_port;
        String file_server_url = args[2];
        int file_server_port;
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
        GroupClient group_client = new GroupClient();
        if (group_client.connect(group_server_url, group_server_port)) {
            System.out.println("Connected to group server " + group_server_url + ":" + group_server_port);
        } else {
            System.out.println("Unable to connect to group server " + group_server_url + ":" + group_server_port);
        }

        FileClient file_client = new FileClient();
        if (file_client.connect(file_server_url, file_server_port)) {
            System.out.println("Connected to file server " + file_server_url + ":" + file_server_port);
        } else {
            System.out.println("Unable to connect to file server " + file_server_url + ":" + file_server_port);
        }

        //Test Code in Here
        if (args.length == 5 && args[5].equals("test")) {
            // get a token
            System.out.println("Running Tests");
            UserToken mytoken = group_client.getToken("alec");
            if (mytoken == null) {
                System.out.println("Token creation unsucessful.");
            } else {
                System.out.println("Token creation sucessful: " + mytoken.getSubject());
            }


            // upload a file
            file_client.upload(".gitignore", "test", "ADMIN", mytoken);

            // list files
            ArrayList<String> files = (ArrayList<String>)file_client.listFiles(mytoken);
            for (String file: files) {
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
            for (String member: members) {
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
            for (String member: members) {
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
            for (String member: members) {
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


}
