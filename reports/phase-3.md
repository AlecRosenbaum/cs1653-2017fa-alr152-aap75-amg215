# Phase 3 Writeup

## Group Information

* Alec Rosenbaum
    - alr152@pitt.edu
    - alecrosenbaum
* Alex George 
    - amg215@pitt.edu
    - alexgeorge222
* Aadu Pirn
    - aap75@pitt.edu
    - aadupirn


## Cryptographic Mechanisms and Protocols

### Symmetric Key Cryptography

The Symmetric Key Cryptography algorithm used in addressing these threat models is AES-128. AES-128 utilizes a 128-bit key, and is considered sufficiently secure according to NIST standards published in 2016, as summarized by https://www.keylength.com/en/4/. 

### Public Key Cryptography

All public key cryptography used in addressing these threat models is implemented using RSA. Current NIST standards indicate that 2048-bit groups with 224-bit keys provide sufficient security for algorithms based on discrete logarithms.

### Hashing Functions

All hashing within the context of this application will be done using SHA-256. Current NIST standards indicate that SHA-256 provides sufficient security for Digital signatures and hash-only applications. 

### Key Agreement

The key agreement algorithm used to address these threat models will be Diffie Hellman. Diffie Hellman exchanges allow securely exchanging cryptographic keys over a public channel. Security of this exchange is based on discrete logarithms. Current NIST standards indicate that 2048-bit groups with 224-bit keys provide sufficient security for modern applications. This application will utilize the prime and generator values for the 2048-bit MODP Group as specified by RFC3526, available at https://www.ietf.org/rfc/rfc3526.txt, and quoted below.

```
   This prime is: 2^2048 - 2^1984 - 1 + 2^64 * { [2^1918 pi] + 124476 }

   Its hexadecimal value is:

      FFFFFFFF FFFFFFFF C90FDAA2 2168C234 C4C6628B 80DC1CD1
      29024E08 8A67CC74 020BBEA6 3B139B22 514A0879 8E3404DD
      EF9519B3 CD3A431B 302B0A6D F25F1437 4FE1356D 6D51C245
      E485B576 625E7EC6 F44C42E9 A637ED6B 0BFF5CB6 F406B7ED
      EE386BFB 5A899FA5 AE9F2411 7C4B1FE6 49286651 ECE45B3D
      C2007CB8 A163BF05 98DA4836 1C55D39A 69163FA8 FD24CF5F
      83655D23 DCA3AD96 1C62F356 208552BB 9ED52907 7096966D
      670C354E 4ABC9804 F1746C08 CA18217C 32905E46 2E36CE3B
      E39E772C 180E8603 9B2783A2 EC07A28F B5C55DF0 6F4C52C9
      DE2BCBF6 95581718 3995497C EA956AE5 15D22618 98FA0510
      15728E5A 8AACAA68 FFFFFFFF FFFFFFFF

   The generator is: 2.
```

## Threats

### T1 - Unauthorized Token Issuance

#### Description

For this threat model, there is an assumption that clients are untrusted, and that illegitimate clients may try and request tokens from the group server.  Any illegitimate client who successfully obtains a token will undermine the security of the sever, and negate any worth of using tokens to access and modify groups and files.  A security breach via illegitimate token access from the group server would look similar to the following diagram: 

* Unathorized Client(C) requests a token from Group Server(GS) with the username of owner of group g
* **C** -> **GS**: `<token request>, group owner`
* The Group Server provides the group owner's token 
* **GS** -> **C**: `<token>`
* Client deletes file f from File Server(FS)
* **C** -> **FS**: `<deletes file f>, <token>`
* Client uploads malicious file m with the same name as the deleted file f
* **C** -> **FS**: `<upload file m>`
* the Unathorized Client has just uploaded malicious file m to the server, which could be downloaded by any user assuming it is file f 

#### Protection

Because there is an assumption that clients are not trustworthy, all clients (C) must be verified via a password before being issued a token.  When an administrator (A) first creates a user, the server (S) issues a one-time password of 8-16 randomized characters.  This password is communicated to the new user via the administrator, and upon their first successful entry to the server they are told to enter their new password.  They will then use this password for all subsequent attempts to enter the server. 

* Administrator creates a new user, is issued one-time password by server
* S -> A: `one-time password`
* Administrator communicates password to client
* A -> C: `one-time password (communicated in person)`
* Client attempts to request token without having set password, request is denied
* C -> S: `<requests token>`
* S -> C: `<request denied> (password not reset)`
* Client provides one-time password, password change requested
* C -> S: `changepassword, <one-time password>, <new password>`
* S -> C: `<accept password change request>`
* Normal client log in after password change
* C -> S: `<requests token>, password`
* S -> C: `<token>`

#### Argument

The suggested protocol gives a base level of protection against unauthorized clients attempting to access the file system illegitimately.  By having the group server create a random password and forcing the administrator to directly communicate it to the authorized client in person (It should also be noted that the communication between the administrator and the group server are secured as well, as noted in T4) there are few ways for an attacker to obtain the random password.  As for the authorized client's permanent password, as for all password based security systems, part of the responsibility lies on the client to create a password that cannot be easily guessed by an attacker.   

### T2 - Token Modification/Forgery

#### Description

Tokens issued by a trusted Group Server grant users access to groups and files. This threat model states that Users are not to be trusted, and will attempt to modify/forge tokens to increase their access rights. If a user is able to increase their access rights by modifying/forging a token, tokens become worthless as a method for defining access rights. To counter this threat, it must be possible for any third party (i.e. a File Server) to verify the integrity of all tokens received. In order to retain the functionality of distributed file servers that can spawn without notifying the trusted Group Server, this verification should be done without contacting said Group Server. The following is a diagram showing how token modification could be exploited to gain access to additional groups:

