# Prerequisites

- KAFKA
- ATLAS

# Getting Started

tar xzf nifi-assembly/target/nifi-*-bin.tar.gz -C ../.

export JAVA_HOME=`/usr/libexec/java_home`

cd /Volumes/MyPassport/BespokeBackups/Business/nifi/nifi-master/nifi-1.4.0-SNAPSHOT

./bin/nifi.sh start

# Configure debugging (JRebel, logback, Remote Debug)

Change the following files under conf:
- bootstrap.conf
- logback.xml
- atlas-application.properties

cp bootstrap.conf /Volumes/MyPassport/BespokeBackups/Business/nifi/nifi-master/nifi-1.4.0-SNAPSHOT/conf/.
cp atlas-application.properties /Volumes/MyPassport/BespokeBackups/Business/nifi/nifi-master/nifi-1.4.0-SNAPSHOT/conf/.


## JRebel

Extract the following jrebel library and put it in the same location at the -agentpath setting below:

    curl -O http://dl.zeroturnaround.com/jrebel-stable-nosetup.zip
    unzip jrebel-stable-nosetup.zip

Added the following to the bootstrap.conf:

    java.arg.debug=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5010

    java.arg.16=-agentpath:/Users/aidancope/Library/Application Support/IdeaIC2017.2/jr-ide-idea/lib/jrebel6/lib/libjrebel64.dylib
    java.arg.17=-Drebel.remoting_plugin=true
    java.arg.18=-Drebel.remoting_port=5011

## Logging

Add to the conf/logback.xml:

        <appender name="ATLAS_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>${org.apache.nifi.bootstrap.config.log.dir}/nifi-atlas.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <!--
                  For daily rollover, use 'user_%d.log'.
                  For hourly rollover, use 'user_%d{yyyy-MM-dd_HH}.log'.
                  To GZIP rolled files, replace '.log' with '.log.gz'.
                  To ZIP rolled files, replace '.log' with '.log.zip'.
                -->
                <fileNamePattern>${org.apache.nifi.bootstrap.config.log.dir}/nifi-atlas_%d.log</fileNamePattern>
                <!-- keep 30 log files worth of history -->
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
                <pattern>%date %level [%thread] %logger{40} %msg%n</pattern>
            </encoder>
        </appender>



        <logger name="org.apache.nifi.atlas" level="DEBUG" additivity="false">
            <appender-ref ref="ATLAS_FILE"/>
       </logger>

        <logger name="org.apache.atlas" level="DEBUG" additivity="false">
            <appender-ref ref="ATLAS_FILE"/>
       </logger>



# Build new version (ATLAS and HIVE)

cd /Volumes/MyPassport/BespokeBackups/Business/nifi/nifi/nifi/nifi-nar-bundles/nifi-atlas-bundle

mvn install

cd /Volumes/MyPassport/BespokeBackups/Business/nifi/nifi/nifi/nifi-nar-bundles/nifi-hive-bundle

# Upgrade NAR (ATLAS and HIVE)

Copy the newly created NAR to the NIFI lib folder.

cp /Volumes/MyPassport/BespokeBackups/Business/nifi/nifi/nifi/nifi-nar-bundles/nifi-atlas-bundle/nifi-atlas-nar/target/nifi-atlas-nar-1.4.0-SNAPSHOT.nar lib/.

cp /Volumes/MyPassport/BespokeBackups/Business/nifi/nifi/nifi/nifi-nar-bundles/nifi-hive-bundle/nifi-hive-nar/target/nifi-hive-nar-1.4.0-SNAPSHOT.nar lib/.
 
# Testing

vi /Volumes/MyPassport/BespokeBackups/Business/nifi/landing/file_1
vi /Volumes/MyPassport/BespokeBackups/Business/nifi/landing/file_2
vi /Volumes/MyPassport/BespokeBackups/Business/nifi/landing/file_3

cp /Volumes/MyPassport/BespokeBackups/Business/nifi/file_12.csv /Volumes/MyPassport/BespokeBackups/Business/nifi/landing/.


# Atlas Lineage Task Setup


# Enabling Atlas Reporting

Go to Controller Settings, and "Reporting Tasks".

Confgure:
- Atlas URL: http://174.138.41.167:21000
- Username: admin
- Password: admin
- NIFI URL for Atlas: http://site-a.nifi
- Local hostname: 174.138.41.167
- API Port: 5580



# Configuring Atlas For Kafka Notifications

https://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.5.6/bk_command-line-installation/content/configuring_atlas_for_kafka_notifications.html

Modify conf/atlas-application.properties to include the KAFKA configuration


# HIVE Setup

(export HIVE_HOME=/opt/hive-2.3.0 && su - hive -c "export HADOOP_HOME=/opt/hadoop-2.8.1 && $HIVE_HOME/bin/hive")


export HIVE_HOME=/opt/hive-1.2.2
export HADOOP_HOME=/opt/hadoop-2.8.1

$HIVE_HOME/bin/beeline

!connect jdbc:hive2://localhost:10000 hive hive


create database DW;

use DW;

create table CUSTOMERS (name String, age Int) clustered by (age) into 5 buckets stored as orc tblproperties ("transactional"="true");



# Setting up permissions on HADOOP for HIVE

groupadd demo
useradd aidancope
usermod -a -G demo aidancope
usermod -a -G demo hive

