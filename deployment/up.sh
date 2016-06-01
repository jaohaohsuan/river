#!/bin/bash

kubectl apply -f target/deployment/manifests
IMG1="$(kubectl describe rc river | grep 'Image(s)' | awk '{print $2}')"
IMG2="127.0.0.1:5000/inu/river:${version}"
if [ "${IMG1}" != "${IMG2}" ]; then
    kubectl rolling-update river --image=${IMG2}
fi