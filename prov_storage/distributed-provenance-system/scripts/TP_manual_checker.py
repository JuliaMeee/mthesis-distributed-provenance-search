from neo4j import GraphDatabase, RoutingControl
import subprocess

URI = "neo4j://localhost:7687"
AUTH = ("neo4j", "password")


def print_organization_info(org):
    print(f"Organization info: \n"
          f"\tidentifier: {org['identifier']}\n"
          f"\tclient certificate: {org['client_cert']}\n"
          f"\tintermediate certificates:")

    for cert in org['intermediate_certs']:
        print(f"\t\t{cert}")


def print_tp_info(tp):
    print(f"Trusted party info: \n"
          f"\tidentifier: {tp['identifier']}\n"
          f"\tcertificate: {tp['certificate']}\n"
          f"\turl: {tp['url']}")


def print_trusted_parties(trusted_parties):
    print("All trusted parties:")
    for id in trusted_parties.keys():
        print(f"\t{id}")


def print_organizations(organizations):
    print("All organizations:")
    for id in organizations.keys():
        print(f"\t{id}")


def mark_tp_record_as_valid(identifier):
    with GraphDatabase.driver(URI, auth=AUTH) as driver:
        driver.execute_query(
            "MATCH (org:Organization)-[:trusts]->(tp:TrustedParty) WHERE tp.identifier = $id "
            "SET tp.checked = true, tp.valid = true",
            id=identifier, database_="neo4j", routing_=RoutingControl.WRITE,
        )


def show_certificate_details(certificate):
    try:
        result = subprocess.run(['openssl', 'x509', '-noout', '-text'], input=certificate, capture_output=True, text=True)

        if result.returncode == 0:
            print(result.stdout)
        else:
            print(f"Error: {result.stderr}")
    except FileNotFoundError:
        print("Error: OpenSSL command not found.")


def mark_tp_record_as_invalid(identifier):
    with GraphDatabase.driver(URI, auth=AUTH) as driver:
        driver.execute_query(
            "MATCH (org:Organization)-[:trusts]->(tp:TrustedParty) WHERE tp.identifier = $id "
            "SET tp.checked = true",
            id=identifier, database_="neo4j", routing_=RoutingControl.WRITE,
        )


def get_unchecked_TPs(driver):
    records, _, _ = driver.execute_query(
        "MATCH (organization:Organization)-[:trusts]->(tp) "
        "WHERE not tp.checked "
        "RETURN organization, tp",
        database_="neo4j", routing_=RoutingControl.READ,
    )

    trusted_parties = dict()
    organizations = dict()
    for record in records:
        tp = record['tp']
        org = record['organization']

        trusted_parties[tp['identifier']] = tp
        organizations[org['identifier']] = org

    return organizations, trusted_parties


def print_all_relations(organizations, trusted_parties):
    assert len(organizations) == len(trusted_parties)

    for i, (org_id, tp_id) in enumerate(zip(organizations.keys(), trusted_parties.keys())):
        print(f"Record number {i + 1}: "
              f"(Organization [{org_id}])-trusts->(TrustedParty [{tp_id}])")


if __name__ == '__main__':
    with GraphDatabase.driver(URI, auth=AUTH) as driver:
        organizations, trusted_parties = get_unchecked_TPs(driver)

    decision = 0
    while decision != 9:
        print("What do you want to do?\n"
              "\t1) Print all trusted parties\n"
              "\t2) Print all organizations\n"
              "\t3) Print relations\n"
              "\t4) Print trusted party info\n"
              "\t5) Print organization info\n"
              "\t6) Show TP certificate details\n"
              "\t7) Mark TP as VALID\n"
              "\t8) Mark TP as INVALID\n"
              "\t9) End script")

        try:
            decision = int(input("Input: "))
        except:
            print("Invalid symbol!")
            exit(1)

        trusted_party = None
        if decision in (4, 6, 7, 8):
            id = input("Trusted party ID: ")
            if id not in trusted_parties.keys():
                print(f"'{id}' is not a valid ID of TP!")
                print("----------------------------------")
                continue
        elif decision == 5:
            id = input("Organization ID:")
            if id not in organizations.keys():
                print(f"'{id}' is not a valid ID of organization!")
                print("----------------------------------")
                continue

        if decision == 1:
            with GraphDatabase.driver(URI, auth=AUTH) as driver:
                organizations, trusted_parties = get_unchecked_TPs(driver)
            print_trusted_parties(trusted_parties)
        elif decision == 2:
            with GraphDatabase.driver(URI, auth=AUTH) as driver:
                organizations, trusted_parties = get_unchecked_TPs(driver)
            print_organizations(organizations)
        elif decision == 3:
            with GraphDatabase.driver(URI, auth=AUTH) as driver:
                organizations, trusted_parties = get_unchecked_TPs(driver)
            print_all_relations(organizations, trusted_parties)
        elif decision == 4:
            print_tp_info(trusted_parties[id])
        elif decision == 5:
            print_organization_info(organizations[id])
        elif decision == 6:
            show_certificate_details(trusted_parties[id]['certificate'])
        elif decision == 7:
            mark_tp_record_as_valid(trusted_parties[id]['identifier'])
        elif decision == 8:
            mark_tp_record_as_invalid(trusted_parties[id]['identifier'])

        print("----------------------------------")
