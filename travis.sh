#!/bin/sh
rm -f ./sbt
wget https://raw.github.com/paulp/sbt-extras/master/sbt &&
chmod u+x ./sbt &&
./sbt -sbt-version 0.12.2 -mem 1024 \
  test 
