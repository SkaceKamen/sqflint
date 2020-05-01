# Dockerfile to build this thing
# Usage:
#   docker build -t sqflint_builder .
#   docker run -v ./:/opt/build sqflint_builder
# Artifacts will be stored in dist
FROM openjdk:8

# Installs Ant
ENV ANT_VERSION 1.10.6
RUN cd /opt && \
    wget -q https://downloads.apache.org/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xzf apache-ant-${ANT_VERSION}-bin.tar.gz && \
    mv apache-ant-${ANT_VERSION} /opt/ant && \
    rm apache-ant-${ANT_VERSION}-bin.tar.gz
ENV ANT_HOME /opt/ant
ENV PATH ${PATH}:/opt/ant/bin

ARG JAVACC_VERSION=7.0.5
ENV JAVACC_VERSION ${JAVACC_VERSION}

RUN cd /opt && \
    wget -q https://github.com/javacc/javacc/archive/${JAVACC_VERSION}.tar.gz && \
    tar -xzf ${JAVACC_VERSION}.tar.gz && \
    mv javacc-${JAVACC_VERSION} /opt/javacc && \
    rm ${JAVACC_VERSION}.tar.gz && \
    rm /opt/javacc/scripts/javacc.bat && \
    echo '#!/bin/sh' > /opt/javacc/scripts/javacc && \
    echo 'JAR=/opt/javacc/target/javacc.jar' >> /opt/javacc/scripts/javacc && \
    echo 'case "`uname`" in' >> /opt/javacc/scripts/javacc && \
    echo '  CYGWIN*) JAR="`cygpath --windows -- "$JAR"`" ;;' >> /opt/javacc/scripts/javacc && \
    echo 'esac' >> /opt/javacc/scripts/javacc && \
    echo 'java -classpath "$JAR" javacc "$@"' >> /opt/javacc/scripts/javacc && \
    chmod +x /opt/javacc/scripts/javacc && \
    cp /opt/javacc/scripts/javacc /opt/javacc/scripts/javacc.bat && \
    cd /opt/javacc && \
    ant

ENV PATH ${PATH}:/opt/javacc/scripts

RUN mkdir /opt/build && chmod 777 /opt/build
WORKDIR /opt/build

ENTRYPOINT [ "/opt/ant/bin/ant" ]


