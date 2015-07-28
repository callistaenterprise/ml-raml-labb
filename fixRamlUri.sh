#!/bin/sh

sed -i.org 's/baseUri: https:\/\/localhost:8080\/api/baseUri: \/api/' src/main/resources/public/raml/az-ip-api.raml
rm src/main/resources/public/raml/az-ip-api.raml.org
