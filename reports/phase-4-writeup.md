# Phase 4 Writeup

## Group Information

* Alec Rosenbaum
    - alr152@pitt.edu
    - alecrosenbaum
* Aadu Pirn
    - aap75@pitt.edu
    - aadupirn


## Cryptographic Mechanisms and Protocols

### Symmetric Key Cryptography

The Symmetric Key Cryptography algorithm used in addressing these threat models is AES-128. AES-128 utilizes a 128-bit key, and is considered sufficiently secure according to NIST standards published in 2016[1]. 

### Public Key Cryptography

All public key cryptography used in addressing these threat models is implemented using RSA. Current NIST standards indicate that 2048-bit groups with 224-bit keys provide sufficient security for algorithms based on discrete logarithms. RSA will be used for encryption and decryption of arbitrary data, as well as the creation and verification of digital signatures.

### Hashing Functions

All hashing within the context of this application will be done using SHA-256. Current NIST standards indicate that SHA-256 provides sufficient security for Digital signatures and hash-only applications. PBKDF2 will be used for storing user passwords, using SHA-256 as the pseudorandom function. As of January 2017, the Internet Engineering Task Force (IETF) recommends PBKDF2 as a password hashing algorithm [2].

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

### Fingerprint

Within the context of this document, the term "fingerprint" shall refer to the SHA256 hash of a known public key. The purpose of a fingerprint is compress a public key into a consistent size that can easily be verified.

## Threats

### T5 - Message Reorder, Replay, or Modification

#### Description

When a client communicates with a file or group server this threat states that messages sent between the user (**U**) and the server (**S**) can be reordered, saved for a replay attack, or modified by a malicious attacker. The following examples show how a malicious attacker (**A**) could intercept a message and reuse it to gain access to an unprotected server.

* The attacker intercepts a username and password meant for a server and replays it to gain access to protected data.
* **U** -> **A**: `<username, password>` 
* **A** -> **S**: `<username, password>`
* **S** -> **A**: `<authenticated token>`
* Once an attacker has an authenticated token it can use it to make attacks on the server.

* The attacker intercepts a message and modifies it to change the original intended message.
* **U** -> **A**: `<username, password, delete file B>` 
* **A** -> **S**: `<username, password, delete file A>`
* Even if the attacker is able to just change parts of the message it can make unintended changes like the deletion of the wrong file.


#### Protection

Threat 4 from phase three was dealt with using a Diffie Hellman exchange. This exchange is signed in the case of a file server. To protect against this threat we will use a signed Diffie Hellman exchange for all communication between a both file and group servers and a Client. The following is how this exchange works.

* Bob picks random value a.
* **B** -> **S**: `(g^a) mod q`
* Server picks random value b and signs the message.
* **S** -> **B**: `[(g^b) mod q]S^(-1)`
* Bob validates signature and Bob and Server now have a shared key `K= g^(a*b) mod q`
* **B** -> **S**: `{<message>}K`
* **S** -> **B**: `{<message>}K`

#### Argument

This signed Diffie Hellman exchange protects from reorder, replay and modification attacks. If an attacker intercepts either of the starting messages that sets up the Diffie Hellman key the attacker will still be unable to learn the Key. The attacker will have access to neither b or a and no way to build `g^(a*b) mod q` which is the key. Trying to replay messages will not give the attacker access to the key because, again, they will not have access to `g^(a*b) mod q`. Changing the order will result in the same. The strength of the AES-128 encryption used once a key is agreed upon prevents an attacker from modifying or learning anything from the message. 

### T6 - File Leakage

#### Description

This threat states that file servers cannot be trusted. Files stored on file servers will be able to be attained by malicious attackers. This means that the data on the file servers is not safe from attackers as, if left unprotected, the files can be read by the attacker.

#### Protection

Each file server (**S**) will create it's own AES-128 key. It will use this key to encrypt any files sent to it for storage. The encrypted files will then be stored on disk with the file names being the SHA-256 hash of the actual filename. If, later on, a user (**U**) requests these files with an approved token it will then decrypt the file and send it to the user. The following is a diagram of that exchange with DHK being the Diffie Hellman key explained in T5, and K being the File server's storage key.