* **Bob (B)** -> **Group Server (GS)**: `<token request>, Bob`
* **GS** -> **B**: `<token>`
* **B** adds group *secret_group* to his token
* **B** -> **File Server (FS)**: `<requests file f in group secret_group>, <token>`
* **FS** -> **B**: `<file f's contents>`
* Bob now has access to all files in the group *secret_group*

#### Protection

The Group Server will have a public key and associated private key used only to generate RSA signatures. The key size will be 2048 bits. When a token is issued by the group server, it will be hashed (with SHA256) and signed using this key pair. Any time a token is communicated, the appropriate signature must be included. This signature will provide the ability for any third party to verify the integrity of the token using the Group Server's public key. The request/receipt of a token will be comprised of the following exchanges between Bob (B) and the Group Server (S):

* **B** -> **S**: ``<requests token for Bob>``
* **S** -> **B**: `[ token ] Ks^(-1)`
* Bob now has a token with integrity that can be easily verified by a third party who trusts S.

#### Argument

The suggested protocol allows any third party to verify the integrity of a token issued by a trusted Group Server. SHA256 will be used to generate the hash of the token, as it is a widely used hash algorithm that does a good job of providing both pre-image and second pre-image resistance. This hash will then be transformed into a signature using the private key of the trusted Group Server. The transformation of the hash into a signature relies on RSA public-key cryptography. RSA's medium-term security with large (2048 - 4096 bits) keys is not disputed by the cryptography community, and it is assumed to be relatively secure. RSA's security relies on the difficulty of the discrete logarithm problem, for which there is no known efficient general solution. Finally, this signature can be verified by anyone, as the Group Server's public key is publicly known information and can be used to decrypt the signature into a verifiable hash.

### T3

#### Description

The file server implementation must ensure that if a user attempts to contact some server, s, then they actually connect to s and not some other server. If there is no way to check that the file server you want to connect to is actually that server, then a malicious agent could try to pretend to be the server that you want to connect to. If a malicious file server is able to pretend to be a differet file server it can recieve the intended files from the user and glean information from it. The malicious server could also provide files on a download that could harm or be used to infiltrate the user's system. 

#### Protection

On the first connection from a user to a file server the file server will provide the user with its public key.  The user will save that locally. Then on any further communication with that file server the client will encrypt a large random number with that public key and send the encrypted message to the server. The server will then respond with the number that they decrypt with their private key. If the number matches the number that the client encrypted the client will know they have a secure connection.

* Bob Connects to file server 1 for the first time.
* **B** -> **S** ``<connects and requests token>``
* File server 1 sends bob its public key.
* **S** -> **B** ``<public key>``
* Bob connects with file server 1 sends it an encrypted message  of a random number with the public key.
* **B** -> **S** ``<publickey(R1)>``
* Server will send Bob R1 decrypted with its private key. If it's the right one then bob will be good to use the server.
* **S** -> **B** ``<R1>``

#### Argument

This protection will assure the user of the client that they are connecting to the file server they intend to connect to. They can be assured they are not uploading or downloading files from a different server by checking the file server's fingerprint. A malicious server will be unable to decrypt R1 and the client will disconnect and refuse to use a server without getting a new random number that it encrypts back. 


### T4 - Information Leakage via Passive Monitoring

#### Description

This threat model assumes the existence of passive attackers (e.g., nosy administrators). This means that all communications between client and server applications are visible to any third party observing the wire, and thus the contents of all traffic must be secured to protect confidentiality. Confidentiality ensures that file contents remain private, and that tokens/passwords cannot be stolen in transit. Without confidentiality, the following scenarios are possible:

##### Token Theft

* Adversary **A** is passively monitoring the network.
* **Bob** -> **Group Server** : `<requests a token>, Bob, password`
* **A** now knows that user **Bob** exists, and also knows how to authenticate as Bob.
* **A** can now impersonate **Bob** and request a token in his name.

##### File Theft

* Adversary **A** is passively monitoring the network.
* **Bob** -> **File Server** : `<requests a file>, <token>`
* **File Server** -> **Bob**: `<file contents>`
* **A** now knows that the file exists, knows how to authenticate as Bob, and also knows the contents of the transmitted file.

#### Protection

To protect against this threat model, we will utilize the Diffie Hellman key exchange during all communications. Every time a client and server interact, their interaction will be prefaced by a Diffie Hellman key exchange. This allows the client and server to agree on a new shared secret key before every interaction, and grants perfect forward secrecy. After the key exchange, all messages will be encrypted using AES with the symmetric, shared key. During application development, values g and q will be chosen and baked into the applications. Value q will be 2048 bits, and values a and b will be 224 bits. Here is the sequence of messages we will use during the key exchange between our two actors, Bob (B) and Server (S):

* Bob picks random value a.
* **B** -> **S**: `(g^a) mod q`
* Server picks random value b.
* **S** -> **B**: `(g^b) mod q`
* Bob and Server now have a shared key `K= g^(a*b) mod q`
* **B** -> **S**: `{<message>}K`
* **S** -> **B**: `{<message>}K`


#### Argument

The suggested protocol specifies an implementation of the Diffie-Hellman key exchange protocol. Diffie-Hellman is a well-known method for securely agreeing on a cryptographic key over a public channel. Diffie-Hellman's security properties rely on the difficulty of solving the discrete logarithm problem, which has no known efficient general solution. After a shared key is agreed upon, communications will be secured by encrypting message contents with AES and the shared key.

## Conclusion

Describe mechanism interplay, design process, etc
