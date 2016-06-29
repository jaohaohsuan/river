FROM jaohaohsuan/jnlp-slave:latest
MAINTAINER Henry Jao
WORKDIR /home/jenkins
ADD . ./
RUN sbt 'clean' 'compile' 'release'