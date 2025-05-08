#!/bin/bash

cd ./app/

echo "hello from setup.sh"

i=0
while [ $i -le 5 ]; do
  sleep 1

  ((i++))

  echo "banana"

done
