#!/bin/sh

cd target/classes

sed -i.org 's/baseUri: https:\/\/localhost:8080\/raml-api/baseUri: \/raml-api/' public/raml/az-ip-api.raml
zip ../az-ip-api-server-1.0.0-SNAPSHOT.jar public/raml/az-ip-api.raml

cd -
