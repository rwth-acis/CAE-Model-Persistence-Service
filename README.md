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
To set up your own CAE instance some work is required. This should give you an overview of the dependencies and preparatory steps.

### 1. Building the CAE services
Build the model persistence service and code generation service from source as described above. You can also download jar files from the GitHub releases, but building the current master branch is recommended.

### 2. The GitHub organization and the CAE Templates
The CAE uses GitHub organizations as a central place for its generated code's repositories. Create a GitHub organization and enter its name in the [code generation service's](https://github.com/rwth-acis/CAE-Code-Generation-Service) .properties file. Then fork the [CAE-Template repository](https://github.com/rwth-acis/CAE-Templates) to your organization. The default name is CAE-Templates, if you want to use another one, e.g. your own templates, change the name in the .properties file.

### 3. Setting up a database
The model persistence service needs a database to store model data. JDBC is used for database connectivity and the .properties file contains the corresponding settings. By default it contains settings for a mysql database, just fill in the schema, user and password you want to use. To initialize your database for use with the model persistence service import the ModelPersistenceService_Database.sql file frome the database folder. By default the database name "commedit" is assumed.

### 4. Setting up the ROLE-SDK
The CAE's frontend consists of widgets in a ROLE environment. To set up your own [ROLE-SDK](https://github.com/rwth-acis/ROLE-SDK) instance follow the information provided in their repository. You need maven to build the latest ROLE-SDK version, alternatively you can use a binary release provided by the ROLE project.

### 5. Setting up the CAE Frontend
The next step consists of preparing the CAE frontend in the form of widgets for the ROLE instance you created. The CAE uses [SyncMeta](https://github.com/rwth-acis/syncmeta) widgets together with some additional widgets available at [CAE-Frontend](https://github.com/rwth-acis/CAE-Frontend). Please follow the instructions provided in the SyncMeta repository to build the widgets. For the additional widgets there are detailed instructions available in the widgets subdirectory of the CAE-Frontend repository, but the process is similar.
While ```grunt connect``` can be used to serve the widgets it is recommended to serve them together with the SyncMeta widgets with a webserver of your choice.
The widgets can than be added by using the little plus button in the widget list:

![Add button](http://i.imgur.com/jEy1ZfK.png)

In the modal enter the url to the xml file of the widget you want to add.
![Modal](http://i.imgur.com/LMtVzGW.png)

It might be useful to check your browser's console for any errors that might occur due to misconfiguration, such as worng paths. If that is the case, re-run the build scripts with the correct parameters or do a manual replacement and create an issu or, even better, a pull request if something is missing from the scripts.
