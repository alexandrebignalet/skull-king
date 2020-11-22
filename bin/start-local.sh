#!/bin/bash
# start local skullking server

function cleanup {
  echo "Stopping emulator"
  kill ${gradle_process}
  ./bin/stop-firebase-emulator
}

trap cleanup EXIT

./bin/start-firebase-emulator
./gradlew run &
gradle_process=$!

read -p "Init successful, press RETURN to stop"
