from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding
import sys
import jcs
import base64
from datetime import datetime
import json

with open(sys.argv[1], "rb") as file:
    priv_key = file.read()

private_key = serialization.load_pem_private_key(
    priv_key,
    password=None,
)

with open(sys.argv[2], 'rb') as f:
    content = f.read()

graph_b64 = base64.b64encode(content)
signature = private_key.sign(
    content,
    padding.PKCS1v15(),
    hashes.SHA256()
)

sig_b64 = base64.b64encode(signature).decode('utf-8')
data = {
    "document": graph_b64.decode('utf-8'),
    "documentFormat": "json",
    "signature": sig_b64,
    "createdOn": int(datetime.timestamp(datetime.now()))
}

print(json.dumps(data))
