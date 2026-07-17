# Sensitive identity transport protection

## Current storage decision

Department-registration PAN and GST values are application-layer encrypted only while travelling from the browser to the server. After RSA-OAEP decryption and strict server validation, the normalized identifiers are stored in the existing `pan_no` and `gst_no` plaintext columns. No hash, blind index, AES envelope, encryption key ID, IV, or authentication tag is stored in the database.

This is an explicit deployment decision. Database, replica, dump, snapshot, and backup access must therefore be tightly controlled and protected with platform/disk encryption. A database read compromise can reveal full identifiers.

No hash, encrypted-envelope field, or automatic destructive schema migration is introduced. If a particular database was modified by an abandoned interim build, inventory and back it up before performing any operator-run column cleanup.

## Browser request contract

The page fetches the existing ephemeral RSA-OAEP-256 public key and submits:

```json
{
  "panNumberEncrypted": "<RSA-OAEP ciphertext>",
  "gstNumberEncrypted": "<RSA-OAEP ciphertext>",
  "encryptionKeyId": "<published key id>",
  "timestamp": "<current epoch milliseconds>",
  "nonce": "<random base64url nonce>"
}
```

Visible PAN/GST controls have no HTML `name`, so plaintext is never included in a normal multipart submission. HTTPS remains mandatory outside loopback development. The key ID, timestamp, nonce, endpoint purpose, and logical field name are included inside each RSA-encrypted envelope as well as request metadata. This binding prevents a captured ciphertext from being replayed with a replacement nonce or timestamp. The server permits five minutes of clock age and consumes a request nonce once.

Plaintext request properties such as `panNo`, `panNumber`, `gstNo`, or `gstNumber` are rejected by `/register/department-registration`.

PAN/GST/TAN documents remain protected by HTTPS/TLS in transit. They are not staged or written to application storage until the encrypted PAN/GST envelopes, ordinary form validation, and OTP checks have succeeded. Validation failures require the user to select the documents again; no client-supplied hidden storage path is accepted.

## Server processing

The server validates transport key ID, timestamp, nonce, replay status, and ciphertext before accepting both fields as one request. Decrypted values must match:

- PAN: `[A-Z]{5}[0-9]{4}[A-Z]`
- GST: `[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]`

Crypto and format failures return only `Unable to process the submitted identity information.` Decrypted values are not logged or returned to the browser.

## Display and responses

Normal department profile, edit, auditor, and invoice rendering uses masked values only:

```json
{"panMasked":"XXXXXX546F","gstMasked":"XXXXXXXXXXXF1Z5"}
```

Entity JSON serialization and generated `toString()` exclude the stored plaintext PAN/GST fields. Profile editing preserves the stored identifiers; the ordinary profile form cannot replace them in plaintext.

## Operations

- Keep `spring.jpa.show-sql=false` and Hibernate JDBC bind logging disabled.
- Do not log form/request DTOs containing decrypted values.
- Protect database backups and exports with access control and storage encryption.
- RSA transport keys and replay state are currently node-local. Multi-node deployment requires sticky routing or shared key/replay infrastructure.
