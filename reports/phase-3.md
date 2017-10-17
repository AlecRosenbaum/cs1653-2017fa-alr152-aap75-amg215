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

## Threats to Protect Against

### T1 - Unauthorized Token Issuance

#### Description

Due to the fact that clients are untrusted, the group server must be protected against illegitimate clients requesting tokens from it, and ensure all clients are authenticated in a secure manner prior to issuing them tokens.

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

The suggested protocol allows any third party to verify the integrity of a token issued by a trusted Group Server. SHA256 will be used to generate the hash of the token, as it is a widely used hash algorithm that does a good job of providing both pre-image and second pre-image resistance. This hash will then be transformed into a signature using the private key of the trusted Group Server. The transformation of the hash into a signature relies on RSA public-key cryptography. RSA's medium-term security with large (2048 - 4096 bits) is not disputed by the cryptography community, and it is assumed to be relatively secure. RSA's security relies on the difficulty of the discrete logarithm problem, for which there is no known efficient general solution. Finally, this signature can be verified by anyone, as the Group Server's public key is publicly known information and can be used to decrypt the signature into a verifiable hash.

### T3

#### Description

The file server implementation must ensure that if a user attempts to contact
some server, s, then they actually connect to s and not some other server.

#### Protection

We will mirror ssh's implementation to solve this issue. On the first connection from a user to a file server the file server will provide the user with a hash of its public key called a fingerprint. This hash will be a SHA256 hash that should be secure for the foreseeable future. The user will save that locally. Then on any further communication with that file server the file server will provide the user with that fingerprint. If it doesn't match to the fingerprint the user expects the user will be alerted and disconnected from the file server.

* Bob Connects to file server 1 for the first time.
* B -> S ``<connects and requests token>``
* File server 1 sends bob its fingerprint.
* S -> B ``SHA256(key)``
* Bob connects with file server 1 and requests its fingerprint.
* B -> S ``<fingerprint request>``
* Server will send Bob a fingerprint. If it's the right one then bob will be good to use the server.
* S -> B ``<fingerprint>``

### T4 - Information Leakage via Passive Monitoring

#### Description

This threat model assumes the the existence of passive attackers (e.g., nosy administrators). This means that all communications between client and server applications are visible to any third party observing the wire, and thus the contents of all traffic must be secured to protect confidentiality. Confidentiality ensures that file contents remain private, and that tokens/passwords cannot be stolen in transit. Without confidentiality, the following scenarios are possible:

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

The suggested protocol specifies an implementation of the Diffie-Hellman key exchange protocol. Diffie-Hellman is a well-known method for securely agreeing on a cryptographic key over a public channel. Diffie-Hellman's security properties rely on the the difficulty of solving the discrete logarithm problem, which has no known efficient general solution. After a shared key is agreed upon, communications will be secured by encrypting message contents with AES and the shared key.
