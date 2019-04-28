#!/bin/sh

$SKYWALKING_HOME/bin/oapService.sh

$SOURCE_HOME/bin/source-core
tail -f -n +1 $SOURCE_HOME/logs/source-core.log