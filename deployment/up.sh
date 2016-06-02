#!/bin/bash
set -x

kubectl get svc river --no-headers
rc=$?
if (( rc != 0 )); then kubectl apply -f target/deployment/manifests/svc.yml; fi

kubectl get rc river --no-headers
rc=$?
if (( rc != 0 )); then kubectl apply -f target/deployment/manifests/rc.yml; fi

kubectl rolling-update river --image=127.0.0.1:5000/inu/river:${version}



