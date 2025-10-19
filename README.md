# mthesis-distributed-provenance-search

<h2>Setup:</h2>

Build the docker images from working directory `/prov_storage/distributed-provenance-system/` with
`docker build -f Dockerfile.TrustedParty -t trusted_party .`
`docker build -f Dockerfile.ProvStorage -t distributed_prov_system .`

Build the docker image for traverser service from working directory `/distributed-provenance-search/` with
`docker build -f Dockerfile -t traverser_image .`

Build the docker image for bundle-search service from working directory `/bundle-search/` with
`docker build -f .\Dockerfile -t bundle_search_image . `

Start docker and run `docker-compose up -d`.

From `/setup` run:
`./simulation_setup.sh`
to register organizations and upload provenance files that can be found in `./setup/data`.
If you get `curl: (52) Empty reply from server`, try again after waiting for a few seconds.

Run requests to the traversal/search containers from the debug-shell container running in docker.

Example request:
```
curl -X POST -H "Content-Type: application/json" -d '{
    "bundleId": {  
        "nameSpaceUri": "http://prov-storage-3:8000/api/v1/organizations/ORG3/documents/",  
        "localPart": "SpeciesIdentificationBundle_V0"  
        },  
    "connectorId": {  
        "nameSpaceUri": "https://openprovenance.org/blank/",  
        "localPart": "IdentifiedSpeciesCon"  
        },  
    "targetSpecification": "ABSPermit_ircc2345678",   
    "targetType": "demo_node_localname"  
    }' http://prov-traverser:8000/api/searchPredecessors
```
Above request should return the json below, with one result from bundle SamplingBundle_V0. The result is a serialized prov document with one entity that fits the specified local name.
```
[
  {
    "bundleId": {
      "nameSpaceUri": null,
      "localPart": "SamplingBundle_V0"
    },
    "pathIntegrity": false,
    "integrity": false,
    "pathValidity": false,
    "validity": false,
    "result": "{\"prefix\":{\"schema\":\"https://schema.org/\",\"cpm\":\"https://www.commonprovenancemodel.org/cpm-namespace-v1-0/\",\"blank\":\"https://openprovenance.org/blank/\",\"meta1\":\"http://prov-storage-1:8000/api/v1/documents/meta/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"orcid\":\"https://orcid.org/\",\"obolibrary\":\"http://purl.obolibrary.org/obo/\",\"gen\":\"gen/\",\"seadatanet\":\"https://edmo.seadatanet.org/report/\",\"ex\":\"http://example.org/\",\"dct\":\"http://purl.org/dc/terms/\",\"marineregions\":\"http://marineregions.org/mrgid/\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"prov\":\"http://www.w3.org/ns/prov#\",\"sosa\":\"http://www.w3.org/ns/sosa/\"},\"bundle\":{\"ex:connectors_bundle\":{\"prefix\":{\"schema\":\"https://schema.org/\",\"cpm\":\"https://www.commonprovenancemodel.org/cpm-namespace-v1-0/\",\"blank\":\"https://openprovenance.org/blank/\",\"meta1\":\"http://prov-storage-1:8000/api/v1/documents/meta/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"orcid\":\"https://orcid.org/\",\"obolibrary\":\"http://purl.obolibrary.org/obo/\",\"gen\":\"gen/\",\"seadatanet\":\"https://edmo.seadatanet.org/report/\",\"ex\":\"http://example.org/\",\"dct\":\"http://purl.org/dc/terms/\",\"marineregions\":\"http://marineregions.org/mrgid/\",\"dcat\":\"http://www.w3.org/ns/dcat#\",\"prov\":\"http://www.w3.org/ns/prov#\",\"sosa\":\"http://www.w3.org/ns/sosa/\"},\"entity\":{\"blank:ABSPermit_ircc2345678\":{\"schema:name\":[\"ABS permit\"],\"schema:validIn\":[\"marineregions:3293\"],\"schema:issuer\":[\"gen:8fefd86e6fd2acc15cd4db7a2ecff042dcde5d323d208c85e7434630d0342588\"],\"schema:validFrom\":[\"2021-01-01\"],\"schema:identifier\":[\"ircc2345678\"],\"schema:description\":[\"Samples of sea water from the Belgian EEZ collected over the period 2021-01-01 to 2022-01-01; processed for environmental DNA for taxonomic classification purposes\"],\"schema:validUntil\":[\"2022-01-01\"],\"prov:type\":[{\"type\":\"prov:QUALIFIED_NAME\",\"$\":\"schema:Permit\"},{\"type\":\"prov:QUALIFIED_NAME\",\"$\":\"schema:Thing\"},{\"type\":\"prov:QUALIFIED_NAME\",\"$\":\"schema:CreativeWork\"}]}},\"@id\":\"ex:connectors_bundle\"}}}"
  }
]
```


After you're done, to clean everything up (stop and remove all containers) run `docker-compose down -v`.

