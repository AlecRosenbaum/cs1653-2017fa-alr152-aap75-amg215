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

### T1

#### Description

#### Protection


### T2 - Token Modification/Forgery

#### Description

Users are expected to attempt to modify their tokens to increase their access rights, and to attempt to create forged tokens. Your implementation of the UserToken interface must be extended to allow file servers (or anyone else) to determine whether a token is in fact valid. Specifically, it must be possible for a third-party to verify that a token was in fact issued by a trusted group server and was not modified after issuance.

#### Protection

The Group Server will have a public key and associated private key used only for signing. When a token is issued by the group server, it will be signed using this key pair. Any time a token is communicated, the appropriate signature must be included. This signature will provide the ability for any third party to verify the integrity of the token using the Group Server's public key. The request/receipt of a token will be comprised of the following exchanges between Bob (B) and the Group Server (S):

* B -> S: ``<requests token for Bob>``
* S -> B: `[ token ] Ks^(-1)`
* Bob now has a token with integrity that can be easily verified by a third party who trusts S.


### T3

#### Description

#### Protection


### T4 - Information Leakage via Passive Monitoring

#### Description

Since our trust model assumes the existence of passive attackers (e.g., nosy administrators), you must ensure that all communications between your client and server applications are hidden from outside observers. This will ensure that file contents remain private, and that tokens cannot be stolen in transit.

#### Protection

To protect against this threat model, we will utilize the Diffie Hellman key exchange during all communications. Every time a client and server interact, their interaction will be prefaced by a Diffie Hellman key exchange. This allows the client and server to agree on a new shared secret key before every interaction, and grants perfect forward secrecy. During application development, values g and q will be chosen and baked into the applications. Here is the sequence of messages we will use during the key exchange between our two actors, Bob (B) and Server (S):

* Bob picks random value a.
* B -> S: `(g^a) mod q`
* Server picks random value b.
* S -> B: `(g^b) mod q`
* Bob and Server now have a shared key `K= g^(a*b) mod q`