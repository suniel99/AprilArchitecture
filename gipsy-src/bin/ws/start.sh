#!/bin/bash
#
# $Id: start.sh
#

JAVA="java -Xss228k"
LIB="lib"

exec $JAVA \
     -Djava.security.policy=start.policy \
     -jar $LIB/start.jar \
     startTransient4.config

# EOF