* **U** -> **S** `<{<file>}DHK>`
* **S** saves `{<file>}K` to disk using the filename `H(filename)`
* **U** -> **S** `<download request for file>`
* **S** s finds the file with the name `H(filename)` and decrypts with K.
* **S** -> **U** `<{<file>}DHK>`

#### Argument

This protection protects attackers from reading files from an unsafe file server as the attackers do not have access to the AES-128 key. Attackers will also not be able to learn the names of the files stored on the machine as the SHA-256 hash of the actual filename. SHA-256 has preimage resistance so this will be impossible to reverse. The only thing attackers would be able to gain access to is the encrypted file. This also continues to work after group memberships change. If a user loses a group membership needed to view a file he/she will be unable to provide the token that they need to provide to the file server to gain access to a file.

### T7 - Token Theft

#### Description

When a user makes a request to a file server, a token is provided with this request. This threat model suggests that a malicious file server may save a copy of that token for later use. The following example of communication between Bob (**B**), a malicious file server (**MS**) shows how a malicious file server may steal a token for later use by malicious client (**MC**) on another file server (**FS**):

* **B** -> **MS**: `<file request>, [<B's token>]Kgs^-1`
* **MS** -> **B**: `<file f's contents>`
* **MS** stores the signed token
* Later, **MS** gives the stored token `[<B's token>]Kgs^-1` to **MC**
* **MC** connects to **FS**, and makes requests with **B**'s token.
* **MC** -> **FS** `<file request>, [<B's token>]Kgs^-1`
* **FS** -> **MC**: `<file f's contents>`

#### Protection

To protect against this threat model, each token should be valid only for one file server. This will be implemented by requiring clients to specify their desired file server when making a request for a new token. The issued token will then include the fingerprint of the file server specified in the token request. The fingerprint will then be verified before the file server fulfills each request. Here is the modified token request protocol:

* **B** -> **GS**: `<requests token>, password, <fileserver_fingerprint>`
* **GS** -> **B**: `<token>, [ H(token-data) ] Kgs^(-1)`

Additionally, token serialization will be modified as follows:

token -> `<issuer>,<username>,<fileserver_fingerprint>;<group_1>,<group_2>,...,<group_n>`

In the case of issuer **GroupServer**, file server **FileServer** and user **Bob** with access to groups `bobs_group`, `alices_group`, and `fun_group`, **Bob's** token would be serialized into the following string:
`GroupServer,Bob,<FileServer's fingerprint>;alices_group,bobs_group,fun_group` 

It is also important to note that:
* commas are disallowed in the names of groups and usernames
* groups are serialized in alphabetical order

Then, when requests are made to file servers, the file server shall verify that that the fingerprint matches their own.

#### Argument

With the implementation of the above protocol, malicious file servers are still able to steal tokens from users. However, these tokens will not be valid on any other file server. If a token is stolen by a malicious file server and provided to a malicious client for use with another file server, that file server will reject the token and terminate the connection with said client.

## T1-T4

### T1

The mechanism preventing unauthorized token issuance is still in place. A password is required to retrieve a token for each user. This has not changed from phase 3.

### T2

The mechanism preventing token modification and forgery is largely unchanged. The primary change with respect to token signing is that tokens are only usable on a single file server. So, that file server's fingerprint is now included in the serialized token, which is then hashed and signed the same way as before. This inclusion does not invalidate the previous method for verification.

### T3

Nothing addressing the above threats changes our solution T3. T3 primarily involved exchanging a file server's public key, then prompting a user if they trust it. Clients then only connect to trusted file servers. The file server's key is exchanged and validated the same way as during phase 3.

### T4

Again, the way we addressed Passive Monitoring will not change for phase 4. We will still be performing a partially-signed Diffie-Hellman exchange before every communication. This prevents passive monitoring, because all messages are encrypted with a session-specific secret key.

## Conclusion

The methods of protection outlined above add to the protocols defined for protection against T1-T4, and add additional protection against active attackers and compromised file servers.