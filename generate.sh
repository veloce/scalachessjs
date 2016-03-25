#!/bin/bash

sbt fastOptJS || exit $?

cp target/scala-*/*-fastopt.js* build/
