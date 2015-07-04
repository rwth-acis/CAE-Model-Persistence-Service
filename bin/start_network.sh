#!/bin/bash

# this script starts a las2peer node providing the model-persistence-service
# pls execute it from the root folder of your deployment, e. g. ./bin/start_network.sh

java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -p 9011 uploadStartupDirectory\(\'etc/startup\'\) startService\(\'i5.las2peer.services.modelPersistenceService.ModelPersistenceService\') startWebConnector interactive