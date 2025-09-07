#!/bin/bash

#register organizations
echo "--Register ORG1"
./register_org.sh prov-storage-1 ORG1 ./certificates/ORG1.pem

echo "--Register ORG2"
./register_org.sh prov-storage-2 ORG2 ./certificates/ORG2.pem

echo "--Register ORG3"
./register_org.sh prov-storage-3 ORG3 ./certificates/ORG3.pem


# Store documents
echo "--Store document SamplingBundle_V0.json"
./store_doc.sh prov-storage-1 ORG1 ./certificates/ORG1.key ./data/dataset1/SamplingBundle_V0.json
echo

echo "--Store document ProcessingBundle_V0.json"
./store_doc.sh prov-storage-2 ORG2 ./certificates/ORG2.key ./data/dataset2/ProcessingBundle_V0.json
echo

echo "--Store document SpeciesIdentificationBundle_V0.json"
./store_doc.sh prov-storage-3 ORG3 ./certificates/ORG3.key ./data/dataset3/SpeciesIdentificationBundle_V0.json
echo

echo "--Store document DnaSequencingBundle_V0.json"
./store_doc.sh prov-storage-1 ORG1 ./certificates/ORG1.key ./data/dataset4/DnaSequencingBundle_V0.json
echo


echo "--Store document SamplingBundle_V1.json"
./update_doc.sh prov-storage-1 ORG1 ./certificates/ORG1.key ./data/dataset1/SamplingBundle_V1.json SamplingBundle_V0
echo

echo "--Store document ProcessingBundle_V1.json"
./update_doc.sh prov-storage-2 ORG2 ./certificates/ORG2.key ./data/dataset2/ProcessingBundle_V1.json ProcessingBundle_V0
echo
