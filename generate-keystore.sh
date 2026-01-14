#!/bin/bash

# Script to generate RSA keystore for JWT signing

KEYSTORE_PATH="authorization-server/src/main/resources/jwt-keystore.jks"
KEY_ALIAS="jwt-key"
KEYSTORE_PASSWORD="changeit"
KEY_PASSWORD="changeit"
VALIDITY_DAYS=3650

echo "Generating RSA keystore for JWT signing..."

# Create resources directory if it doesn't exist
mkdir -p authorization-server/src/main/resources

# Generate keystore with RSA key pair
keytool -genkeypair \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -keystore "$KEYSTORE_PATH" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -validity $VALIDITY_DAYS \
  -dname "CN=Authorization Server, OU=Development, O=Luizguizl, L=City, ST=State, C=BR"

if [ $? -eq 0 ]; then
    echo "✓ Keystore generated successfully at: $KEYSTORE_PATH"
    echo "  Key Alias: $KEY_ALIAS"
    echo "  Keystore Password: $KEYSTORE_PASSWORD"
    echo "  Key Password: $KEY_PASSWORD"
else
    echo "✗ Failed to generate keystore"
    exit 1
fi
