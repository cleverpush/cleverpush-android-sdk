## Deployment

Generate key:
gpg --gen-key

Export keys:
gpg --export-secret-keys >~/.gnupg/secring.gpg

Get 4 Byte ID:
gpg --list-keys --keyid-format SHORT
