#!/bin/bash

FUNCTIONS=(create_user_and_group
           download
           unzip_and_chmod
           create_symbolic_links
           create_systemd_service
           create_log_file_cleanup_cron)

VARIABLES=(APP_GROUP
           APP_GROUP_ID
           APP_USER
           APP_USER_HOME
           APP_USER_ID
           APP_VERSION
           APP_URL)

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

APP_HOME=${APP_USER_HOME}/${APP_VERSION}

create_user_and_group() {
groupadd -r -g ${APP_GROUP_ID} ${APP_GROUP}
useradd -r -m -u ${APP_USER_ID} -g ${APP_GROUP_ID} -d ${APP_USER_HOME} -s /bin/bash ${APP_USER}
}

download() {
cd /tmp
wget ${APP_URL}
}

unzip_and_chmod() {
unzip /tmp/apache-tomcat-${APP_VERSION}.zip -d ${APP_USER_HOME}
mv ${APP_USER_HOME}/apache-tomcat-${APP_VERSION} ${APP_HOME}
chown -R ${APP_USER}:${APP_GROUP} ${APP_USER_HOME}
chmod +x ${APP_HOME}/bin/*.sh
}

create_symbolic_links() {
cd ${APP_USER_HOME}
ln -s ${APP_VERSION} current
ln -s current/conf conf
ln -s current/logs logs
ln -s current/bin bin
}

create_systemd_service() {
if (( ${APP_HTTPS_PORT} < 1024 ))
then
  sysctl -w net.ipv4.ip_unprivileged_port_start=${WILDFLY_HTTPS_PORT} >> /etc/sysctl.conf
fi

mv /opt/tomcat/run.env ${APP_HOME}/conf/run.env

cat > /etc/systemd/system/app.service << EOF
[Unit]
Description=Application Server
After=syslog.target network.target
[Service]
Type=forking
EnvironmentFile=${APP_HOME}/conf/run.env
User=${APP_USER}
Group=${APP_GROUP}
PIDFile=/run/tomcat.pid
ExecStart=/${APP_HOME}/bin/startup.sh
ExecStop=/${APP_HOME}/bin/shutdown.sh
[Install]
WantedBy=multi-user.target
EOF
systemctl enable app
systemctl start app
}

create_log_file_cleanup_cron() {
cat > /root/delete-old-app-logs.sh << EOF
#!/bin/sh
if [ -d ${APP_USER_HOME}/log ] ; then
 /usr/bin/find ${APP_USER_HOME}/log/ -mtime +30 -exec /usr/bin/rm {} \;
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