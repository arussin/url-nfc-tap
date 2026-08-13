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

backup_dir="$repo_root/.play-signing"
keystore_path="$backup_dir/url-nfc-tap-upload-key.p12"
certificate_path="$backup_dir/upload-certificate.pem"
credentials_path="$backup_dir/credentials.txt"
archive_path="$backup_dir/url-nfc-tap-signing-backup.tar.gz"
key_alias="upload"

if ! gh auth status > /dev/null 2>&1; then
  echo "GitHub CLI is not authenticated. Run 'gh auth login' and try again." >&2
  exit 1
fi

umask 077
mkdir -p "$backup_dir"

if [[ -f "$keystore_path" && -f "$credentials_path" ]]; then
  key_alias="$(sed -n 's/^Alias: //p' "$credentials_path")"
  upload_password="$(sed -n 's/^Password: //p' "$credentials_path")"
  if [[ -z "$key_alias" || -z "$upload_password" ]]; then
    echo "The existing credentials file is incomplete; refusing to replace the key." >&2
    exit 1
  fi
  keytool -list \
    -keystore "$keystore_path" \
    -alias "$key_alias" \
    -storepass "$upload_password" > /dev/null
  echo "Reusing the signing key already generated in .play-signing."
elif [[ -e "$keystore_path" || -e "$credentials_path" ]]; then
  echo "Only part of the signing material exists; refusing to overwrite it." >&2
  exit 1
else
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

  printf 'Alias: %s\nPassword: %s\n' "$key_alias" "$upload_password" > "$credentials_path"
fi

keytool -export -rfc \
  -keystore "$keystore_path" \
  -alias "$key_alias" \
  -storepass "$upload_password" \
  -file "$certificate_path"

tar -czf "$archive_path" \
  -C "$backup_dir" \
  "$(basename "$keystore_path")" \
  "$(basename "$certificate_path")" \
  "$(basename "$credentials_path")"

if ! {
  base64 < "$keystore_path" | tr -d '\n' | gh secret set PLAY_UPLOAD_KEYSTORE_BASE64 &&
  printf '%s' "$key_alias" | gh secret set PLAY_UPLOAD_KEY_ALIAS &&
  printf '%s' "$upload_password" | gh secret set PLAY_UPLOAD_KEYSTORE_PASSWORD &&
  printf '%s' "$upload_password" | gh secret set PLAY_UPLOAD_KEY_PASSWORD
}; then
  echo >&2
  echo "The signing key is safe, but this Codespace token cannot manage Actions secrets." >&2
  echo "Run these commands in this same terminal, then run this script again:" >&2
  echo >&2
  echo "  unset GITHUB_TOKEN GH_TOKEN" >&2
  echo "  gh auth login --hostname github.com --git-protocol https --web --scopes repo,workflow" >&2
  echo "  ./scripts/setup-play-upload-key.sh" >&2
  exit 1
fi

echo
echo "Play upload-key secrets are configured for this GitHub repository."
echo "Download and privately store: .play-signing/$(basename "$archive_path")"
echo "Upload to Play Console when requesting the key reset: .play-signing/$(basename "$certificate_path")"
