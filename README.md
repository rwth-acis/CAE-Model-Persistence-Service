# CAE-Model-Persistence-Service
This [las2peer](https://github.com/rwth-acis/las2peer) service handles all outgoing and incoming requests from the Community Application Editor's frontend, as well as persisting the created frontend-, backend- and application-models. The actual generation of code and its repositories is handled by the [code generation service](https://github.com/rwth-acis/CAE-Code-Generation-Service).

## Are you looking for the official CAE instance?
If you just want to try out the Community Application Editor you don't need to set up your own environment. For building applications there are the following spaces:
* [Microservice Editor](http://cloud10.dbis.rwth-aachen.de:8081/spaces/CAEMicroservice)
* [Frontend Widget Editor](http://cloud10.dbis.rwth-aachen.de:8081/spaces/CAEFrontend)
* [Application Editor](http://cloud10.dbis.rwth-aachen.de:8081/spaces/CAEApplication)

Otherwise feel free to continue down there.

## How to build this service
Building the service is just building a las2peer service. The [las2peer template project](https://github.com/rwth-acis/las2peer-Template-Project) and its wiki contain detailed information, but basically you should be able to clone the repository and execute a build using ant.

## How to set up the CAE
To set up your own CAE instance take a look at the [wiki](https://github.com/rwth-acis/CAE/wiki/Deployment-and-Configuration)
