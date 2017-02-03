# CAE-Model-Persistence-Service
This [las2peer](https://github.com/rwth-acis/las2peer) service handles all outgoing and incoming requests from the Community Application Editor's frontend, as well as persisting the created frontend-, backend- and application-models. The actual generation of code and its repositories is handled by the [code generation service](https://github.com/rwth-acis/CAE-Code-Generation-Service).

## How to build this service
Building the service is just building a las2peer service. The [las2peer template project](https://github.com/rwth-acis/las2peer-Template-Project) and its wiki contain detailed information, but basically you should be able to clone the repository and execute a build using ant.

## How to set up the CAE
To set up your own CAE instance some work is required. This should give you an overview of the dependencies. (TBC)

### 1. Building the CAE services
Build the model persistence service and code generation service from source as described above. You can also download jar files from the GitHub releases, but building the current master branch is recommended.

### 2. The GitHub organization and the CAE Templates
The CAE uses GitHub organizations as a central place for its generated code's repositories. Create a GitHub organization and enter its name in the [code generation service's] (https://github.com/rwth-acis/CAE-Code-Generation-Service) .properties file. Then fork the [CAE-Template repository](https://github.com/rwth-acis/CAE-Templates) to your organization. The default name is CAE-Templates, if you want to use another one, e.g. your own templates, change the name in the .properties file.

### 3. Setting up a database
TBA

### 4. Setting up the ROLE-SDK
TBA [ROLE-SDK](https://github.com/rwth-acis/ROLE-SDK)

### 5. Setting up the CAE Frontend
