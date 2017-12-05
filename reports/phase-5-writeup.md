# Phase 5 Writeup

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

SHA-256 will also be used for HMAC operations. NIST standards indicate that SHA-256 based HMAC's are currently sufficiently. secure 

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

### T8 - Unauthorized group servers

#### Description

The group server implementation must ensure that if a user attempts to contact some server, s, then they actually connect to s and not some other server. If there is no way to check that the group server you want to connect to is actually that server, then a malicious agent could try to pretend to be the server that you want to connect to. If a malicious server is able to pretend to be a an authorized group server, it can convince users to provide them with their credentials. The malicious server could then use those credentials to impersonate that user in a system that user has controlled access to. The following is an example of how a malicious user M could exploit a system without a protection for this threat.

* **B** -> **M**  **S** `<connects to group server, begins DH exchange, M intercepts>`
* **M** -> **B** `<completes DH exchange>`
* **B** -> **M** `<requests token, provides password>`
* **M** now has user **B**'s secret credentials


#### Protection

This threat will be protected through the use of a signed Diffie Hellman key exchange. All connections using Diffie Hellman key exchanges will be initiated with the server by providing a signed message with an public key provided when the server is first connected to. This public key is approved through a prompt to the user. The prompt provides the fingerprint of the public key, which is the SHA-256 hash of the public key presented in hex. If and only if the public key is approved, will the signed Diffie Hellman exchange proceed.

* Bob makes an initial connection to a group server **S**
* **B** -> **S** ``<Initial connection>``
* **S** -> **B** ``<public key>``
* Bob must then approve this public key via a prompt displaying its fingerprint. If approved, that key will then be used to verify future signed Diffie Hellman exchanges.
* Bob and **S** proceed with signed Diffie Hellman exchange and symmetric key cryptographic communication can continue. 
* Bob picks random value a.
* **B** -> **S**: `(g^a) mod q`
* Server picks random value b and signs the message.
* **S** -> **B**: `[(g^b) mod q]S^(-1)`
* Bob validates signature with the approved public key, and Bob and Server now have a shared key `K= g^(a*b) mod q`
* **B** -> **S**: `{<message>}K`
* **S** -> **B**: `{<message>}K`

#### Argument

This protection will assure the user of the client that they are connecting to the group server they intend to connect to. The key to the protection is that Bob has to approve the public key of the group server initially. After that, all the signed Diffie Hellman exchange can be verified using the approved public key. An attacker is unable to provide the correct signature in the Diffie Hellman exchange, and thus is unable to convince Bob that they are the group server.


### T10 - File Integrity

#### Description

The users must be able to verify the integrity of files retrieved from file servers. If there is no way to validate integrity, rogue file servers may modify uploaded files as they see fit with no way for an unsuspecting client to know that the file has been modified. The following shows an example of how a malicious file server (**FS**) may exploit a vulnerability in this threat model with users Bob (**B**) and Alice (**A**) and a trusted group server (**GS**):

* **B** -> **GS** `<request new key for group g>, token`
* **GS** -> **B** `Kf, {Kf}Kg`
* **B** encrypts file f using Kf, creates metadata = {Kf}Kg 
* **B** -> **FS** `<upload file>, metadata, file {f}, token`
* **FS** modifies stored file
* **A** -> **FS** `<request file f>, token`
* **FS** -> **A** `metadata, file`

Alice has now downloaded and decrypted a file that differs from the file that Bob uploaded. Alice has no way of verifying integrity on downloaded files.

#### Protection

This threat will be protected against by adding additional information into the metadata. Now, the metadata will include an HMAC of the file, calculated with a different key than used for signing. The new exchange and upload routines are shown below:

* **B** -> **GS** `<request new key for group g>, token`
* **GS** -> **B** `Kf, Ki, {Kf, Ki}Kg, `
* **B** encrypts file f using `Kf`, creates `metadata = {Kf, Ki}Kg, HMAC(Ki, {f}Kf)`
* **B** -> **FS** `<upload file>, metadata, file {f}Kf, token`
* **FS** modifies stored file
* **A** -> **FS** `<request file f>, token`
* **FS** -> **A** `metadata, file`
* **A** -> **GS** `<request to decrypt keys>, {Kf, Ki}Kg`
* **GS** -> **A** `Kf, Ki`
* Alice now verifies integrity using HMAC with Ki and {f}Kf, then decrypts {f}Kf using Kf

#### Argument

The protection will ensure integrity on stored files. If a malicious file server modifies a stored file, the HMAC stored in the metadata will no longer match. Users will detect file modification after the file is downloaded by verifying their calculated HMAC matches the HMAC stored in the metadata. The malicious file server has no way of obtaining Ki, and thus is not able to regenerate a valid HMAC after modification.

### Previous Threats

The addition of these protections do not invalidate T1-T7.

The only protocol modified is the prefix to the diffie helman exchange, which only adds the step of initially sending the public key from the group server to the client. This does not invalidate any of the protections offered by the diffie hellman exchange.


## Conclusion

In this document it is outlined how protocols can be implemented to protect against an active adversary impersonating the group server. The described protocols provide group server authentication. Implementing these protocols will protect a file sharing system from the specified threats while maintaining protections against previously specified threats.

