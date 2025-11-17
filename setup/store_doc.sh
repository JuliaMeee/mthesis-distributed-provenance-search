#!/bin/bash

STORAGE_CONTAINER_NAME=$1
ORG_NAME=$2
ORG_KEY_PATH=$3
FILE_PATH=$4

FILE_NAME_WITH_EXT=$(basename "$FILE_PATH")
FILE_NAME="${FILE_NAME_WITH_EXT%.*}"

read STORAGE_INNER_IP STORAGE_INNER_PORT STORAGE_HOST_PORT < <(./get_container_network_info.sh $STORAGE_CONTAINER_NAME)

TEMP_FILE=$(mktemp)
cp $FILE_PATH $TEMP_FILE

openssl dgst -sha256 -sign $ORG_KEY_PATH -out /tmp/sign.sha256 $TEMP_FILE

DOCUMENT=$(base64 -w 0 $TEMP_FILE)
SIGNATURE=$(base64 -w 0 /tmp/sign.sha256)

CURRENT_TIMESTAMP=$(date +%s)

PAYLOAD_TEMP_FILE=$(mktemp)
cat <<EOF > "$PAYLOAD_TEMP_FILE"
{
    "document": "$DOCUMENT",
    "documentFormat": "json",
    "signature": "$SIGNATURE",
    "clearancePeriod": 30,
    "createdOn": $CURRENT_TIMESTAMP
}
EOF

curl --location "http://localhost:${STORAGE_HOST_PORT}/api/v1/organizations/${ORG_NAME}/documents/${FILE_NAME}" \
     --header 'Content-Type: application/json' \
     --data "@${PAYLOAD_TEMP_FILE}"