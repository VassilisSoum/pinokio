#!/bin/bash
awslocal dynamodb create-table \
   --table-name pinokio \
   --attribute-definitions AttributeName=UrlHash,AttributeType=S \
   --key-schema AttributeName=UrlHash,KeyType=HASH \
   --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

echo "Executed init-dynamodb.sh"