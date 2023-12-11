#!/usr/csite/pubtools/bin/python3.11

import argparse
import os
import sys
import re
from typing import Dict, List, Tuple
from datetime import datetime


SCRIPT_NAME = os.path.basename(__file__)
DEPLOYMENT_FILENAME = "deployments.properties"
CONTEXT_FILENAME = "context.xml"


def parse_configs(config_dir: str = "/cs/certified/config/mya"):
    """Parse the config files in the certified mya area."""

    comment_pattern = r"^\s*#"
    blank_line_pattern = r"^\s*$"
    host_pattern = r"^\s*MyaHost = { name = ([\w\-\.]+) role = (.+)(?:utilization.*)}"
    host_pattern = r"^\s*MyaHost = { name = ([\w\-\.]+) role = (.+?) (?:utilization = .*)?}"

    deployments = {}

    for file in os.listdir(config_dir):
        deployments[file] = {}
        with open(f"{config_dir}/{file}", "r") as f:
            for line in f:
                if re.match(comment_pattern, line):
                    continue
                if re.match(blank_line_pattern, line):
                    continue

                m = re.match(host_pattern, line)
                if m is not None:
                    host = m.group(1)
                    roles = m.group(2).split(" ")
                    if len(roles) > 1:
                        roles = [role for role in roles if not role in ('{', '}') ]

                    deployments[file][host] = roles

    return deployments


def create_deployment_properties(deployments: Dict[str, Dict[str, List[str]]]):
    prop_string = "port=3306\n"
    for dep in deployments.keys():
        if dep.startswith("_"):
            continue
        hosts = []
        for host in deployments[dep].keys():
            hosts.append(host)
            if "master" in deployments[dep][host]:
                prop_string += f"{dep}.master.host={host}\n"
        prop_string += f"{dep}.hosts={','.join(hosts)}\n"

    return prop_string


def create_context_string(deployments: Dict[str, Dict[str, List[str]]],
                       username: str, password: str):
    """Write the context.xml needed for Tomcat server datasources"""
    file_footer = "</Context>\n"
    file_header = """<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!-- The contents of this file will be loaded for each web application -->
<Context>

    <!-- Default set of monitored resources. If one of these changes, the    -->
    <!-- web application will be reloaded.                                   -->
    <WatchedResource>WEB-INF/web.xml</WatchedResource>
    <WatchedResource>WEB-INF/tomcat-web.xml</WatchedResource>
    <WatchedResource>${catalina.base}/conf/web.xml</WatchedResource>

    <!-- Uncomment this to enable session persistence across Tomcat restarts -->
    <!--
    <Manager pathname="SESSIONS.ser" />
    -->
"""

    resource_template = """
  <Resource name="jdbc/{host}"
            type="javax.sql.DataSource"
            username="{username}"
            password="{password}"
            driverClassName="org.mariadb.jdbc.Driver"
            url="jdbc:mariadb://{host}:3306/archive"
            maxTotal="4"
            minIdle="0"
            maxIdle="2"
            testOnBorrow="true"
            validationQuery="select 1 from dual"
            connectionProperties="useCompression=true;noAccessToProcedureBodies=true"
            closeMethod="close"/>
"""


    out_string = file_header
    for dep in deployments.keys():
        if dep.startswith("_"):
            continue
        for host in deployments[dep].keys():
            out_string += resource_template.format(username=username,
                                                    password=password,
                                                    host=host)
    out_string += file_footer
    return out_string


def file_contents_match(generated: str, filename: str) -> bool:
    """Compare generated file contents to existing file.
    
    Args:
        generated: The newly generated file contents
        filename: Path to the existing config file
    """

    if not os.path.isfile(filename):
        return False

    with open(filename, "r") as f:
        contents = f.read()

    return generated == contents


def update_files(deployment_file: str, context_file: str, properties: str,
                 context: str) -> bool:

    now = datetime.now().strftime("%Y-%m-%d_%H%M%S")
    success = False
    try:
        if os.path.exists(deployment_file):
            os.rename(deployment_file, f"{deployment_file}--{now}")

        if os.path.exists(context_file):
            os.rename(context_file, f"{context_file}--{now}")

        with open(deployment_file, "w") as f:
            f.write(properties)

        with open(context_file, "w") as f:
            f.write(context)

        success = True
    except Exception as exc:
        print(f"Error: {repr(exc)}")

    return success


def deploy_files(remote: str, src_prop: str, src_context: str, dst_prop: str,
                 dst_context: str):
    """Copy the config files to the destination host
    
    Args:
        remote: The hostname of the destination server
        src_prop: Source file for deployment.properties
        src_context: Source file for context.xml
        dst_prop: Where to write the deployment.properties file
        dst_context: Where to write the context.xml file
    """

    os.system(f"scp {src_prop} {remote}:{dst_prop}")
    os.system(f"scp {src_context} {remote}:{dst_context}")


def get_secrets(filename: str) -> Tuple[str, str]:
    """Read username/password from the provided file."""
    with open(filename, "r") as f:
        username = f.readline().strip()
        password = f.readline().strip()

    return username, password


def main():
    """Parse certified mya config and push out updated configs for myquery.
    
    Only do anything if the resulting config would be different.
    """

    parser = argparse.ArgumentParser(description="Keep myquery deployments in sync with certified config.")
    parser.add_argument("-d", "--dir", type=str, required=True, help="Existing configuration directory")
    parser.add_argument("-r", "--remote", type=str, required=True, help="Remote host where configs are deployed")
    parser.add_argument("-f", "--secrets-file", type=str, required=True,
                        help="File containg username on first line, password on second")

    args = parser.parse_args()
    config_dir = args.dir
    remote = args.remote
    secrets_file = args.secrets_file

    if not os.path.isfile(secrets_file):
        print(f"file not found: {secrets_file}")
        exit(1)


    if not os.path.isdir(config_dir):
        print(f"directory not found: {config_dir}")
        exit(1)

    prop_src = f"{config_dir}/{DEPLOYMENT_FILENAME}"
    context_src = f"{config_dir}/{CONTEXT_FILENAME}"
    prop_dst = f"/opt/tomcat/lib/{DEPLOYMENT_FILENAME}"
    context_dst = f"/opt/tomcat/conf/{CONTEXT_FILENAME}"

    username, password = get_secrets(secrets_file)
    deployments = parse_configs()
    properties = create_deployment_properties(deployments)
    context = create_context_string(deployments, username, password)

    if not file_contents_match(properties, prop_src) or not file_contents_match(context, context_src):
        print(f"{SCRIPT_NAME} found MYA deployments configuration has changed.  Updating web services.")
        success = update_files(prop_src, context_src, properties, context)

        if success:
            deploy_files(remote=remote, src_prop=prop_src, src_context=context_src, dst_prop=prop_dst,
                         dst_context=context_dst)

if __name__ == "__main__":
    main()
