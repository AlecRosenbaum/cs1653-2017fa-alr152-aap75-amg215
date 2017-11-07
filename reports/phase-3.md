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

The Symmetric Key Cryptography algorithm used in addressing these threat models is AES-128. AES-128 utilizes a 128-bit key, and is considered sufficiently secure according to NIST standards published in 2016[1]. 

### Public Key Cryptography

All public key cryptography used in addressing these threat models is implemented using RSA. Current NIST standards indicate that 2048-bit groups with 224-bit keys provide sufficient security for algorithms based on discrete logarithms. RSA will be used for encryption and decryption of arbitrary data, as well as the creation and verification of digital signatures.

### Hashing Functions

All hashing within the context of this application will be done using SHA-256. Current NIST standards indicate that SHA-256 provides sufficient security for Digital signatures and hash-only applications. In the case of storing user passwords, PBKDF2 will be used to create the password key, while SHA-256 will be used as the pseudorandom function.  For our use, PBKDF2 will apply the HMAC SHA-256 encryption multiple times, and then proceed to perform a xor operation over the multiple iterations of the pseudorandom function. This process is known as key stretching, and lengthens the time it takes to crack a password. As of January 2017, the Internet Engineering Task Force (IETF) recommends PBKDF2 as a password hashing algorithm [2].  The key derivation formula for PBKDF2 is as follows [2]:

```
   DK = (P, S, c, dkLen)
   Options:        PRF        underlying pseudorandom function (hLen
                              denotes the length in octets of the
                              pseudorandom function output)

   Input:          P          password, an octet string
                   S          salt, an octet string
                   c          iteration count, a positive integer
                   dkLen      intended length in octets of the derived
                              key, a positive integer, at most
                              (2^32 - 1) * hLen

   Output:         DK         derived key, a dkLen-octet string

   Steps:

      1.  If dkLen > (2^32 - 1) * hLen, output "derived key too long"
          and stop.

      2.  Let l be the number of hLen-octet blocks in the derived key,
          rounding up, and let r be the number of octets in the last
          block:

                   l = CEIL (dkLen / hLen)
                   r = dkLen - (l - 1) * hLen

          Here, CEIL (x) is the "ceiling" function, i.e., the smallest
          integer greater than, or equal to, x.

      3.  For each block of the derived key apply the function F defined
          below to the password P, the salt S, the iteration count c,
          and the block index to compute the block:

                   T_1 = F (P, S, c, 1) ,
                   T_2 = F (P, S, c, 2) ,
                   ...
                   T_l = F (P, S, c, l) ,

          where the function F is defined as the exclusive-or sum of the
          first c iterates of the underlying pseudorandom function PRF
          applied to the password P and the concatenation of the salt S
          and the block index i:

                   F (P, S, c, i) = U_1 \xor U_2 \xor ... \xor U_c

          where
                   U_1 = PRF (P, S || INT (i)) ,
                   U_2 = PRF (P, U_1) ,
                   ...
                   U_c = PRF (P, U_{c-1}) .

          Here, INT (i) is a four-octet encoding of the integer i, most
          significant octet first.

      4.  Concatenate the blocks and extract the first dkLen octets to
          produce a derived key DK:

                   DK = T_1 || T_2 ||  ...  || T_l<0..r-1>

      5.  Output the derived key DK.
```

For example, the Wi-Fi Protected Alliance II (WPA2) uses the following parameters
```
DK = PBKDF2(HMAC−SHA1, passphrase, ssid, 4096, 256)
```

### Key Agreement

The key agreement algorithm used to address these threat models will be Diffie Hellman. Diffie Hellman exchanges allow securely exchanging cryptographic keys over a public channel. Security of this exchange is based on discrete logarithms. Current NIST standards indicate that 2048-bit groups with 224-bit keys provide sufficient security for modern applications. This application will utilize the prime and generator values for the 2048-bit MODP Group as specified by RFC3526[3].

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



For this threat model, there is an assumption that clients are untrusted, and that illegitimate clients may try and request tokens from the group server.  Any illegitimate client who successfully obtains a token will undermine the security of the sever, and negate any worth of using tokens to access and modify groups and files.  A security breach via illegitimate token access from the group server would look similar to the following diagram: 

