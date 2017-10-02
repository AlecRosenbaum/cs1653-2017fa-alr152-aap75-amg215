# Usage Instructions
 There are shortcuts for the running of the group and file server along with the client in our makefile. Run `make run_group_server`, ` make run_file_server`, and `make run_client` to check them out.

## Running the Group Server

To start the Group Server:
 - Enter the directory containing `RunGroupServer.class`
 - Type `java RunGroupServer [port number]`

Note that the port number argument to `RunGroupServer` is optional.  This argument specifies the port that the Group Server will listen to.  If unspecified, it defaults to port 8765.

When the group server is first started, there are no users or groups. Since there must be an administer of the system, the user is prompted via the console to enter a username. This name becomes the first user and is a member of the *ADMIN* group.  No groups other than *ADMIN* will exist.

## Running the File Server

To start the File Server:
 - Enter the directory containing `RunFileServer.class`
 - Type `java RunFileServer [port number]`

Note that the port number argument to `RunFileServer is optional.  This argument speficies the port that the File Server will list to. If unspecified, it defaults to port 4321.

The file server will create a shared_files inside the working directory if one does not exist. The file server is now online.

## Resetting the Group or File Server

To reset the Group Server, delete the file `UserList.bin`

To reset the File Server, delete the `FileList.bin` file and the `shared_files/` directory.

## Running the Client

To run the client type `java RunClient localhost 8765 localhost 4321`. The arguments are the group server url then group server port then file server url then file server port. The above command will start the client if you started the file and group servers with their default configurations.

To login use the user name you entered when creating the group server as initially that will be the only user.

You can enter `help` at any time to view all commands but the available commands are as follows:

##### File Server Commands:
- fconnect [url] [port]
    - Connects to new file server      
- fdisconnect 
    - Disconnects from current file server.            
- listfiles 
    - Lists file in current file server.        
- upload [sourcefile] [destinationfile] [group] 
    - Upload file to file server.  
- download [remotefile] [localfile] 
    - Download file from file server.  
- delete [filename] 
    - Delete file from file server.  
##### Group Server Commands 
- gconnect [url] [port] 
    - Connects to new group server.                             
- gdisconnect 
    - Disconnects from current group server.                        
- changeuser [username] 
    - Changes current user.                   
- createuser [username] 
    - Creates new user, does NOT switch to that user. 
- deleteuser [username] 
    - Deletes existing user. 
- creategroup [groupname] 
    - Creates new group. 
- deletegroup [groupname] 
    - Deletes an existing groups. 
- addgroupuser [username] [groupname] 
    - Adds user to an existing group. 
- deletegroupuser [username] [groupname] 
    - Deletes user from a group. 
- listmembers [groupname] 
    - Lists all users in given group.