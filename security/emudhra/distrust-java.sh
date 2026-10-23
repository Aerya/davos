#!/bin/sh
set -eu
# Remove only the two roots identified by the incident, by SHA-256 fingerprint.
# This updates the image's Java store; OS roots are maintained by ca-certificates.
for store in "$JAVA_HOME/lib/security/cacerts" "$JAVA_HOME/jre/lib/security/cacerts"; do
    [ -f "$store" ] || continue
    keytool -J-Duser.language=en -list -v -keystore "$store" -storepass changeit > /tmp/java-roots.txt
    awk '
      /^Alias name: / { alias=substr($0,13) }
      /SHA256: / {
        fingerprint=$2; gsub(":", "", fingerprint)
        if (fingerprint == "D7A7A0FB5D7E2731D771E9484EBCDEF71D5F0C3E0A2948782BC83EE0EA699EF4" ||
            fingerprint == "86A1ECBA089C4A8D3BBE2734C612BA341D813E043CF9E8A862CD5C57A36BBE6B") print alias
      }' /tmp/java-roots.txt > /tmp/java-distrust.txt
    while IFS= read -r alias; do
        keytool -delete -alias "$alias" -keystore "$store" -storepass changeit
    done < /tmp/java-distrust.txt
    rm /tmp/java-roots.txt /tmp/java-distrust.txt
done
