# POS bill crypto

Electricity bill lookup and bill payment seal the payload on the phone and open it only where the matching private key lives. The mock build does that inside `LocalBillPay`. A live build posts the ciphertext; the host opens it. The AES key is never written to the request.

The scheme is **ECDH on P-256**, a **SHA-256** key derivation, and **AES-256-GCM**. Code: `PayBox` in `core/model`.

## What is sealed

| Step | Plaintext inside the ciphertext | What comes back |
|------|----------------------------------|-----------------|
| Fetch | `lookup`, biller id, bill number | Amount and account name, or a failure |
| Pay | Card id, biller id, amount, PIN, bill number | Success or failure |

The user sees the amount before paying. A bill number that is not on file never reaches the card or the PIN. The screen then shows **Bill not fetched**: the board did not return a bill, nothing was charged, and the user can correct the number.

In this demo the board has `104829` (₹500.00) and `220184` (₹1,250.00). Any other number fails the lookup on purpose.

## Why this pair

A bill payment needs two different jobs:

1. **Agree a key** with the other side without sending that key.
2. **Hide and authenticate** the bill number, the amount, the card id, and the PIN.

No single primitive does both well. ECDH does the agreement. AES-GCM does the message. That split is the usual one in TLS: a handshake, then an authenticated cipher.

### AES-256-GCM for the message

AES-GCM is authenticated encryption. One call gives confidentiality and a 128-bit tag over the ciphertext and the IV. If the body or the IV is changed, `open` throws and the payment is rejected. The implementation is `AES/GCM/NoPadding` from the JDK, which Android has had since well before this app's minimum SDK (26):

- 256-bit key from the SHA-256 digest below
- 12-byte random IV, the length NIST SP 800-38D specifies for GCM
- 128-bit tag
- no padding, so there is no padding oracle

A fresh P-256 key pair is generated for each lookup and each payment, so the AES key is used for one message. GCM's real operational rule — never reuse an IV under the same key — is then easy to keep.

AES is the right default on this app because current phones have AES hardware, the platform provider already implements GCM, and adding a third-party crypto library would not make the bill any safer.

### ECDH P-256 for the key

Each side has an ephemeral P-256 (`secp256r1`) key pair. Each combines its own private key with the other side's public key. Both get the same shared secret. Only the public key travels with the ciphertext (`SealedPay.clientPublic`). The server's public key is fetched from `pay/key` on a live host, or kept in process in the mock.

P-256 is the curve `KeyPairGenerator` and `KeyAgreement` ("ECDH") expose on the Android provider this project already uses. No extra dependency.

### SHA-256 as the KDF

The raw ECDH secret is not an AES key. `PayBox.agree` hashes it with SHA-256 and uses the 32-byte digest as AES-256. One message needs one key, and the key pair is thrown away afterwards, so there is no salt and no second derived key.

`ponytail: SHA-256 of the ECDH secret; HKDF if a second key is needed.` HKDF-SHA-256 is the upgrade when one shared secret must become more than one key (for example a separate response key) or when a salt and an info string should bind the key to "bill lookup" versus "bill pay".

## Why not the others — and when those others are the right tool

"Best" here means best for this phone, this JDK provider, and this payload. Several algorithms below are the better choice in a different setting.

### AES-CBC, AES-CTR, and AES-ECB

**ECB** encrypts each block on its own. Equal blocks stay equal, so a structured payload leaks shape. Do not use it for a bill or a PIN.

**CTR** is a stream mode. It hides the bytes and nothing else. An attacker who can flip ciphertext bits flips the same bits in the bill number or the amount. It is the right primitive only as the encryption half of an authenticated construction (GCM is CTR plus a tag).

**CBC** hides the bytes if the IV is random, but it does not authenticate them. It also needs padding. A padding oracle (the class of bug behind several TLS attacks) lets an attacker decrypt by submitting modified ciphertext and watching which errors come back. CBC plus HMAC-SHA-256 (encrypt-then-MAC, independent keys) is a sound legacy design. Use it when the other side cannot do GCM — older HSMs, some bank gateways — not when both sides are ours.

### ChaCha20-Poly1305

This is the other modern authenticated cipher, and TLS treats it as a peer of AES-GCM. It is constant-time in software and faster than AES on devices **without** AES hardware (many older phones, some small servers). Use it there, or where a profile shows AES-GCM is the slow part.

It is not used here because `Cipher.getInstance("AES/GCM/NoPadding")` is the provider default on our minimum SDK, and the phones this app targets have AES hardware. Shipping a second cipher would mean a second set of tests for the same threat.

### AES-GCM-SIV

GCM-SIV stays secure even if an IV is reused. Use it when a key is long-lived and an IV collision is plausible (a counter that might reset, a random IV drawn many times under one key). Each bill here uses a new ECDH key, so the misuse GCM-SIV exists to survive does not apply. It is also not in the default Android `Cipher` set, so adopting it means a new dependency.

### RSA (OAEP for the key, PKCS#1 for the data)

RSA-OAEP is a good way to **wrap** an AES key for a party that already has a long-term RSA certificate and cannot run ECDH. The wrapped key is small; AES still encrypts the bill.

RSA is a poor way to encrypt the bill itself: slow, size-capped by the modulus, and a static RSA key does not give forward secrecy. PKCS#1 v1.5 encryption is worse; it has a padding oracle. We do not use RSA because both sides can do ECDH, and then the AES key never needs to be transported.

### Static or hardcoded AES keys

A key baked into the APK is extractable. ECDH avoids putting the payment key in the app and avoids sending it. A long-term AES key on the server would also decrypt every past bill if that key leaked. Ephemeral ECDH limits a leaked private key to the messages that used it.

### 3DES, DES, RC4, Blowfish

DES and 3DES use a 64-bit block. Sweet32-style birthday attacks are practical against long-lived 3DES sessions, and DES keys are short. RC4 has known biases; TLS removed it. Blowfish has the same 64-bit block problem. None of them are authenticated. They show up only when a counterparty contractually demands them. This app does not have that constraint.

### X25519 instead of P-256

X25519 is the better Diffie–Hellman for new protocols: smaller keys, simpler, designed to be constant-time. Use it when the provider on every device you ship is guaranteed to offer it (or you accept a maintained library such as the platform Conscrypt build that exposes it) and the server agrees. P-256 stays here because `ECGenParameterSpec("secp256r1")` plus `KeyAgreement.getInstance("ECDH")` is already on the provider, with no added library. P-256 gives about 128 bits of security. Hashing that secret with SHA-256 and feeding it to AES-256 does not raise the agreement to 256 bits; the curve is the ceiling. That is enough for a bill payload. Move to X25519 if the server standard changes, not to squeeze 256-bit agreement strength out of P-256.

### HMAC alone, or a hash of the bill number

A hash or an HMAC proves something was not edited. It does not hide the bill number or the PIN. Those fields need encryption as well as a tag. GCM's tag replaces a separate HMAC for this payload.

## What this does not claim

The mock PIN check is "four digits". That is a demo rule, not a substitute for a hardware-backed PIN or a card scheme cryptogram. Sealing a weak PIN still hides it in transit; it does not make the PIN strong.

The lookup response (amount and holder) is returned in the clear after the mock decrypts the request, because the user has to read the amount before paying. The bill number and the PIN are the values that stay inside the ciphertext until `PayBox.open`.
