#!/usr/bin/env bash
java -Xmx9000m -Xss1m -verbose:gc -Xloggc:/styx/home/hzlindzh/rpc-server/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:SurvivorRatio=3 -XX:NewSize=2g -jar rpc-server.jar -h0.0.0.0 -p44444 -th200 -t600000
