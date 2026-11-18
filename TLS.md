# Local and production security configuration

The app uses three separate secrets for different purposes:

- `server-keystore.p12` presents the HTTPS server certificate.
- `jwt-signing-keystore.p12` holds the private key that signs JWTs.
- `AES_DB_KEY` encrypts sensitive database columns.

Do not reuse the TLS certificate as the JWT signing key. Keep all keys and
passwords out of source control. The `certs` directory and PKCS12 files are
ignored by Git.

## Local development

Create separate self-signed keystores from PowerShell:

```powershell
New-Item -ItemType Directory -Force certs
keytool -genkeypair -alias server -keyalg RSA -keysize 3072 -storetype PKCS12 `
  -keystore certs\server-keystore.p12 -validity 365 `
  -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"
keytool -genkeypair -alias jwt-signing -keyalg RSA -keysize 3072 -storetype PKCS12 `
  -keystore certs\jwt-signing-keystore.p12 -validity 3650 `
  -dname "CN=bank-app-jwt-signing"
```

`keytool` prompts for each keystore password and key password. PKCS12 keystores
use the same password for the store and private key, so press Enter at each
key-password prompt to reuse the store password. Set `JWT_KEY_PASSWORD` to the
same value as `JWT_KEY_STORE_PASSWORD`.

Set the required values in the PowerShell process that will run Compose:

```powershell
$env:SSL_PASSWORD = Read-Host "Server keystore password"
$env:JWT_KEY_STORE_PASSWORD = Read-Host "JWT keystore password"
$env:JWT_KEY_PASSWORD = Read-Host "JWT private key password"
$env:DB_PASSWORD = Read-Host "PostgreSQL password"
$env:REDIS_PASSWORD = Read-Host "Redis password"
$env:GITHUB_CLIENT_ID = Read-Host "GitHub OAuth client ID"
$env:GITHUB_CLIENT_SECRET = Read-Host "GitHub OAuth client secret"
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 24
$rng.GetBytes($bytes)
$env:AES_DB_KEY = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

The generated `AES_DB_KEY` is 32 ASCII bytes, which is valid for AES-256. Save
the value securely and reuse it for the same database. Changing it without
re-encrypting existing rows makes encrypted values unreadable.

Build the application and start the services:

```powershell
.\mvnw.cmd package
docker compose up --build
```

The self-signed HTTPS certificate is only for local testing; browsers will
warn until it is trusted. Use actual GitHub OAuth app credentials if testing
OAuth login.

## Production

Use a CA-issued TLS certificate whose Subject Alternative Name (SAN) includes
the API hostname. Store and mount both keystores read-only from your
deployment's secret manager, and inject all keystore passwords, database and
Redis credentials, OAuth client credentials, and `AES_DB_KEY` through secrets
management. The JWT signing key must be stable across application restarts and
protected separately from the TLS key.

This config enables HTTPS for the server; it does not enable mutual TLS. The
previous keystore was also used for JWT signing, so replacing it invalidates
tokens signed by that key. The old private key was committed to Git; rotate it
and treat it as exposed. Removing it from the current revision does not remove
it from Git history.

The old AES key was also present in source. If a database contains values
encrypted with that key, do not switch to a new key until those values are
re-encrypted; rotate the key as a controlled data migration.