* Unauthorized Client(C) requests a token from Group Server(GS) with the username of owner of group g
* **C** -> **GS**: `<token request>, group owner`
* The Group Server provides the group owner's token 
* **GS** -> **C**: `<token>`
* Client deletes file f from File Server(FS)
* **C** -> **FS**: `<deletes file f>, <token>`
* Client uploads malicious file m with the same name as the deleted file f
* **C** -> **FS**: `<upload file m>`
* the Unauthorized Client has just uploaded malicious file m to the server, which could be downloaded by any user assuming it is file f 

#### Protection

Because there is an assumption that clients are not trustworthy, all clients (C) must be verified via a password before being issued a token.  When an administrator (A) first creates a user, the server (S) issues a one-time password of 8 randomized characters. The one-time password is hashed and stored using PBKDF2 and a randomized salt. This password is communicated to the new user via the administrator in person, and upon their first successful entry to the server they are told to enter their new password.  They will then use this password for all subsequent attempts to enter the server. At this point a new derived key is created consisting of the new password and previous salt, still using PBKDF2.  This will be stored on the server and will serve as the basis for verifying a user’s password.  Finally, upon five failed attempts to login, a user's account is then locked.  At this point, an administrator will have to unlock the account and the process of creating a password is repeated.  It should also be noted that all communications are secured using a signed Diffie Hellman exchanged as implemented in T3 and T4.

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
* Normal client logs in after password change
* C -> S: `<requests token>, password`
* S -> C: `<token>`

#### Argument

The suggested protocol gives a secure level of protection against unauthorized clients attempting to access the file system illegitimately.  The suggested protocol protects against multiple different attacks that an adversary could perform on the system.  If an adversary attempts to perform an online dictionary attack against the system, the profile they are trying to access will lock after several failed attempts to guess it.  The salted nature of the derived key also protects against a rainbow table attack, as the salt will force the hash dictionary to be recomputed for every password.  The use of PBKDF2 also provides benefits against a brute force attack. This is because, in our case, PBKDF2 arbitrarily increases the time it takes to hash a password, thus increasing bruteforce time.

### T2 - Token Modification/Forgery

#### Description

Tokens issued by a trusted Group Server grants users access to groups and files. This threat model states that Users are not to be trusted, and will attempt to modify/forge tokens to increase their access rights. If a user is able to increase their access rights by modifying/forging a token, tokens become worthless as a method for defining access rights. To counter this threat, it must be possible for any third party (i.e. a File Server) to verify the integrity of all tokens received. In order to retain the functionality of distributed file servers that can spawn without notifying the trusted Group Server, this verification should be done without contacting said Group Server. The following is a diagram showing how token modification could be exploited to gain access to additional groups:

* **Bob** logs in to the **Group Server (GS)** and retrieves a token
* **GS** -> **B**: `<token>`
* **B** adds group *secret_group* to his token
* **B** -> **File Server (FS)**: `<requests file f in group secret_group>, <token>`
* **FS** -> **B**: `<file f's contents>`
* Bob now has access to all files in the group *secret_group*

#### Protection

The Group Server will have a public key and associated private key used only to generate RSA signatures. When a token is issued by the group server, the identity and list of groups will be extracted, hashed, and signed using this key pair. Any time a token is communicated, the appropriate signature must be included. This signature will provide the ability for any third party to verify the integrity of the token using the Group Server's public key. The request/receipt of a token will be comprised of the following exchanges between Bob (B) and the Group Server (GS):

* **Bob** logs in to the **Group Server (GS)** and requests a token for **Bob** following the protocol specified in T1, using the key agreement protocol specified in T4
* **GS** -> **B**: `token, [ H(token-data) ] Ks^(-1)`
* Bob now has a token with integrity that can be easily verified by a third party who trusts S.

Note: tokens will be serialized into token-data as follows:

token -> `<issuer>,<username>;<group_1>,<group_2>,...,<group_n>`

In the case of issuer **GroupServer** and user **Bob** with access to groups `bobs_group`, `alices_group`, and `fun_group`, **Bob's** token would be serialized into the following string: `GroupServer,Bob;alices_group,bobs_group,fun_group` 

