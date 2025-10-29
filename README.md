# mthesis-distributed-provenance-search

<h2>Project structure:</h2>

`/bundle-search`: implementation of <b>bundle-searcher</b> service that should represent a provenance controller and can answer search queries about individual bundles in storage of this provenance controller.  
`/distributed-provenance-search`: implementation of <b>prov-traverser</b>.   
`/prov_storage`: implementation of provenance storage and trusted party from https://gitlab.fi.muni.cz/xmojzis1/provenance-system-tests.  
`/setup`: has sripts and data for demo setup.  

<h2>Setup:</h2>

Build the docker images from working directory `/prov_storage/distributed-provenance-system/` with  
```sh
docker build -f Dockerfile.TrustedParty -t trusted_party .
```  
```sh
docker build -f Dockerfile.ProvStorage -t distributed_prov_system .
```

Build the docker image for traverser service from working directory `/distributed-provenance-search/` with  
```sh
docker build -f Dockerfile -t traverser_image .
```

Build the docker image for bundle-search service from working directory `/bundle-search/` with  
```sh
docker build -f .\Dockerfile -t bundle_search_image .
```

Start docker and from the project root directory run
```sh
docker-compose up -d
```

To register organizations and upload provenance files that can be found in `./setup/data`, run the following command from directory `./setup` (if you get `curl: (52) Empty reply from server`, wait a few seconds and try again)
```sh
./simulation_setup.sh
```


<h2>Services</h2>

With all the containers running, you can check the OpenAPI specification for <b>prov-traverser</b> service that does search of distributed provenance at:  
`http://localhost:8090/swagger-ui/index.html#`.  
The <b>prov-traverser</b> service searches the provenance chain in the given direction looking for the specified target. It fetches results for individual bundles from their respective provenance controllers (bundle-searcher-x containers).  

You can check out the OpenAPI specification for <b>bundle-searcher-x</b> services at:  
`http://localhost:8081/swagger-ui/index.html#`.  
It also has an endpoint that lists all supported targetTypes of search.


Run requests to the prov-traversal container from the <b>debug-shell</b> container running in docker.

 If you get `curl: (7) Failed to connect to prov-traverser port 8000 after 6 ms: Connection refused` soon after starting the container, wait a few seconds and try again.

 <h2>Storage compatibility issues and java serialization issues</h2>
This is a list of issues encountered when working with the storage service and java prov toolbox library.

- Issue: In meta document, id of entity representing a version did not match represented bundle id.  
Fix: Changed the entity id namespaceUri generation in
`prov_storage\distributed-provenance-system\distributed_prov_system\provenance\neomodel2prov.py`.
- Issue: Some values in meta documents were not strings. prov toolbox then could not load the document.  
Fix: Before deserializing json document, stringify all values.
- Issue: In meta documents, "prov:type" attribute values were not serialized as a list of typed values. Prov toolbox then failed to load this attribute.  
Fix: Before deserializing json document, rewrite all "prov:type" attribute values into expected format.
- Issue: Prov toolbox demands explicit bundle id property as "@id".  
Fix: Before deserializing json document, find bundle id and add explicit "@id" property.
- Issue: Prov toolbox does not load bundle id namespaceUri unless prefixes are defined inside the bundle entity.  
Fix: Before deserializing json document, copy document namespace declaration into the bundle entity.

<h2>Example requests</h2>

Example 1:

```sh
# Request 1: in predecessors find nodes with given id. Return their full data.
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-3:8000/api/v1/organizations/ORG3/documents/",
      "localPart": "SpeciesIdentificationBundle_V0"
    },
    "startNodeId": {
      "nameSpaceUri": "https://openprovenance.org/blank/",
      "localPart": "IdentifiedSpeciesCon"
    },
    "versionPreference":"latest",
    "targetSpecification": "https://openprovenance.org/blank/ABSPermit_ircc2345678",
    "targetType": "nodes_by_id"
  }' \
  http://prov-traverser:8000/api/searchPredecessors
```

