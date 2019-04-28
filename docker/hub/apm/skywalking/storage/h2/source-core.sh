#!/bin/sh

$SKYWALKING_HOME/bin/oapService.sh
sleep 5

$SOURCE_HOME/bin/source-core
tail -f -n +1 $SOURCE_HOME/logs/source-core.log