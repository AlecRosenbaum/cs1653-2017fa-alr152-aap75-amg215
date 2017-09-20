/* Driver program for FileSharing File Server */

public class RunClient {
    
    public static void main(String[] args) {
        // args:
        //  - group server url
        //  - group server port
        //  - file server url
        //  - file server port
        
        if (args.length != 4) {
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
        if (!group_client.connect(group_server_url, group_server_port)) {
            System.out.println("Unable to connect to group server " + group_server_url + ":" + group_server_port);
        }

        FileClient file_client = new FileClient();
        if (!file_client.connect(file_server_url, file_server_port)) {
            System.out.println("Unable to connect to group server " + file_server_url + ":" + file_server_port);
        }
        
        // get a token
        UserToken mytoken = group_client.getToken("alec");
        if (mytoken == null) {
            System.out.println("unsucessful.");
        } else {
            System.out.println("it worked");
        }


        // create a group if it doesn't exist
        

        // upload a file


        // list files


        // teardown
        group_client.disconnect();
        file_client.disconnect();
    }


}