```sh
# Request 1 response: full data about node matching the id, encapsulated in prov document, serialized as json.
[
  {
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
      "localPart": "SamplingBundle_V1"
    },
    "pathIntegrity": true,
    "integrity": true,
    "pathValidity": false,
    "validity": false,
    "result": {
      "prefix": {
        "schema": "https://schema.org/",
        "prov": "http://www.w3.org/ns/prov#",
        "blank": "https://openprovenance.org/blank/"
      },
      "bundle": {
        "blank:anonymous_encapsulating_bundle": {
          "entity": {
            "blank:ABSPermit_ircc2345678": {
              "schema:name": ["ABS permit"],
              "schema:validIn": ["marineregions:3293"],
              "schema:issuer": [
                "gen:8fefd86e6fd2acc15cd4db7a2ecff042dcde5d323d208c85e7434630d0342588"
              ],
              "schema:validFrom": ["2021-01-01"],
              "schema:identifier": ["ircc2345678"],
              "schema:description": [
                "Samples of sea water from the Belgian EEZ collected over the period 2021-01-01 to 2022-01-01; processed for environmental DNA for taxonomic classification purposes"
              ],
              "schema:validUntil": ["2022-01-01"],
              "prov:type": [
                { "type": "prov:QUALIFIED_NAME", "$": "schema:Permit" },
                { "type": "prov:QUALIFIED_NAME", "$": "schema:Thing" },
                { "type": "prov:QUALIFIED_NAME", "$": "schema:CreativeWork" }
              ]
            }
          }
        }
      }
    }
  }
]
```
---
Example 2:
```sh
# Request 2: in successors find nodes with matching attributes. Return only their ids. Atributes to match are encapsulated in a single node in a bundle in a document, serialzied as json.
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
      "localPart": "SamplingBundle_V0"
    },
    "startNodeId": {
      "nameSpaceUri": "https://openprovenance.org/blank/",
      "localPart": "Sampling"
    },
    "versionPreference": "latest",
    "targetSpecification": {
      "prefix": {
        "cpm": "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/",
        "blank": "https://openprovenance.org/blank/",
        "meta2": "http://prov-storage-2:8000/api/v1/documents/meta/",
        "prov": "http://www.w3.org/ns/prov#",
        "schema": "https://schema.org/"
      },
      "bundle": {
        "blank:does_not_matter": {
          "agent": {
            "blank:does_not_matter": {
              "prov:type": [
                {
                  "type": "prov:QUALIFIED_NAME",
                  "$": "schema:Person"
                }
              ]
            }
          }
        }
      }
    },
    "targetType": "NODE_IDS_BY_ATTRIBUTES"
  }' \
  http://prov-traverser:8000/api/searchSuccessors

```

```sh
# Request 2 response: qualified names of found nodes matching the attributes. Matches were found in 4 different bundles, their order might differ.
[
  {
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
      "localPart": "SamplingBundle_V1"
    },
    "pathIntegrity": true,
    "integrity": true,
    "pathValidity": true,
    "validity": false,
    "result": [
      {
        "nameSpaceUri": "https://orcid.org/",
        "localPart": "0000-0001-0001-0002"
      },
      {
        "nameSpaceUri": "https://orcid.org/",
        "localPart": "0000-0001-0001-0001"
      }
    ]
  },
  {
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-3:8000/api/v1/organizations/ORG3/documents/",
      "localPart": "SpeciesIdentificationBundle_V0"
    },
    "pathIntegrity": true,
    "integrity": true,
    "pathValidity": false,
    "validity": false,
    "result": [
      {
        "nameSpaceUri": "https://orcid.org/",
        "localPart": "0000-0001-0001-0002"
      }
    ]
  },
  {
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-2:8000/api/v1/organizations/ORG2/documents/",
      "localPart": "ProcessingBundle_V1"
    },
    "pathIntegrity": true,
    "integrity": true,
    "pathValidity": false,
    "validity": false,
    "result": [
      {
        "nameSpaceUri": "https://orcid.org/",
        "localPart": "0000-0001-0001-0002"
      }
    ]
  },
  {
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
      "localPart": "DnaSequencingBundle_V0"
    },
    "pathIntegrity": true,
    "integrity": true,
    "pathValidity": false,
    "validity": false,
    "result": [
      {
        "nameSpaceUri": "https://orcid.org/",
        "localPart": "0000-0001-0001-0002"
      },
      {
        "nameSpaceUri": "https://openprovenance.org/blank/",
        "localPart": "DNATechnicianPerson"
      }
    ]
  }
]
```

