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

## How to run using Docker

First build the image:
```bash
docker build . -t cae-model-persistence-service
```

Then you can run the image like this:

```bash
docker run -e MYSQL_USER=myuser -e MYSQL_PASSWORD=mypasswd -p 8080:8080 -p 9011:9011 cae-model-persistence-service
```

Replace *myuser* and *mypasswd* with the username and password of a MySQL user with access to a database named *commedit*.
By default the database host is *mysql* and the port is *3306*.
The REST-API will be available via *http://localhost:8080/CAE* and the las2peer node is available via port 9011.

In order to customize your setup you can set further environment variables.

### Node Launcher Variables

Set [las2peer node launcher options](https://github.com/rwth-acis/las2peer-Template-Project/wiki/L2pNodeLauncher-Commands#at-start-up) with these variables.
The las2peer port is fixed at *9011*.

| Variable | Default | Description |
|----------|---------|-------------|
| BOOTSTRAP | unset | Set the --bootstrap option to bootrap with existing nodes. The container will wait for any bootstrap node to be available before continuing. |
| SERVICE_PASSPHRASE | Passphrase | Set the second argument in *startService('<service@version>', '<SERVICE_PASSPHRASE>')*. |
| SERVICE_EXTRA_ARGS | unset | Set additional launcher arguments. Example: ```--observer``` to enable monitoring. |

### Service Variables

| Variable | Default |
|----------|---------|
| MYSQL_USER | *mandatory* |
| MYSQL_PASSWORD | *mandatory* |
| MYSQL_HOST | mysql |
| MYSQL_PORT | 3306 |
| SEMANTIC_CHECK_SERVICE | "" |
| CODE_GENERATION_SERVICE | i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1 |
| METADATA_SERVICE | i5.las2peer.services.metadataService.MetadataService@0.1 |
| DEPLOYMENT_URL | http://localhost:8080 |

### Web Connector Variables

Set [WebConnector properties](https://github.com/rwth-acis/las2peer-Template-Project/wiki/WebConnector-Configuration) with these variables.
*httpPort* and *httpsPort* are fixed at *8080* and *8443*.

| Variable | Default |
|----------|---------|
| START_HTTP | TRUE |
| START_HTTPS | FALSE |
| SSL_KEYSTORE | "" |
| SSL_KEY_PASSWORD | "" |
| CROSS_ORIGIN_RESOURCE_DOMAIN | * |
| CROSS_ORIGIN_RESOURCE_MAX_AGE | 60 |
| ENABLE_CROSS_ORIGIN_RESOURCE_SHARING | TRUE |
| OIDC_PROVIDERS | https://api.learning-layers.eu/o/oauth2,https://accounts.google.com |

### Other Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DEBUG  | unset | Set to any value to get verbose output in the container entrypoint script. |
| INIT_WIREFRAME_EXTENSION | unset | Set to any value to extend the database schema with the wireframe extension tables. |


### Volumes

The following places should be persisted in volumes in productive scenarios:

| Path | Description |
|------|-------------|
| /src/node-storage | Pastry P2P storage. |
| /src/etc/startup | Service agent key pair and passphrase. |
| /src/log | Log files. |

*Do not forget to persist you database data*