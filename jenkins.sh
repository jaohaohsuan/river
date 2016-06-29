#!/bin/bash

# make storedq-api images
sbt 'clean' 'compile' 'test' 'docker:publish'

chmod +x target/deployment/up.sh
./target/deployment/up.sh
