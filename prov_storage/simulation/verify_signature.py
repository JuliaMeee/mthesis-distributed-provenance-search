import base64
import cryptography.exceptions
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.serialization import load_pem_public_key
from cryptography.x509 import load_pem_x509_certificate
import sys
import json
import jcs

# Load the public key.
with open(sys.argv[1], 'rb') as f:
    cert = f.read().decode('utf-8').replace('\\n', '\n').encode('utf-8')
    loaded_cert = load_pem_x509_certificate(cert, backend=default_backend())
    public_key = loaded_cert.public_key()

# Load the payload contents and the signature.
with open(sys.argv[2], 'rb') as f:
    token = json.load(f)
    token = token['token']

signature = base64.b64decode(token['signature'])

# Perform the verification.
try:
    public_key.verify(
        signature,
        jcs.canonicalize(token['data']),
        padding.PKCS1v15(),
        hashes.SHA256(),
    )
    print("Alles gut")
except cryptography.exceptions.InvalidSignature as e:
    print('ERROR: Payload and signature files failed verification!')
