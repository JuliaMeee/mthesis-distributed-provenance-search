#!/bin/bash

STORAGE_CONTAINER_NAME=$1
ORG_NAME=$2
ORG_CERT_FILE=$3

read STORAGE_INNER_IP STORAGE_INNER_PORT STORAGE_HOST_PORT < <(./get_container_network_info.sh $STORAGE_CONTAINER_NAME)

ORG_CERT=$(sed 's/\r//' $ORG_CERT_FILE | awk '{printf "%s\\n", $0}')
INTERMEDIATE_CERT_1=$(sed 's/\r//' ./certificates/int1.pem | awk '{printf "%s\\n", $0}')
INTERMEDIATE_CERT_2=$(sed 's/\r//' ./certificates/int2.pem | awk '{printf "%s\\n", $0}')

PAYLOAD_FILE=$(mktemp)
cat <<EOF > "$PAYLOAD_FILE"
{
    "clientCertificate": "$ORG_CERT",
    "intermediateCertificates": [
        "$INTERMEDIATE_CERT_1",
        "$INTERMEDIATE_CERT_2"
    ],
    "clearancePeriod": 30
}
EOF

curl --location "http://localhost:$STORAGE_HOST_PORT/api/v1/organizations/$ORG_NAME" \
     --header 'Content-Type: application/json' \
     --data "@$PAYLOAD_FILE"
