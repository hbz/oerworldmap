#!/bin/bash

cd "$(dirname $(readlink -f $0))" || exit

until curl --output /dev/null --silent --head --fail http://localhost:9200; do
    printf '.'
    sleep 5
done
until curl --output /dev/null --silent --head --fail http://localhost:8080; do
    printf '.'
    sleep 5
done

kill "$(cat ./target/universal/stage/RUNNING_PID)" || rm ./target/universal/stage/RUNNING_PID
./target/universal/stage/bin/oerworldmap -J-Xms2G -J-Xmx2G -J-server -Duser.language=en -Duser.country=EN -Dhttp.port=9000 -Dconfig.file=./conf/application.conf