su - hadoop -c "(export JAVA_HOME=/usr/java/latest && cd /opt/hadoop-2.8.1 && ./bin/hdfs dfs -chown -R hive:demo /user/hive/warehouse)"
su - hadoop -c "(export JAVA_HOME=/usr/java/latest && cd /opt/hadoop-2.8.1 && ./bin/hdfs dfs -chmod -R 775 /user/hive/warehouse)"


# Importing HIVE metadata into ATLAS

cp /opt/atlas-0.8.1/conf/atlas-application.properties /opt/hive-1.2.2/conf/.

su - atlas

cd /opt/atlas-0.8.1

export HADOOP_HOME=/opt/hadoop-2.8.1
export HIVE_HOME=/opt/hive-1.2.2
export HIVE_CONF=/opt/hive-1.2.2/conf

 
bin/import-hive.sh


# Reset the Atlas data

- stop atlas
- go to hbase shell
- disable 'apache_atlas_titan'
- drop 'apache_atlas_titan'
- exit
- start atlas
- import the hive table metadata)
- restart nifi

Start Atlas:
su - atlas -c "(export HBASE_CONF_DIR=/opt/atlas-0.8.1/conf/hbase && cd /opt/atlas-0.8.1 && rm logs/* && bin/atlas_start.py -port 21000)"

(export HBASE_CONF_DIR=/opt/atlas-0.8.1/conf/hbase && cd /opt/atlas-0.8.1 && rm -f logs/* && bin/atlas_start.py -port 21000)

Stop Atlas:
su - atlas -c "(export HBASE_CONF_DIR=/opt/atlas-0.8.1/conf/hbase && cd /opt/atlas-0.8.1 && bin/atlas_stop.py -port 21000)"

(export HBASE_CONF_DIR=/opt/atlas-0.8.1/conf/hbase && cd /opt/atlas-0.8.1 && bin/atlas_stop.py -port 21000)


su - atlas -c "(export HBASE_CONF_DIR=/opt/atlas-0.8.1/conf/hbase && cd /opt/atlas-0.8.1 && bin/atlas_stop.py -port 21000)"
su - hbase
cd /opt/hbase-1.3.1
bin/hbase shell
disable 'apache_atlas_titan'
drop 'apache_atlas_titan'
disable 'apache_atlas_entity_audit'
drop 'apache_atlas_entity_audit'
exit
exit
su - atlas -c "(export HBASE_CONF_DIR=/opt/atlas-0.8.1/conf/hbase && cd /opt/atlas-0.8.1 && rm logs/* && bin/atlas_start.py -port 21000)"



# Creating a custom docker image

export DOCKER_ID_USER="canzea"
docker login

docker tag my_image $DOCKER_ID_USER/my_image

docker push $DOCKER_ID_USER/my_image

(cd /opt && git clone https://github.com/apache/nifi.git)

git checkout tags/rel/nifi-1.4.0 -b nifi-1.4.0-atlas-enhancement



---
cd ~/docker-compose-nifi-cluster

docker build --label nifi-1.4.0-databc --tag nifi:1.4.0-databc .

docker push $DOCKER_ID_USER/nifi:1.4.0-databc



# Running the docker cluster

docker-compose up --scale nifi-nodes=2
docker-compose up --scale nifi-nodes=2 -d

docker-compose -f docker-compose-site-b.yml up

docker-compose -p nificluster_a down

docker-compose -f docker-compose-site-a.yml -p nificluster_a down

docker-compose -f docker-compose-site-b.yml -p nificluster_b down

docker-compose -f docker-compose-site-a.yml -p nificluster_a build

docker-compose -f docker-compose-site-a.yml -p nificluster_a up
docker-compose -p nificluster_b up -d
http://174.138.41.167:6580/nifi/

docker exec nifi-cluster-seed cat logs/nifi-app.log


docker-compose exec --index=1 nifi-nodes bash

docker-compose exec --index=1 nifi-nodes cat /opt/nifi/nifi-1.4.0/conf/nifi.properties
docker-compose exec --index=1 nifi-nodes cat /opt/nifi/nifi-1.4.0/logs/nifi-atlas.log

docker-compose exec nifi-node-1 cat /opt/nifi/nifi-1.4.0/logs/nifi-atlas.log
docker-compose exec nifi-node-2 cat /opt/nifi/nifi-1.4.0/logs/nifi-atlas.log


docker-compose -p nificluster_a down

docker-compose -f docker-compose-site-a.yml -p nificluster_a exec nifi-node-1 bash
docker-compose -p nificluster_a exec nifi-node-1 tail -f /opt/nifi/nifi-1.4.0/logs/nifi-atlas.log

docker-compose -f docker-compose-site-b.yml -p nificluster_b exec nifi-node-1 bash

# Creating volumes to support persistent storage

NOTE: NIFI runs as USER ID 1000 - which is hadoop in host


docker volume create --name nifi-cluster-a

docker volume inspect nifi-cluster-a

mkdir /var/lib/docker/volumes/nifi-cluster-a/_data/nifi-data


cd nifi-data
mkdir -p a/1 a/2 a/files
mkdir -p b/1 b/2 b/files
mkdir inbox_from_site_a && chown hadoop:docker inbox_from_site_a



# DEBUGGING SITE B

cp /root/docker-compose-nifi-cluster/nifi-data/a/files/sample_a /root/docker-compose-nifi-cluster/nifi-data/a/files/sample_4

