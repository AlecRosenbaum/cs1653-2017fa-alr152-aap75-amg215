/* Driver program for Client */
import java.util.ArrayList;
import java.io.*;
import java.util.*;


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
        Scanner console = new Scanner(System.in);
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

        System.out.print("Enter user name: ");
        String u = console.nextLine();
        UserToken mytoken = group_client.getToken(u);
        if (mytoken == null) {
            System.out.println("Login Unsuccessful");
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
                                         "\tdownload [remoteFile] [localFile]\n" +
                                         "\t\tDownload file from file server.\n" +
                                         "\tdelete [filename]\n" +
                                         "\t\tDelete file from file server.\n" +
                                         "\nGroup Server Commands\n" +
                                         "\tgconnect [url] [port]\n" +
                                         "\t\tConnects to new group server.\n" +
                                         "\tgdisconnect\n" +
                                         "\t\tDisconnects from current group server.\n" +
                                         "\tchangeuser [username]\n" +
                                         "\t\tChanges current user.\n" +
                                         "\tcreateuser [username]\n" +
                                         "\t\tCreates new user, does NOT switch to that user.\n" +
                                         "\tdeleteuser [username]\n" +
                                         "\t\tDeletes existing user.\n" +
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
                        FileClient new_file_client = new FileClient();
                        if (new_file_client.connect(url, port)) {
                            System.out.println("Connected to file server " + url + ":" + port);
                            file_client = new_file_client;
                            file_server_url = url;
                            file_server_port = port;
                        } else {
                            System.out.println("Unable to connect to file server " + url + ":" + port);
                        }
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
                        file_client.upload(localFile, remoteFile, groupName, mytoken);
                        System.out.println("Uploaded " + localFile + " to " + remoteFile + ".");
                        break;
                    case "download":
                        remoteFile = inputArray[1];
                        localFile = inputArray[1];
                        file_client.download(remoteFile, localFile, mytoken);
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
                        if (new_group_client.connect(url, port)) {
                            System.out.println("Connected to group server " + url + ":" + port);
                            group_client = new_group_client;
                            group_server_url = url;
                            group_server_port = port;
                        } else {
                            System.out.println("Unable to connect to file server " + url + ":" + port);
                        }
                        break;
                    case "gdisconnect":
                        group_client.disconnect();
                        System.out.println("Disconnected");
                        break;
                    case "changeuser":
                        userName = inputArray[1];
                        UserToken newToken = group_client.getToken(userName);
                        if (newToken == null) {
                            System.out.println("Invalid user.");
                        } else {
                            System.out.println("Switched to user " + userName);
                            mytoken = newToken;
                        }
                        break;
                    case "createuser":
                        userName = inputArray[1];
                        if (group_client.createUser(userName, mytoken)) {
                            System.out.println("Created user " + userName);
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
                        break;
                    case "deletegroup":
                        groupName = inputArray[1];
                        if (group_client.deleteGroup(groupName, mytoken)) {
                            System.out.println("Deleted group " + groupName);
                        } else {
                            System.out.println("Unable to delete group.");
                        }
                        break;
                    case "addgroupuser":
                        userName = inputArray[1];
                        groupName = inputArray[2];
                        if (group_client.addUserToGroup(userName, groupName, mytoken)) {
                            System.out.println("Added user " + userName + " to " + groupName);
                        } else {
                            System.out.println("Unable to add user to group");
                        }
                        break;
                    case "deletegroupuser":
                        userName = inputArray[1];
                        groupName = inputArray[2];
                        if (group_client.deleteUserFromGroup(userName, groupName, mytoken)) {
                            System.out.println("Deleted user " + userName + " from " + groupName);
                        } else {
                            System.out.println("Unable to delete user from group");
                        }
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
                group_client.disconnect();
                group_client.connect(group_server_url, group_server_port);
                mytoken = group_client.getToken(mytoken.getSubject()); //refresh token
            } while (true);
        }
        // Test Code In Here
        else {
            // get a token
            System.out.println("Running Tests");
            mytoken = group_client.getToken("aadu");
            if (mytoken == null) {
                System.out.println("Token creation unsucessful.");
            } else {
                System.out.println("Token creation sucessful: " + mytoken.getSubject());
            }


            // upload a file
            file_client.upload(".gitignore", "test", "ADMIN", mytoken);

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


}
