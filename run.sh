#!/bin/bash

mvn exec:java -D exec.mainClass=oeg.dia.fi.upm.es.GtfsRealtimeTripUpdatesProducerDemoMain -Dexec.args="--tripUpdatesUrl=http://localhost:8080/barcelona/tram/tbs/trip-updates"


