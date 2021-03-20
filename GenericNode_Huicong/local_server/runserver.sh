#!/bin/bash

cp /home/huicong/Downloads/GenericNode/target/GenericNode.jar /home/huicong/Downloads/GenericNode/local_server/

# TCP Server
java -jar GenericNode.jar ts 1230 &
java -jar GenericNode.jar ts 1231 &
java -jar GenericNode.jar ts 1232 &
java -jar GenericNode.jar ts 1233 &
java -jar GenericNode.jar ts 1234 &

