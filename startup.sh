#!/bin/bash

cd $(dirname $(readlink -f $0)) || exit

until curl --output /dev/null --silent --head --fail http://localhost:9200; do
    printf '.'
    sleep 5
done
until curl --output /dev/null --silent --head --fail http://localhost:8080; do
    printf '.'
    sleep 5
done

./target/universal/stage/bin/oerworldmap -J-Xms2G -J-Xmx2G -J-server -Duser.language=en -Duser.country=EN -Dconfig.file=./conf/application.conf
