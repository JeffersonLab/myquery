#!/bin/bash

FUNCTIONS=(create_user_and_group
           download
           install
           create_systemd_service
           create_log_file_cleanup_cron)

VARIABLES=(APP_GROUP
           APP_GROUP_ID
           APP_USER
           APP_NAME
           APP_USER_ID
           APP_VERSION
           APP_URL
           INSTALL_DIR)

if [[ $# -eq 0 ]] ; then
    echo "Usage: $0 [var file] <optional function>"
    echo "The var file arg should be the path to a file with bash variables that will be sourced."
    echo "The optional function name arg if provided is the sole function to call, else all functions are invoked sequentially."
    printf 'Variables: '
    printf '%s ' "${VARIABLES[@]}"
    printf '\n'
    printf 'Functions: '
    printf '%s ' "${FUNCTIONS[@]}"
    printf '\n'
    exit 0
fi

if [ ! -z "$1" ] && [ -f "$1" ]
then
echo "Loading environment $1"
. $1
fi

# Verify expected env set:
for i in "${!VARIABLES[@]}"; do
  var=${VARIABLES[$i]}
  [ -z "${!var}" ] && { echo "$var is not set. Exiting."; exit 1; }
done

APP_HOME=${INSTALL_DIR}/${APP_NAME}
APP_VERSIONED_HOME=${INSTALL_DIR}/apache-tomcat-${APP_VERSION}

create_user_and_group() {
groupadd -r -g ${APP_GROUP_ID} ${APP_GROUP}
useradd -r -m -u ${APP_USER_ID} -g ${APP_GROUP_ID} -d ${APP_HOME} -s /bin/bash ${APP_USER}
}

download() {
cd /tmp
wget ${APP_URL}
}

install() {
unzip /tmp/apache-tomcat-${APP_VERSION}.zip -d ${INSTALL_DIR}
ln -snf ${INSTALL_DIR}/apache-tomcat-${APP_VERSION} ${INSTALL_DIR}/tomcat
chown -R ${APP_USER}:${APP_GROUP} ${INSTALL_DIR}/apache-tomcat-${APP_VERSION}
chmod +x ${INSTALL_DIR}/tomcat/bin/*.sh
}

create_systemd_service() {
if (( ${APP_HTTPS_PORT} < 1024 ))
then
  sysctl -w net.ipv4.ip_unprivileged_port_start=${APP_HTTPS_PORT} >> /etc/sysctl.conf
fi

CLASSPATH=${APP_HOME}/bin/bootstrap.jar:${APP_HOME}/bin/tomcat-juli.jar

cat > /etc/systemd/system/tomcat.service << EOF
[Unit]
Description=Tomcat Application Server
After=syslog.target network.target
[Service]
Type=simple
User=${APP_USER}
Group=${APP_GROUP}
ExecStart=${JAVA_HOME}/bin/java \
 -classpath ${CLASSPATH} \
 -Xms64m \
 -Xmx1024m \
 -XX:MetaspaceSize=96M \
 -XX:MaxMetaspaceSize=512m \
 -Djava.net.preferIPv4Stack=true \
 -Dfile.encoding=UTF-8 \
 -Dcatalina.home=${APP_HOME} \
 -Dcatalina.base=${APP_HOME} \
 -Djava.io.tmpdir=${APP_HOME}/temp \
 -Djava.util.logging.config.file=${APP_HOME}/conf/logging.properties \
 -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager \
 -Djdk.tls.ephemeralDHKeySize=2048 \
 -Djava.protocol.handler.pkgs=org.apache.catalina.webresources \
 -Dorg.apache.catalina.security.SecurityListener.UMASK=0027 \
 --add-opens=java.base/java.lang=ALL-UNNAMED \
 --add-opens=java.base/java.io=ALL-UNNAMED \
 --add-opens=java.base/java.util=ALL-UNNAMED \
 --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED \
 org.apache.catalina.startup.Bootstrap \
 start
ExecStop=${JAVA_HOME}/bin/java \
 -classpath ${CLASSPATH} \
 -Dcatalina.home=${APP_HOME} \
 -Djava.util.logging.config.file=${APP_HOME}/conf/logging.properties \
 -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager \
 org.apache.catalina.startup.Bootstrap \
 stop
SuccessExitStatus=143
[Install]
WantedBy=multi-user.target
EOF
systemctl enable tomcat
systemctl start tomcat
}

create_log_file_cleanup_cron() {
cat > /root/delete-old-app-logs.sh << EOF
#!/bin/sh
if [ -d ${APP_HOME}/log ] ; then
 /usr/bin/find ${APP_HOME}/log/ -mtime +30 -exec /usr/bin/rm {} \;
fi
EOF
chmod +x /root/delete-old-app-logs.sh
cat > /etc/cron.d/delete-old-app-logs.cron << EOF
0 0 * * * /root/delete-old-app-logs.sh >/dev/null 2>&1
EOF
}

if [ ! -z "$2" ]
then
  echo "------------------------"
  echo "$2"
  echo "------------------------"
  $2
else
for i in "${!FUNCTIONS[@]}"; do
  echo "------------------------"
  echo "${FUNCTIONS[$i]}"
  echo "------------------------"
  ${FUNCTIONS[$i]};
done
fi