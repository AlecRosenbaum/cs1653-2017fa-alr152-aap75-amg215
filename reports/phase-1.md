# Phase 1 Report

## Group Information

* Alec Rosenbaum
    - alr152@pitt.edu
    - alecr95

## Security Properties

* **Authentication:** Authentication states that users self-identifying as user *u* should be validated as being user *u*. Without ensuring users are who they say they are, users can identify as other users, thus basically nullifying any permission-scheme setup.
* **Authorization:** Authorization states that user *u*'s permissions should be checked before performing any action. Without this requirement, users will be able to perform any action they request, and thus violate their allowed permissions.
* **File Authorization:** File Authorization states that file *f* belonging to group *g* may only be overwritten, downloaded, or deleted by users belonging to group *g*. Without this, any user could act on any file, directly going against the notion of group-based file sharing.
* **Least Permission:** Least Permission states that user *u* may only create new users with fewer permissions than *u* has at the time of creation. This requirement prevent users from effectively elevating their permissions by creating new accounts.
* **Group Permissions:** Group Deletion states that user *u* must be a member of group *g* in order to see the existence of *g*, add or remove members from *g*, or in order to delete *g* entirely. This requirement prevents users from acting on groups they do not belong to.
* **Secure Transmission** Secure Transimission states that any communication between two parties in the system shall be done such that all traffic is encrypted using modern, secure algorithms. This requirement prevents data from being comprimised at the time of transmission by a third-party observer.
* 



## Threat Models

### Threat Model 1

#### System Model

Lorem ipsum...

#### Assumtion

Lorem ipsum...

#### Security Properties

* example property
    - how it applies to this system

## References