---
Example 3:

```sh
# Request 3: in predecessors find backward connectors.
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-3:8000/api/v1/organizations/ORG3/documents/",
      "localPart": "SpeciesIdentificationBundle_V0"
    },
    "startNodeId": {
      "nameSpaceUri": "https://openprovenance.org/blank/",
      "localPart": "IdentifiedSpeciesCon"
    },
    "versionPreference":"latest",
    "targetSpecification": "backward",
    "targetType": "connectors"
  }' \
  http://prov-traverser:8000/api/searchPredecessors
```
```sh
# Request 3 response: found backward connectors data in custom DTO.
[
  {
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-2:8000/api/v1/organizations/ORG2/documents/",
      "localPart": "ProcessingBundle_V1"
    },
    "pathIntegrity": true,
    "integrity": true,
    "pathValidity": false,
    "validity": false,
    "result": [
      {
        "id": {
          "nameSpaceUri": "https://openprovenance.org/blank/",
          "localPart": "StoredSampleCon_r1"
        },
        "referencedConnectorId": {
          "nameSpaceUri": "https://openprovenance.org/blank/",
          "localPart": "StoredSampleCon_r1"
        },
        "referencedBundleId": {
          "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
          "localPart": "SamplingBundle_V0"
        },
        "referencedMetaBundleId": {
          "nameSpaceUri": "http://prov-storage-1:8000/api/v1/documents/meta/",
          "localPart": "SamplingBundle_V0_meta"
        },
        "referencedBundleHashValue": "252be860992eef5a1b9292027762bb155b7943e48014f986cec00115a209a553",
        "hashAlg": "SHA256"
      }
    ]
  },
  {
    "bundleId": {
      "nameSpaceUri": "http://prov-storage-3:8000/api/v1/organizations/ORG3/documents/",
      "localPart": "SpeciesIdentificationBundle_V0"
    },
    "pathIntegrity": true,
    "integrity": true,
    "pathValidity": true,
    "validity": false,
    "result": [
      {
        "id": {
          "nameSpaceUri": "https://openprovenance.org/blank/",
          "localPart": "ProcessedSampleCon"
        },
        "referencedConnectorId": {
          "nameSpaceUri": "https://openprovenance.org/blank/",
          "localPart": "ProcessedSampleCon"
        },
        "referencedBundleId": {
          "nameSpaceUri": "http://prov-storage-2:8000/api/v1/organizations/ORG2/documents/",
          "localPart": "ProcessingBundle_V0"
        },
        "referencedMetaBundleId": {
          "nameSpaceUri": "http://prov-storage-2:8000/api/v1/documents/meta/",
          "localPart": "ProcessingBundle_V0_meta"
        },
        "referencedBundleHashValue": "0de33213de06c588e76341c53ab872185b6985100487559ec455cef64779a6fd",
        "hashAlg": "SHA256"
      },
      {
        "id": {
          "nameSpaceUri": "https://openprovenance.org/blank/",
          "localPart": "StoredSampleCon_r1"
        },
        "referencedConnectorId": {
          "nameSpaceUri": "https://openprovenance.org/blank/",
          "localPart": "StoredSampleCon_r1"
        },
        "referencedBundleId": {
          "nameSpaceUri": "http://prov-storage-1:8000/api/v1/organizations/ORG1/documents/",
          "localPart": "SamplingBundle_V0"
        },
        "referencedMetaBundleId": {
          "nameSpaceUri": "http://prov-storage-1:8000/api/v1/documents/meta/",
          "localPart": "SamplingBundle_V0_meta"
        },
        "referencedBundleHashValue": "252be860992eef5a1b9292027762bb155b7943e48014f986cec00115a209a553",
        "hashAlg": "SHA256"
      }
    ]
  }
]
```
