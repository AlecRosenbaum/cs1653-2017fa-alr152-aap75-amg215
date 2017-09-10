# Phase 1 Report

## Group Information

* Alec Rosenbaum
    - alr152@pitt.edu
    - alecrosenbaum

## Security Properties

* **Authentication:** Authentication states that users self-identifying as user *u* should be validated as being user *u*. Without ensuring users are who they say they are, users can identify as other users, thus basically nullifying any permission-scheme setup.
* **File Authorization:** File Authorization states that file *f* belonging to group *g* may only be overwritten, downloaded, or deleted by users belonging to group *g*. Without this, any user could act on any file, directly going against the notion of group-based file sharing.
* **Least Permission:** Least Permission states that user *u* may only create new users with fewer permissions than *u* has at the time of creation. This requirement prevent users from effectively elevating their permissions by creating new accounts.
* **Group Permissions:** Group Deletion states that user *u* must be a member of group *g* in order to see the existence of *g*, add or remove members from *g*, or in order to delete *g* entirely. This requirement prevents users from acting on groups they do not belong to.
* **Transmission Integrity:** Transimission Integrity states that any communication between two parties in the system shall be done such that it is possible to verify communication integrity. This requirement prevents data from being altered at the time of transmission by a third-party observer.
* **Transmission Confidentiality:** Transmission Confidentiality states that and communication between to parties in the system will be done such that an unauthorized third party may not know the contents of said transmission. This requirement prevents eavesdropping while a file is being transferred.
* **Server Port Firewall:** Server Port Firewall states that the only ports open for communication on File Servers will be used for the file sharing application. This requiremnt prevents unauthorized access to the system through exploiting weaknesses in other applications.
* **IP Firewall:** IP Firewall states that a firewall run on file servers will block any non-whitelisted IP addresses (or IP-ranges). This requirement prevents any access to the server from outside of a pre-approved network. 



## Threat Models

### University Wireless Network

#### System Model

The system will be deployed within a university to facilitate file sharing between members of technical staff. Servers will only be only accessible from IP's originating from within the University's network. Users will autheniticate using their university-given accounts, and will be assigned a base space allotment to store files.

#### Assumption

Though servers on this network are internet-connected, incoming traffic will only be allowed from the University's network, and only on designated ports. Communications may be occuring wirelessly; it is assumed that anyone may listen to or intercept any traffic at any time. It is assumed that everyone able to connect to the network will be allowed access to the system.

#### Security Properties

* IP Firewall
    - This requirement will prevent access by users with IP's not originating from within the University's network.
* Server Port Firewall
    - This requirement will prevent attacks exploiting weaknesses in other software being run on the file sharing servers besides the file sharing application.
* Transmission Integrity
    - Communication may be done wirelessly in this system. It is important to verify that the communication was not tampered with during wireless (or wired) transmission.
* Transmission Confidentiality
    - Communication will be occuring using (relatively) public channels. Any data sent over these channels should not be visible to any unauthorized third-party.


### Threat Model 1

#### System Model

Lorem ipsum...

#### Assumption

Lorem ipsum...

#### Security Properties

* example property
    - how it applies to this system

## References
