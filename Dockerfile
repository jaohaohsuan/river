FROM jaohaohsuan/jnlp-slave:latest
MAINTAINER Henry Jao
COPY build.sbt ./
COPY project project/
COPY src src/
RUN chown -R jenkins:jenkins /home/jenkins  && \
    chown -R jenkins:jenkins /tmp/.ivy2  && \
    chown -R jenkins:jenkins /tmp/.sbt  && \
    ls -al /tmp
USER jenkins
RUN /usr/local/bin/sbt -v -sbt-dir /tmp/.sbt/0.13.11 -sbt-boot /tmp/.sbt/boot -ivy /tmp/.ivy2 -sbt-launch-dir /tmp/.sbt/launchers 'compile' && \
    alias sbt='/usr/local/bin/sbt -sbt-dir /tmp/.sbt/0.13.11 -sbt-boot /tmp/.sbt/boot -ivy /tmp/.ivy2 -sbt-launch-dir /tmp/.sbt/launchers'
VOLUME /home/jenkins
ENTRYPOINT ["jenkins-slave"]
