# CardPulse — Security & Privacy

CardPulse is built on a **Zero-Trust privacy model**. As a user, your financial statements and transaction history are highly sensitive. CardPulse guarantees that your data stays on your device.

---

## Privacy Architecture

### 1. No Internet Permission
CardPulse does not request `android.permission.INTERNET` in its `AndroidManifest.xml`.
- Without this permission, the Android operating system actively blocks the application sandbox from opening network sockets, making it impossible to transmit data over HTTP, TCP, or UDP.
- You can verify this at the OS level by navigating to **App Info -> Permissions** in your Android settings. You will see that "Network Access" is completely absent.

### 2. In-Memory Decryption
For password-protected bank statements (which contain password encryption standard for Indian banks):
- Decryption occurs **entirely in-memory** inside the `PDDocument.load(stream, password)` block.
- Cleartext passwords are never logged, never cached in local databases, and never written to temporary disk storage.
- If you close the app or reset the import screen, the password strings are instantly garbage-collected from RAM.

### 3. Local Sandboxed Storage
- All imported statements, parsed transaction details, and user configuration settings are saved in the app's private sandbox storage (`/data/data/com.example.cardpulse/databases/` and `/data/data/com.example.cardpulse/files/datastore/`).
- On standard Android, other applications on your device cannot access this directory due to kernel-level sandboxing (User ID isolation).

### 4. Zero Storage Permissions required
- CardPulse uses the Android Storage Access Framework (SAF) via the standard file selector launcher (`ActivityResultContracts.OpenDocument()`).
- This allows you to select a PDF statement from your device's filesystem without granting the app read/write permissions to your entire storage (such as `READ_EXTERNAL_STORAGE`).

### 5. Safe Clipboard Backups
- The export-backup mechanism generates a JSON representation of your cards and transactions. It does not export PDF passwords.
- Wiping all local data (`Settings -> Danger Zone`) performs a complete purge of SQLite tables and deletes DataStore preference files from disk, restoring the app to its fresh install state.
