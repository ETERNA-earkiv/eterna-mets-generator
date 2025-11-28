#!/bin/bash

echo Generating code signing keystore
openssl \
    pkcs12 \
    -export \
    -in ./codesign/code-sign.pem \
    -inkey ./codesign/code-sign.key \
    -certfile ./codesign/plugin-ca.pem \
    -name codesign \
    -password pass:changeit \
    -out ./codesign/code-sign.p12

echo Signing jar
jarsigner \
    -keystore ./codesign/code-sign.p12 \
    -storepass changeit \
    -signedjar "./codesign/$JAR_FILE" \
    "./target/$PLUGIN_BASENAME/$JAR_FILE" \
    codesign
