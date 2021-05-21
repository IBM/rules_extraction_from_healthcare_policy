#!/bin/bash

wait-for-url() {
    echo "Testing $1"
    timeout -s TERM 45 bash -c \
    'while [[ "$(curl -s -o /dev/null -L -w ''%{http_code}'' ${0})" != "404" ]];\
    do echo "Waiting for ${0}" && sleep 2;\
    done' ${1}
    echo "OK!"
    curl -I $1
}

echo "Waiting for Rdf4j"
wait-for-url http://localhost:8080
echo "Rdf4j started"

echo "Creating new repository rules-extraction-from-healthcare-policy..."
curl -X PUT "http://localhost:8080/rdf4j-server/repositories/rules-extraction-from-healthcare-policy" \
  -H "Content-type: text/turtle" \
  --data-binary @rdf4j-repo-definition.rdf
echo "Populating new repository spmRepo from ./populated-ontology.ttl ..."
curl -X PUT -H "Content-type: text/turtle" --data-binary @./populated-ontology.ttl http://localhost:8080/rdf4j-server/repositories/rules-extraction-from-healthcare-policy/statements