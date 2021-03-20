#!/bin/bash
PORT=1230

cp /home/huicong/Downloads/GenericNode/target/GenericNode.jar /home/huicong/Downloads/GenericNode/local_client/

# TCP Client
java -jar GenericNode.jar tc localhost $PORT put a 123 &
java -jar GenericNode.jar tc localhost $PORT put b 456 &
java -jar GenericNode.jar tc localhost $PORT put c 789 &
java -jar GenericNode.jar tc localhost $PORT get a
java -jar GenericNode.jar tc localhost $PORT del a
java -jar GenericNode.jar tc localhost 1230 store
java -jar GenericNode.jar tc localhost 1231 store
java -jar GenericNode.jar tc localhost 1232 store

