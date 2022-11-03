# myquery [![CI](https://github.com/JeffersonLab/myquery/actions/workflows/ci.yml/badge.svg)](https://github.com/JeffersonLab/myquery/actions/workflows/ci.yml) [![Docker](https://img.shields.io/docker/v/jeffersonlab/myquery?sort=semver&label=DockerHub)](https://hub.docker.com/r/jeffersonlab/myquery)
The myquery web service provides a simple query interface to the Jefferson Lab MYA archiver via [jmyapi](https://github.com/JeffersonLab/jmyapi). 

---
 - [Overview](https://github.com/JeffersonLab/myquery#overview)   
 - [Quick Start with Compose](https://github.com/JeffersonLab/myquery#quick-start-with-compose)    
 - [Install](https://github.com/JeffersonLab/myquery#install)   
 - [API](https://github.com/JeffersonLab/myquery#api)    
 - [Configure](https://github.com/JeffersonLab/myquery#configure)    
 - [Build](https://github.com/JeffersonLab/myquery#build)
 - [Test](https://github.com/JeffersonLab/myquery#test)
 - [Release](https://github.com/JeffersonLab/myquery#release)
 - [See Also](https://github.com/JeffersonLab/myquery#see-also)
---

## Overview
The primary goal of myquery is to allow users simple, programmatic access to MYA data without any dependencies on a specific language or requiring wrapping a command line tool.

Supports querying channel history over a time interval and at a specific point in time, and includes simple forms that allow users to easily generate a valid query string for each type of supported query.

Access via Internet (Authentication Required): [Public MYA Web Service](https://epicsweb.jlab.org/myquery/)   
Access via Intranet: [Internal MYA Web Service](https://myaweb.acc.jlab.org/myquery/)

## Quick Start with Compose 
1. Grab project
```
git clone https://github.com/JeffersonLab/myquery
cd myquery
```
2. Launch Docker
```
docker compose up
```
3. Use web browser to request channel list

http://localhost:8080/myquery/channel?q=channel%25&m=docker

## Install
 1. Download [Apache Tomcat](http://tomcat.apache.org/)
 2. Download [myquery.war](https://github.com/JeffersonLab/myquery/releases) and drop it into the Tomcat webapps directory
 3. [Configure](https://github.com/JeffersonLab/myquery#configure) Tomcat
 4. Start Tomcat and navigate your web browser to localhost:8080/myquery

## API    

[API Reference](https://github.com/JeffersonLab/myquery/wiki/API-Reference)

## Configure
A [deployments.properites](https://github.com/JeffersonLab/jmyapi#deployments) file must be placed in the lib directory of Tomcat.  Download the mariadb [database driver](https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.0.8/mariadb-java-client-3.0.8.jar) and place it in the lib directory of Tomcat.  A new context.xml file is needed in the Tomcat conf directory that includes a DataSource for each host in the deployments.properties. 

## Build
This project is built with [Java 17](https://adoptium.net/) (compiled to Java 11 bytecode), and uses the [Gradle 7](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/myquery
cd myquery
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)


## Test
Continuous Integration (CI) is setup using GitHub Actions, so on push tests are automatically run unless `[no ci]` is included in the commit message.   Tests can be manually run on a local workstation using:
```
docker compose -f build.yml up
```
Wait for containers to start then:
```
gradlew integrationTest
```

## Release
1. Bump the version number and release date in build.gradle and commit and push to GitHub (using [Semantic Versioning](https://semver.org/)).   
2. Create a new release on the GitHub [Releases](https://github.com/JeffersonLab/jaws-libj/releases) page corresponding to same version in build.gradle (Enumerate changes and link issues).  Attach war file for users to download.
3. Build and publish a new Docker image [from the GitHub tag](https://gist.github.com/slominskir/a7da801e8259f5974c978f9c3091d52c#8-build-an-image-based-of-github-tag).
4. Bump and commit quick start [image version](https://github.com/JeffersonLab/myquery/blob/main/docker-compose.override.yml)

## See Also
   - [Web Archive Viewer and Expositor (WAVE)](https://github.com/JeffersonLab/wave)
   - [Setup Troubleshooting](https://github.com/JeffersonLab/myquery/wiki/Setup-Troubleshooting)
   - [Usage Examples](https://github.com/JeffersonLab/myquery/wiki/Usage-Examples)
