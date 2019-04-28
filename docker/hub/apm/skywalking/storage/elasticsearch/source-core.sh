#!/bin/sh

nohup /usr/share/elasticsearch/bin/elasticsearch &
sleep 15

$SKYWALKING_HOME/bin/oapService.sh
sleep 30

$SOURCE_HOME/bin/source-core
tail -f -n +1 $SOURCE_HOME/logs/source-core.log