# mthesis-distributed-provenance-search

<h2>Setup:</h2>

Build the docker images from working directory `/prov_storage/distributed-provenance-system/` with
`docker build -f Dockerfile.TrustedParty -t trusted_party .`
`docker build -f Dockerfile.ProvStorage -t distributed_prov_system .`

Start docker and run `docker-compose up -d`.

From `/setup` run:
`./simulation_setup.sh`
to register organizations and upload provenance files.

To clean everything up (stop and remove all containers) run `docker-compose down -v`

