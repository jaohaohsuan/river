#!/bin/bash
set -x

kubectl get svc river --no-headers
rc=$?
if (( rc != 0 )); then kubectl apply -f target/deployment/manifests/svc.yml; fi

kubectl apply -f target/deployment/manifests/deploy.yml --record