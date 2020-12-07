#!/bin/bash
# start local skullking server

function cleanup {
  echo "Stopping emulator"
  kill ${gradle_process}
  ./bin/stop-firebase-emulator
  docker-compose stop
  docker-compose rm -f
}

trap cleanup EXIT

docker-compose up -d

./bin/start-firebase-emulator

./gradlew run &
gradle_process=$!

read -p "Init successful, press RETURN to stop"
