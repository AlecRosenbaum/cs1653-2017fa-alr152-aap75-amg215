# Phase 3 Preliminary Report

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

Users are expected to attempt to modify their tokens to increase their access rights, and to attempt to create forged tokens. It must be possible for a third-party to verify that a token was in fact issued by a trusted group server and was not modified after issuance.

#### Protection

The Group Server will have a public key and associated private key used only to generate RSA signatures. The key size will be 2048 bits. When a token is issued by the group server, it will be signed using this key pair. Any time a token is communicated, the appropriate signature must be included. This signature will provide the ability for any third party to verify the integrity of the token using the Group Server's public key. The request/receipt of a token will be comprised of the following exchanges between Bob (B) and the Group Server (S):

* B -> S: ``<requests token for Bob>``
* S -> B: `[ token ] Ks^(-1)`
* Bob now has a token with integrity that can be easily verified by a third party who trusts S.


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

<!-- Begin by describing the threat treated in this section. This may include describing examples of the threat being exploited by an adversary, a short discussion of why this threat is problematic and needs to be addressed, and/or diagrams showing how the threat might manifest in your group’s current (insecure) implementation -->

This threat model assumes the the existence of passive attackers (e.g., nosy administrators). This means that all communications between client and server applications are visible to any third party observing the wire, and thus the contents of all traffic must be secured to protect confidentiality. Confidentiality ensures that file contents remain private, and that tokens/passwords cannot be stolen in transit. Without confidentiality, the following scenario is possible:

* Adversary *A* is passively monitoring the network.
* *Bob* -> *Group Server* : `<requests a token>, Bob, password`
* *A* now knows that user *Bob* exists, and also knows what Bob's password is.
* *A* can now impersonate *Bob* and request a token in his name.

#### Protection

To protect against this threat model, we will utilize the Diffie Hellman key exchange during all communications. Every time a client and server interact, their interaction will be prefaced by a Diffie Hellman key exchange. This allows the client and server to agree on a new shared secret key before every interaction, and grants perfect forward secrecy. After the key exchange, all messages will be encrypted using AES with the symmetric, shared key. During application development, values g and q will be chosen and baked into the applications. Value q will be 2048 bits, and values a and b will be 224 bits. Here is the sequence of messages we will use during the key exchange between our two actors, Bob (B) and Server (S):

* Bob picks random value a.
* *B* -> *S*: `(g^a) mod q`
* Server picks random value b.
* *S* -> *B*: `(g^b) mod q`
* Bob and Server now have a shared key `K= g^(a*b) mod q`
* *B* -> *S*: `{<message>}K`
* *S* -> *B*: `{<message>}K`


#### Argument

The suggested protocol specifies an implementation of the Diffie-Hellman key exchange protocol. Diffie-Hellman is a well-known method for securely agreeing on a cryptographic key over a public channel. Diffie-Hellman's security properties rely on the the difficulty of solving the discrete logarithm problem, which has no known efficient general solution. After a shared key is agreed upon, communications will be secured by encrypting message contents with AES and the shared key.
