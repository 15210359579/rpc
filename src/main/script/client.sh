#!/usr/bin/env bash
java -Xmx9000m -Xss1m -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:/styx/home/hzlindzh/rpc-client/gc.log -XX:+UseParallelGC -XX:+UseParallelOldGC -jar rpc-client.jar -h10.120.47.41 -p44444 -t300000 -th200 -c25000 -s1000 &
