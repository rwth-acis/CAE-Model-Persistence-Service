:: this script starts a las2peer node providing the model-persistence-service
:: pls execute it from the bin folder of your deployment by double-clicking on it

%~d0
cd %~p0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;"

java -cp %CLASSPATH% i5.las2peer.tools.L2pNodeLauncher -p 9011 uploadStartupDirectory('etc/startup') --service-directory service startService('i5.las2peer.services.modelPersistenceService.ModelPersistenceService@0.1') startWebConnector interactive
pause
