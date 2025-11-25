from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509 import load_pem_x509_certificate
import datetime
from pathlib import Path

# functions from Matej Gallos simulation of the provenance application https://gitlab.ics.muni.cz/422328/dbprov/
def generate_certificate(
    country_tag: str,
    name: str,
    auth_key: Path | serialization.SSHCertPrivateKeyTypes | None = None,
    auth_cert: Path | x509.Certificate | None = None,
    ca: bool = True,
    path_length: int | None = None,
):
    # Generate our key
    key = ec.generate_private_key(ec.SECP256R1())

    subject = x509.Name(
        [
            x509.NameAttribute(NameOID.COUNTRY_NAME, country_tag),
            x509.NameAttribute(
                NameOID.ORGANIZATION_NAME, f"Distributed Provenance Demo {name}"
            ),
            x509.NameAttribute(NameOID.COMMON_NAME, f"DPD {name}"),
        ]
    )

    if auth_cert:
        auth_cert = parse_certificate(auth_cert)
        auth_key = parse_key(auth_key)
    cert = (
        x509.CertificateBuilder()
        .subject_name(subject)
        .issuer_name(auth_cert.subject if auth_cert else subject)
        .public_key(key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(datetime.datetime.now(datetime.timezone.utc))
        .not_valid_after(
            # Our certificate will be valid for ~10 years
            datetime.datetime.now(datetime.timezone.utc)
            + datetime.timedelta(days=365 * 10)
        )
        .add_extension(
            x509.BasicConstraints(ca=ca, path_length=path_length),
            critical=True,
        )
        .add_extension(
            x509.KeyUsage(
                digital_signature=True,
                content_commitment=False,
                key_encipherment=not ca,
                data_encipherment=False,
                key_agreement=False,
                key_cert_sign=ca,
                crl_sign=True,
                encipher_only=False,
                decipher_only=False,
            ),
            critical=True,
        )
        .add_extension(
            x509.SubjectKeyIdentifier.from_public_key(key.public_key()),
            critical=False,
        )
    )
    if not ca:
        cert = cert.add_extension(
            x509.ExtendedKeyUsage(
                [
                    x509.ExtendedKeyUsageOID.CLIENT_AUTH,
                    x509.ExtendedKeyUsageOID.SERVER_AUTH,
                ]
            ),
            critical=False,
        )
    if auth_cert:
        cert = cert.add_extension(
            x509.AuthorityKeyIdentifier.from_issuer_subject_key_identifier(
                auth_cert.extensions.get_extension_for_class(
                    x509.SubjectKeyIdentifier
                ).value
            ),
            critical=False,
        ).sign(auth_key, hashes.SHA256())
    else:
        cert = cert.sign(key, hashes.SHA256())

    return key, cert

def load_certificate(cert_filepath: Path, as_string: bool = False):
    with cert_filepath.open("rb") as file:
        cert = file.read()
    if as_string:
        return cert.decode("utf-8")
    return load_pem_x509_certificate(cert)


def load_private_key(key_filepath: Path, password: str | None = None):
    with key_filepath.open("rb") as file:
        key = file.read()
    return serialization.load_pem_private_key(key, password=password)

def parse_certificate(certificate: x509.Certificate | Path, as_string: bool = False):
    if isinstance(certificate, x509.Certificate):
        if as_string:
            return certificate.public_bytes(serialization.Encoding.PEM).decode("utf-8")
        return certificate
    elif isinstance(certificate, Path):
        return load_certificate(certificate, as_string=as_string)
    else:
        raise ValueError(
            f"Invalid certificate type. Expected 'Path' or 'x509.Certificate' but got {type(certificate)} instead."
        )


def parse_key(
    key: serialization.SSHCertPrivateKeyTypes | Path, password: str | None = None
):
    if isinstance(key, serialization.SSHCertPrivateKeyTypes):
        return key
    elif isinstance(key, Path):
        return load_private_key(key, password)
    else:
        raise ValueError(
            f"Invalid private key type. Expected 'Path' or 'SSHCertPrivateTypes' but got {type(key)} instead."
        )