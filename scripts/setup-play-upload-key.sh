#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

for required_command in gh keytool openssl base64 tar; do
  if ! command -v "$required_command" > /dev/null; then
    echo "Missing required command: $required_command" >&2
    exit 1
  fi
done

if ! gh auth status > /dev/null 2>&1; then
  echo "GitHub CLI is not authenticated. Run 'gh auth login' and try again." >&2
  exit 1
fi

backup_dir="$repo_root/.play-signing"
keystore_path="$backup_dir/url-nfc-tap-upload-key.p12"
certificate_path="$backup_dir/upload-certificate.pem"
credentials_path="$backup_dir/credentials.txt"
archive_path="$backup_dir/url-nfc-tap-signing-backup.tar.gz"
key_alias="upload"

if [[ -e "$keystore_path" || -e "$credentials_path" ]]; then
  echo "Signing material already exists in .play-signing; refusing to overwrite it." >&2
  exit 1
fi

umask 077
mkdir -p "$backup_dir"
upload_password="$(openssl rand -hex 32)"

keytool -genkeypair \
  -keystore "$keystore_path" \
  -storetype PKCS12 \
  -alias "$key_alias" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass "$upload_password" \
  -keypass "$upload_password" \
  -dname "CN=URL NFC Tap Upload, O=URL NFC Tap, C=US" \
  -noprompt

keytool -export -rfc \
  -keystore "$keystore_path" \
  -alias "$key_alias" \
  -storepass "$upload_password" \
  -file "$certificate_path"

printf 'Alias: %s\nPassword: %s\n' "$key_alias" "$upload_password" > "$credentials_path"

base64 < "$keystore_path" | tr -d '\n' | gh secret set PLAY_UPLOAD_KEYSTORE_BASE64
printf '%s' "$key_alias" | gh secret set PLAY_UPLOAD_KEY_ALIAS
printf '%s' "$upload_password" | gh secret set PLAY_UPLOAD_KEYSTORE_PASSWORD
printf '%s' "$upload_password" | gh secret set PLAY_UPLOAD_KEY_PASSWORD

tar -czf "$archive_path" \
  -C "$backup_dir" \
  "$(basename "$keystore_path")" \
  "$(basename "$certificate_path")" \
  "$(basename "$credentials_path")"

echo
echo "Play upload-key secrets are configured for this GitHub repository."
echo "Download and privately store: .play-signing/$(basename "$archive_path")"
echo "Upload to Play Console when requesting the key reset: .play-signing/$(basename "$certificate_path")"
