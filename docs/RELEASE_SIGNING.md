# AURA Music permanent release signing

Every public AURA Music APK must be signed with the same private keystore. Do not regenerate it after the first signed release: Android will reject an update signed with a different key.

## Required GitHub Actions repository secrets

Open **Settings → Secrets and variables → Actions → New repository secret** and add:

- `KEYSTORE_BASE64`: the complete one-line Base64 encoding of the JKS keystore file.
- `STORE_PASSWORD`: the keystore password.
- `KEY_ALIAS`: the signing key alias.
- `KEY_PASSWORD`: the signing key password.

## Generate the keystore on your Android phone

1. Install **Termux** from [F-Droid](https://f-droid.org/packages/com.termux/). Do not use an untrusted APK mirror.
2. Open Termux and run:

```sh
pkg update && pkg upgrade -y
pkg install openjdk-21 -y
keytool -help
mkdir -p ~/aura-signing
cd ~/aura-signing
keytool -genkeypair -v -keystore aura-release.jks -storetype JKS -alias aura-release -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=AURA Music, OU=Android, O=AURA, C=IN"
```

When prompted, choose a strong keystore password and record it safely. When keytool asks for the key password, press **Enter** to use the same password. Use alias `aura-release`.

3. Create a text file containing the one-line Base64 value and keep an original backup of the keystore:

```sh
termux-setup-storage
mkdir -p ~/storage/shared/Download ~/storage/shared/Documents
base64 aura-release.jks | tr -d '\\n' > ~/storage/shared/Download/aura-release-base64.txt
cp aura-release.jks ~/storage/shared/Documents/aura-release.jks
```

Open `Download/aura-release-base64.txt` in a text editor, copy its entire contents, and paste it into the `KEYSTORE_BASE64` secret. Enter your recorded password for both password secrets and `aura-release` for `KEY_ALIAS`. Keep the original `aura-release.jks` and password in a separate secure backup. **Never paste the keystore or passwords into this chat.**

Generate the keystore once on a trusted device with a JDK:

```sh
keytool -genkeypair -v -keystore aura-release.jks -storetype JKS -alias aura-release -keyalg RSA -keysize 4096 -validity 10000
base64 < aura-release.jks | tr -d '\n'
```

Copy only the Base64 output into the `KEYSTORE_BASE64` secret. Enter the same alias and passwords in their respective secrets. Store the original `aura-release.jks` and its passwords in at least two secure offline backups.

Never commit the keystore, put its passwords in source code, print secret values in Actions logs, or send the keystore/passwords in chat.

## Build a signed APK

After all four secrets exist, open **Actions → Android CI → Run workflow**, select `main`, and run it. The workflow validates the keystore and alias, then builds `universalFossRelease` and uploads the signed APK as `AURA-Music-Universal-FOSS-APK`.

Pull-request builds run tests only. They do not receive the signing secrets and do not create distributable release APKs. If signing secrets are missing on a main-branch run, the workflow intentionally skips APK generation rather than creating another temporary-key APK.

## One-time migration notice

Older AURA builds created by CI before permanent signing was configured used temporary runner-generated debug keys. The new permanent key cannot match those lost private keys. Install the first permanent-key APK after uninstalling the old AURA build once. Every later release must use this same keystore so Android can install updates without uninstalling.
