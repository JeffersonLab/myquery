ARG BUILD_IMAGE=gradle:7.4-jdk17-alpine
ARG RUN_IMAGE=tomcat:10.1.4-jre17

################## Stage 0
FROM ${BUILD_IMAGE} as builder
ARG CUSTOM_CRT_URL
USER root
WORKDIR /
RUN if [ -z "${CUSTOM_CRT_URL}" ] ; then echo "No custom cert needed"; else \
       wget -O /usr/local/share/ca-certificates/customcert.crt $CUSTOM_CRT_URL \
       && update-ca-certificates \
       && keytool -import -alias custom -file /usr/local/share/ca-certificates/customcert.crt -cacerts -storepass changeit -noprompt \
       && export OPTIONAL_CERT_ARG=--cert=/etc/ssl/certs/ca-certificates.crt \
    ; fi
COPY . /app
RUN cd /app && gradle build -x test --no-watch-fs $OPTIONAL_CERT_ARG
RUN cd /app && wget https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.0.8/mariadb-java-client-3.0.8.jar

################## Stage 1
FROM ${RUN_IMAGE} as runner
ARG CUSTOM_CRT_URL
ARG RUN_USER=tomcat
ARG DEPLOYMENTS=/usr/local/tomcat/webapps
USER root
COPY --from=builder /app/build/libs /usr/local/tomcat/webapps
COPY --from=builder /app/docker/myquery/lib /usr/local/tomcat/lib
COPY --from=builder /app/docker/myquery/conf /usr/local/tomcat/conf
COPY --from=builder /app/mariadb-java-client-3.0.8.jar /usr/local/tomcat/lib
RUN useradd -m tomcat \
    && if [ -z "${CUSTOM_CRT_URL}" ] ; then echo "No custom cert needed"; else \
       mkdir -p /usr/local/share/ca-certificates \
       && curl -o /usr/local/share/ca-certificates/customcert.crt $CUSTOM_CRT_URL \
       && update-ca-certificates \
    ; fi \
    && mkdir /usr/local/tomcat/conf/Catalina \
    && chown -R ${RUN_USER}:tomcat /usr/local/tomcat/conf/Catalina \
    && chown -R ${RUN_USER}:tomcat ${DEPLOYMENTS} \
    && chmod -R g+rw ${DEPLOYMENTS}
USER ${RUN_USER}
ENV TZ='America/New_York'