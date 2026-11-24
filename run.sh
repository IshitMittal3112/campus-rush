#!/bin/sh
# GUI: ./run.sh | console: ./run.sh console | tests: ./run.sh test | balance sim: ./run.sh sim
cd "$(dirname "$0")"
case "$1" in
  console) mvn -q exec:java -Dexec.mainClass=com.campusrush.console.ConsoleMain ;;
  test)    mvn -q compile exec:java -Dexec.mainClass=com.campusrush.sim.SelfTest ;;
  sim)     mvn -q exec:java -Dexec.mainClass=com.campusrush.sim.BotSimulator ;;
  *)       mvn -q javafx:run ;;
esac
