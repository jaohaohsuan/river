#!/bin/bash

if [ "$(kubectl describe rc river | grep 'Image(s)' | awk '{print $2}')" != "127.0.0.1:5000/inu/river:${version}" ]; then
    kubectl rolling-update river --image=127.0.0.1:5000/inu/river:${version}
fi