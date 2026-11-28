# AURA Music permanent release signing

Every public AURA Music APK must be signed with the same private keystore. Do not regenerate it after the first signed release: Android will reject an update signed with a different key.

## Required GitHub Actions repository secrets

Open **Settings → Secrets and variables → Actions → New repository secret** and add:

- `KEYSTORE_BASE64`: the complete one-line Base64 encoding of the JKS keystore file.
- `STORE_PASSWORD`: the keystore password.
- `KEY_ALIAS`: the signing key alias.
- `KEY_PASSWORD`: the signing key password.

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
