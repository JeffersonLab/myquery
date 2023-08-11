# myquery [![CI](https://github.com/JeffersonLab/myquery/actions/workflows/ci.yml/badge.svg)](https://github.com/JeffersonLab/myquery/actions/workflows/ci.yml) [![Docker](https://img.shields.io/docker/v/jeffersonlab/myquery?sort=semver&label=DockerHub)](https://hub.docker.com/r/jeffersonlab/myquery)
The myquery web service provides a simple query interface to the Jefferson Lab MYA archiver via [jmyapi](https://github.com/JeffersonLab/jmyapi). 

![Screenshot](https://github.com/JeffersonLab/myquery/raw/main/Screenshot.png?raw=true "Screenshot")

---
 - [Overview](https://github.com/JeffersonLab/myquery#overview)   
 - [Quick Start with Compose](https://github.com/JeffersonLab/myquery#quick-start-with-compose)    
 - [Install](https://github.com/JeffersonLab/myquery#install)   
 - [API](https://github.com/JeffersonLab/myquery#api)    
 - [Configure](https://github.com/JeffersonLab/myquery#configure)    
 - [Build](https://github.com/JeffersonLab/myquery#build)
 - [Develop](https://github.com/JeffersonLab/myquery#develop)
 - [Test](https://github.com/JeffersonLab/myquery#test)
 - [Release](https://github.com/JeffersonLab/myquery#release)
 - [Deploy](https://github.com/JeffersonLab/myquery#deploy)
 - [See Also](https://github.com/JeffersonLab/myquery#see-also)
---

## Overview
The primary goal of myquery is to allow users simple, programmatic access to MYA data without any dependencies on a specific language or requiring wrapping a command line tool.  The myquery server supports HTTP requests and returns JSON responses.

Supports querying channel history over a time interval and at a specific point in time, and includes simple forms that allow users to easily generate a valid query string for each type of supported query.

See: [Public MYA Web Service](https://epicsweb.jlab.org/myquery/) (auth required from offsite).   

## Quick Start with Compose 
1. Grab project
```
git clone https://github.com/JeffersonLab/myquery
cd myquery
```
2. Launch [Compose](https://github.com/docker/compose)
```
docker compose up
```
3. Use web browser to request channel list

http://localhost:8080/myquery/channel?q=channel%25&m=docker

## Install
 1. Download [Java JDK 17+](https://adoptium.net/)
 2. Download [Apache Tomcat 10+](http://tomcat.apache.org/) (Compiled against Jakarta EE)
 3. Download [MariaDB Driver](https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.0.8/mariadb-java-client-3.0.8.jar) and drop it into the Tomcat lib directory
 4. Download [myquery.war](https://github.com/JeffersonLab/myquery/releases) and drop it into the Tomcat webapps directory
 5. [Configure](https://github.com/JeffersonLab/myquery#configure) Tomcat
 6. Start Tomcat and navigate your web browser to localhost:8080/myquery

## API    

[API Reference](https://github.com/JeffersonLab/myquery/wiki/API-Reference)

## Configure
A [deployments.properites](https://github.com/JeffersonLab/jmyapi#deployments) file must be placed in the lib directory of Tomcat.  Download the mariadb [database driver](https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.0.8/mariadb-java-client-3.0.8.jar) and place it in the lib directory of Tomcat.  A new [context.xml](https://github.com/JeffersonLab/myquery/blob/main/docker/myquery/conf/context.xml) file is needed in the Tomcat conf directory that includes a DataSource for each host in the deployments.properties. 

### CORS
You can enable the Tomcat [CORS filter](https://tomcat.apache.org/tomcat-10.1-doc/config/filter.html#CORS_Filter) for cross origin support by setting the environment variable `CORS_ALLOWED_ORIGINS` to the orgins you'd like to support.  See [Docker Compose example](https://github.com/JeffersonLab/myquery/blob/main/cors-test.yml).

## Build
This project is built with [Java 17](https://adoptium.net/) (compiled to Java 17 bytecode), and uses the [Gradle 7](https://gradle.org/) build tool to automatically download dependencies and build the project from source:

```
git clone https://github.com/JeffersonLab/myquery
cd myquery
gradlew build
```
**Note**: If you do not already have Gradle installed, it will be installed automatically by the wrapper script included in the source

**Note for JLab On-Site Users**: Jefferson Lab has an intercepting [proxy](https://gist.github.com/slominskir/92c25a033db93a90184a5994e71d0b78)

## Develop
In order to iterate rapidly when making changes it's often useful to run the app directly on the local workstation, perhaps leveraging an IDE.  In this scenario run the service dependencies with:
```
docker compose -f deps.yml up
```
**Note**: The local install of Tomcat should be [configured](https://github.com/JeffersonLab/myquery#configure) to proxy connections to mya via localhost and therefore `deployments.properties` should contain:
```
port=3306
docker.master.host=mya
docker.hosts=mya
proxy.host.mya=localhost
```
Further, the local mya DataSource must also leverage localhost port forwarding so the `context.xml` URL field should be: `url="jdbc:mariadb://localhost:3306/archive"`.

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
2. Create a new release on the GitHub [Releases](https://github.com/JeffersonLab/myquery/releases) page corresponding to same version in build.gradle (Enumerate changes and link issues).  Attach war file for users to download.
3. Build and publish a new Docker image [from the GitHub tag](https://gist.github.com/slominskir/a7da801e8259f5974c978f9c3091d52c#8-build-an-image-based-of-github-tag).  GitHub is configured to do this automatically on git push of semver tag (typically part of GitHub release) or the [Publish to DockerHub](https://github.com/JeffersonLab/myquery/actions/workflows/docker-publish.yml) action can be manually triggered after selecting a tag.
4. Bump and commit quick start [image version](https://github.com/JeffersonLab/myquery/blob/main/docker-compose.override.yml)

## Deploy
At JLab this app is found at [epicsweb.jlab.org/myquery](https://epicsweb.jlab.org/myquery/) and internally at [epicswebtest.acc.jlab.org/myquery](https://epicswebtest.acc.jlab.org/myquery/).  However, those servers are proxies for `tomcat1.acc.jlab.org` and `tomcattest1.acc.jlab.org` respectively.   Use wget or the like to grab the release war file.  Don't download directly into webapps dir as file scanner may attempt to deploy before fully downloaded.  Be careful of previous war file as by default wget won't overrwite.  The war file should be attached to each release, so right click it and copy location (or just update version in path provided in the example below).  Example:

```
cd /tmp
rm myquery.war
wget https://github.com/JeffersonLab/myquery/releases/download/v1.2.3/myquery.war
mv  myquery.war /opt/tomcat/webapps
```

**JLab Internal Docs**:  [InstallGuideTomcatRHEL9](https://accwiki.acc.jlab.org/do/view/SysAdmin/InstallGuideTomcatRHEL9)

## See Also
   - [Web Archive Viewer and Expositor (WAVE)](https://github.com/JeffersonLab/wave)
   - [Setup Troubleshooting](https://github.com/JeffersonLab/myquery/wiki/Setup-Troubleshooting)
   - [Usage Examples](https://github.com/JeffersonLab/myquery/wiki/Usage-Examples)
