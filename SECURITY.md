# Security Policy

Gesto stores the identity of each paired PC and the token the PC gave to the phone. Treat bug reports about pairing, the encrypted channel, host key pinning, token storage or command authorization as security issues.

The PC side, [Gesto for Linux](https://github.com/Frumorn12/gesto-linux), has its own policy for problems in the daemon.

## Supported versions

Gesto is pre-1.0. Security fixes go into the latest release.

## Reporting a vulnerability

Open a [private security advisory](https://github.com/Frumorn12/gesto/security/advisories/new) on GitHub, or contact the repository owner directly. Please include:

- The app version (Settings › About) or commit.
- Android version and device model.
- The Gesto for Linux version on the PC.
- Which part is affected: discovery, pairing, the encrypted channel, host pinning, token storage or command authorization.
- Steps to reproduce.

## How the app protects you

- Discovery on the network is never trusted by itself: it only finds addresses.
- Pairing needs someone at the PC, who either presses Allow on the notification after checking the PIN, or runs `gesto pair` for a one-time code.
- The control channel uses P-256 key exchange (ECDH), a handshake signed by the PC's host key, HKDF and AES-GCM frames. The app pins the host key at pairing and refuses a PC whose key changed.
- Paired PCs, tokens included, are encrypted with an AES-GCM key kept in the Android Keystore, and excluded from Android backups.
- Commands go only through the encrypted channel.
- There is no account, no cloud backend and no analytics.

## Known limits

- The screen and sound stream is protected by a one-time token but is not encrypted like the control channel. Use it on a network you trust, or through a VPN.
- Someone with access to an unlocked, paired phone can control the PC. Revoke a lost phone on the PC with `gesto devices revoke <id>`.
- The protocol has not had an external security review yet.
