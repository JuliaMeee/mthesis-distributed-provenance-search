# mthesis-distributed-provenance-search

<h2>Project structure:</h2>

`/bundle-search`: implementation of <b>bundle-searcher</b>.   
`/distributed-provenance-search`: implementation of <b>prov-traverser</b>.   
`/prov_storage`: implementation of provenance storage and trusted party from https://gitlab.fi.muni.cz/xmojzis1/provenance-system-tests.  
`/setup`: has sripts and data for demo setup.  

<h2>Demo setup:</h2>

Download the repo files or clone the repo.'/

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

Run the following command from directory `./setup` to register organizations and upload provenance files for the demo (if you get `curl: (52) Empty reply from server`, wait a few seconds and try again)
```sh
./simulation_setup.sh
```


<h2>Services</h2>

<h3>prov-traverser</h3> 

- Service that traverses the provenance chain in the given direction. It fetches results of a given query for individual bundles in the chain from their respective provenance controllers (bundle-searcher containers).
- OpenAPI specification\*: 
`http://localhost:8090/swagger-ui/index.html#`

<h3>bundle-searcher</h3>  

- Service that represents a provenance controller and can answer queries about individual bundles in storage of this provenance controller.  
- OpenAPI specification\*: `http://localhost:8081/swagger-ui/index.html#` 

Checkout the <b>prov-traverser</b> API to list available <i>validity checks</i> and <i>traversal priorities</i>.  
Checkout the <b>bundle-searcher</b> API to list  available <i>version preferences</i> and <i>query types</i> along with their desriptions.  

Both OpenAPI specifications contains multiple executable query examples to test the demo.

You can also run requests to the prov-traverser and bundle-searcher containers from the <b>debug-shell</b> container.

<i>-- Note that the service containers must be running for the OpenAPI specification to load.  
-- It might take up to a minute for the services and their OpenAPI specifications to load after (re)starting the containers. If you get `Failed to connect to prov-traverser port 8000 after X ms: Connection refused`, wait a few seconds and try again.</i>

<h2>Demo data</h2>

The demo data can be found in `./setup/data/` folder. Below is a schema of traversal information of the bundles, excluding sender and reciever agent nodes (to save space). Main activies are depicted as rectangles, and connectors are depicted as ovals. connectors to the left of the main activity are backwad connectors. Connectors to the right of the main acitivyt are forward connectors.

![Demo bundles TI scheme](./setup/data/demo-data-backbone.drawio.svg)

 <h2>Technical issues with storage service and java serialization</h2>
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
- Issue: Trusted party enpoint for retrieving tokens of a specific bundle always results in an error.  
Fix: Use endpoint for retrieving all tokens under an organization instead.