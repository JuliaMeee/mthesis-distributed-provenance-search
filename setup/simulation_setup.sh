#!/bin/bash

./register_org.sh prov-storage-1 ORG1 ./certificates/ORG1.pem
# echo "Registered ORG1 with prov-storage-1"
./register_org.sh prov-storage-2 ORG2 ./certificates/ORG2.pem
# echo "Registered ORG2 with prov-storage-2"
./register_org.sh prov-storage-3 ORG3 ./certificates/ORG3.pem
# echo "Registered ORG3 with prov-storage-3"


./store_doc.sh prov-storage-1 ORG1 ./certificates/ORG1.key ./data/dataset1/SamplingBundle_V0.json
echo
# echo "Saved SamplingBundle_V0.json to prov-storage-1 under ORG1"
./store_doc.sh prov-storage-2 ORG2 ./certificates/ORG2.key ./data/dataset2/ProcessingBundle_V0.json
echo
# echo "Saved ProcessingBundle_V0.json to prov-storage-2 under ORG2"
./store_doc.sh prov-storage-3 ORG3 ./certificates/ORG3.key ./data/dataset3/SpeciesIdentificationBundle_V0.json
echo
# echo "Saved SpeciesIdentificationBundle_V0.json to prov-storage-3 under ORG3"
./store_doc.sh prov-storage-1 ORG1 ./certificates/ORG1.key ./data/dataset4/DnaSequencingBundle_V0.json
echo
# echo "Saved DnaSequencingBundle_V0.json to prov-storage-1 under ORG1"


./store_doc.sh prov-storage-1 ORG1 ./certificates/ORG1.key ./data/dataset1/SamplingBundle_V1.json
echo
# echo "Saved SamplingBundle_V1.json to prov-storage-1 under ORG1"
./store_doc.sh prov-storage-2 ORG2 ./certificates/ORG2.key ./data/dataset2/ProcessingBundle_V1.json
echo
# echo "Saved ProcessingBundle_V1.json to prov-storage-2 under ORG2"