It is also important to note that:
* commas are disallowed in the names of groups
* groups are serialized in alphabetical order

#### Argument

The suggested protocol allows any third party to verify the integrity of a token issued by a trusted Group Server. When a token is issued, the token's data is serialized and hashed. This hash will then be transformed into a signature using the private key of the trusted Group Server. Finally, this signature can be verified by anyone, as the Group Server's public key is publicly known information and can be used to decrypt the signature into a verifiable hash.

If Bob modifies his token after receipt, the computed hash of that token's data (done by the file server) will not match the hash signed by the group server. If Bob forges a new token, there will be no signed hash associated with that token. If no signed hash is provided by to the file server along with the token, the file server will reject the request, as its integrity cannot be verified.

Furthermore, functionally equivalent tokens will always generate the serialized output while functionally different tokens will serialize differently. This property is ensured by a) including all data in the serialization output, b) sorting the serialized groups alphabetically, and c) disallowing use of the serialization delimiter in group names.

### T3 - Unauthorized file servers

#### Description

The file server implementation must ensure that if a user attempts to contact some server, s, then they actually connect to s and not some other server. If there is no way to check that the file server you want to connect to is actually that server, then a malicious agent could try to pretend to be the server that you want to connect to. If a malicious file server is able to pretend to be a different file server it can receive the intended files from the user and glean information from it. The malicious server could also provide files on a download that could harm or be used to infiltrate the user's system. The following is an example of how a malicious user M could exploit a system without a protection for this threat.

* **B** -> **A**  **S** ``<connects to file server requests file, A intercepts>``
* **A** -> **B** ``<provides fraudulent files>``

#### Protection

This threat will be protected through the use of a signed Diffie Hellman key exchange. The only thing that differs to ensure authorized file servers is that on initial connection the signature of the file server the user is connecting to must be approved by the user. All future connections Diffe Hellmen key exhanges will be initialted with the server providing a signed message with the initially provide public key. 

* Bob makes an initial connection to a file server **S**
* **B** -> **S** ``<Initial connection>``
* **S** -> **B** ``<public key>``
* Bob must then approve this public key via a prompt and that key will then be used when using signed Diffie Hellman exchanges in the future.
* Bob and **S** proceed with signed Diffie Hellman exchange and symmetric key cryptographic communication can continue. 
* Bob picks random value a.
* **B** -> **S**: `(g^a) mod q`
* Server picks random value b.
* **S** -> **B**: `[(g^b) mod q]S^(-1)`
* Bob validates signature with the intially provided public key and Bob and Server now have a shared key `K= g^(a*b) mod q`
* **B** -> **S**: `{<message>}K`
* **S** -> **B**: `{<message>}K`

#### Argument

This protection will assure the user of the client that they are connecting to the file server they intend to connect to. The key to the protection is that Bob has to approve the public key of the file server initially. After that it is impossible for an attacker to provide the correct signature in the Diffie Hellman exchange to convince Bob to create a shared key with him and communicate with the attacker.


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

To protect against this threat model, we will utilize a signed Diffie Hellman key exchange during all communications. Every time a client and server interact, their interaction will be prefaced by a signed Diffie Hellman key exchange. This allows the client and server to agree on a new shared secret key before every interaction, and grants perfect forward secrecy. This method also enables the user and server to ensure that they are each communicating with the entity they intend to be communicating with by verifying the signatures. After the key exchange, all messages will be encrypted using AES with the symmetric, shared key. As stated within the assumptions section, values g and q will follow the recommendations laid out in RFC3526 for 2048-bit exchanges. The implementation and messages sent are described in T3.

#### Argument

The suggested protocol specifies an implementation of the Diffie-Hellman key exchange protocol. Diffie-Hellman is a well-known method for securely agreeing on a cryptographic key over a public channel. Diffie-Hellman's security properties rely on the difficulty of solving the discrete logarithm problem, which has no known efficient general solution. After a shared key is agreed upon, communications will be secured by encrypting message contents with AES and the shared key.

## Conclusion

In this document it is outlined how protocols can be implemented to protect against passive listening adversaries, and actively adversarial clients. The described protocols secure communications, provide File server authentication, allow token verification, and prevent tokens from being issued to unauthorized clients.


