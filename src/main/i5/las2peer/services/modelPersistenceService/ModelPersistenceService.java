package i5.las2peer.services.modelPersistenceService;

import java.io.Serializable;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonProcessingException;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.SimpleModel;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.api.Service;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.services.modelPersistenceService.database.DatabaseManager;
import i5.las2peer.services.modelPersistenceService.exception.CGSInvocationException;
import i5.las2peer.services.modelPersistenceService.exception.ModelNotFoundException;
import i5.las2peer.services.modelPersistenceService.model.Model;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

/**
 * 
 * CAE Model Persistence Service
 * 
 * A LAS2peer service used for persisting (and validating) application models. Part of the CAE.
 * 
 */

@Path("CAE/models")
@Version("0.1")
@Api
@SwaggerDefinition(info = @Info(title = "CAE Model Persistence Service", version = "0.1",
    description = "A LAS2peer service used for persisting (and validating) application models. Part of the CAE.",
    termsOfService = "none",
    contact = @Contact(name = "Peter de Lange", url = "https://github.com/PedeLa/",
        email = "lange@dbis.rwth-aachen.de") ,
    license = @License(name = "BSD",
        url = "https://github.com/PedeLa/CAE-Model-Persistence-Service//blob/master/LICENSE.txt") ) )
public class ModelPersistenceService extends Service {

  /*
   * Database configuration
   */
  private String jdbcDriverClassName;
  private String jdbcLogin;
  private String jdbcPass;
  private String jdbcUrl;
  private String jdbcSchema;
  private DatabaseManager dbm;

  /*
   * Global variables
   */
  private boolean useCodeGenerationService;

  private final L2pLogger logger = L2pLogger.getInstance(ModelPersistenceService.class.getName());

  public ModelPersistenceService() {
    // read and set properties values
    setFieldValues();
    // instantiate a database manager to handle database connection pooling and credentials
    dbm = new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass, jdbcUrl, jdbcSchema);
  }


  /**
   * 
   * Entry point for all new models. Stores it to the database.
   * 
   * @param inputModel the model as a JSON string
   * 
   * @return HttpResponse containing the status code of the request and a small return message
   * 
   */
  @POST
  @Path("/")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Entry point for storing a model to the database.",
      notes = "Entry point for storing a model to the database.")
  @ApiResponses(
      value = {@ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "OK, model stored"),
          @ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST,
              message = "Input model was not valid"),
      @ApiResponse(code = HttpURLConnection.HTTP_CONFLICT,
          message = "Tried to save a model that already had a name and thus was not new"),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR,
          message = "Internal server error")})
  public HttpResponse postModel(@ContentParam String inputModel) {
    L2pLogger.logEvent(Event.SERVICE_MESSAGE, "postModel: trying to store new model");
    Model model;
    try {
      // create the model
      model = new Model(inputModel);
    } catch (ParseException e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "postModel: exception parsing JSON input: " + e);
      HttpResponse r = new HttpResponse("JSON parsing exception, file not valid!",
          HttpURLConnection.HTTP_BAD_REQUEST);
      return r;
    } catch (Exception e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "postModel: something went seriously wrong: " + e);
      logger.printStackTrace(e);
      HttpResponse r =
          new HttpResponse("Internal server error!", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }

    // check if model name is already taken
    if (this.getModel(model.getAttributes().getName())
        .getStatus() != HttpURLConnection.HTTP_NOT_FOUND) {
      L2pLogger.logEvent(Event.SERVICE_MESSAGE,
          "postModel: model name " + model.getAttributes().getName() + " is already taken");
      HttpResponse r = new HttpResponse(
          "Model with name " + model.getAttributes().getName() + " already exists!",
          HttpURLConnection.HTTP_CONFLICT);
      return r;
    }

    // call code generation service
    if (this.useCodeGenerationService) {
      try {
        L2pLogger.logEvent(Event.SERVICE_MESSAGE, "postModel: invoking code generation service..");
        model = callCodeGenerationService("createFromModel", model);
      } catch (CGSInvocationException e) {
        HttpResponse r = new HttpResponse("Model not valid: " + e.getMessage(),
            HttpURLConnection.HTTP_INTERNAL_ERROR);
        return r;
      }
    }

    // save the model to the database
    Connection connection = null;
    try {
      connection = dbm.getConnection();
      model.persist(connection);
      int modelId = model.getId();
      L2pLogger.logEvent(Event.SERVICE_MESSAGE, "postModel: model with id " + modelId + " and name "
          + model.getAttributes().getName() + " stored!");
      HttpResponse r = new HttpResponse("Model stored!", HttpURLConnection.HTTP_CREATED);
      return r;
    } catch (SQLException e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "postModel: exception persisting model: " + e);
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Could not persist, database rejected model!",
          HttpURLConnection.HTTP_BAD_REQUEST);
      return r;
    } catch (Exception e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "postModel: something went seriously wrong: " + e);
      logger.printStackTrace(e);
      HttpResponse r =
          new HttpResponse("Internal server error..", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }
    // always close connections
    finally {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.printStackTrace(e);
      }
    }
  }


  /**
   * 
   * Searches for a model in the database by name.
   * 
   * @param modelName the model as a JSON string
   * 
   * @return HttpResponse containing the status code of the request and (if successful) the model as
   *         a JSON string
   * 
   */
  @GET
  @Path("/{modelName}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      value = "Searches for a model in the database. Takes the modelName as search parameter.",
      notes = "Searches for a model in the database.")
  @ApiResponses(
      value = {@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model found"),
          @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND,
              message = "Model could not be found."),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR,
          message = "Internal server error")})
  public HttpResponse getModel(@PathParam("modelName") String modelName) {
    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "getModel: searching for model with name " + modelName);
    Model model = null;
    Connection connection = null;
    try {
      connection = dbm.getConnection();
      model = new Model(modelName, connection);
    } catch (ModelNotFoundException e) {
      L2pLogger.logEvent(Event.SERVICE_MESSAGE,
          "getModel: did not find model with name " + modelName);
      HttpResponse r = new HttpResponse("Model not found!", HttpURLConnection.HTTP_NOT_FOUND);
      return r;
    } catch (SQLException e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "getModel: exception fetching model: " + e);
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Database error!", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    } catch (Exception e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "getModel: something went seriously wrong: " + e);
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Server error!", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    } finally {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.printStackTrace(e);
      }
    }
    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "getModel: found model " + modelName + ", now converting to JSONObject and returning");
    JSONObject jsonModel = model.toJSONObject();

    HttpResponse r = new HttpResponse(jsonModel.toJSONString(), HttpURLConnection.HTTP_OK);
    return r;
  }


  /**
   * 
   * Retrieves all model names from the database.
   * 
   * 
   * @return HttpResponse containing the status code of the request and (if the database is not
   *         empty) the model-list as a JSON array
   * 
   */
  @SuppressWarnings("unchecked")
  @GET
  @Path("/")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Retrieves a list of models from the database.",
      notes = "Retrieves a list of all models stored in the database. Returns a list of model names.")
  @ApiResponses(value = {
      @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model list is returned"),
      @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "No models in the database"),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR,
          message = "Internal server error")})
  public HttpResponse getModels() {
    ArrayList<String> modelNames = new ArrayList<String>();
    Connection connection = null;
    try {
      connection = dbm.getConnection();
      // search for all models
      PreparedStatement statement =
          connection.prepareStatement("SELECT modelName FROM ModelAttributes;");
      L2pLogger.logEvent(Event.SERVICE_MESSAGE, "getModels: retrieving all models..");
      ResultSet queryResult = statement.executeQuery();
      while (queryResult.next()) {
        modelNames.add(queryResult.getString(1));
      }
      if (modelNames.isEmpty()) {
        L2pLogger.logEvent(Event.SERVICE_MESSAGE, "getModels: database is empty!");
        HttpResponse r = new HttpResponse("Database is empty!", HttpURLConnection.HTTP_NOT_FOUND);
        return r;
      }
      connection.close();
    } catch (SQLException e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "getModels: exception fetching model: " + e);
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Database error!", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    } catch (Exception e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "getModels: something went seriously wrong: " + e);
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Server error!", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    } finally {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.printStackTrace(e);
      }
    }
    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "getModels: created list of models, now converting to JSONObject and returning");
    JSONArray jsonModelList = new JSONArray();
    jsonModelList.addAll(modelNames);

    HttpResponse r = new HttpResponse(jsonModelList.toJSONString(), HttpURLConnection.HTTP_OK);
    return r;
  }


  /**
   * 
   * Deletes a model.
   * 
   * @param modelName a string containing the model name
   * 
   * @return HttpResponse containing the status code of the request
   * 
   */
  @DELETE
  @Path("/{modelName}")
  @Consumes(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a model given by its name.",
      notes = "Deletes a model given by its name.")
  @ApiResponses(
      value = {@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model is deleted"),
          @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Model does not exist"),
          @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR,
              message = "Internal server error")})
  public HttpResponse deleteModel(@PathParam("modelName") String modelName) {
    Connection connection = null;
    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "deleteModel: trying to delete model with name: " + modelName);
    try {
      connection = dbm.getConnection();
      Model model = new Model(modelName, connection);

      // call code generation service
      if (this.useCodeGenerationService) {
        try {
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "deleteModel: invoking code generation service..");
          model = callCodeGenerationService("deleteRepositoryOfModel", model);
        } catch (CGSInvocationException e) {
          HttpResponse r = new HttpResponse("Model not valid: " + e.getMessage(),
              HttpURLConnection.HTTP_INTERNAL_ERROR);
          return r;
        }
      }

      model.deleteFromDatabase(connection);
      L2pLogger.logEvent(Event.SERVICE_MESSAGE, "deleteModel: deleted model " + modelName);
      HttpResponse r = new HttpResponse("Model deleted!", HttpURLConnection.HTTP_OK);
      return r;
    } catch (ModelNotFoundException e) {
      L2pLogger.logEvent(Event.SERVICE_MESSAGE,
          "deleteModel: did not find model with name " + modelName);
      HttpResponse r = new HttpResponse("Model not found!", HttpURLConnection.HTTP_NOT_FOUND);
      return r;
    } catch (SQLException e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "deleteModel: exception deleting model: " + e);
      logger.printStackTrace(e);
      HttpResponse r =
          new HttpResponse("Internal server error..", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    } finally {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.printStackTrace(e);
      }
    }
  }


  /**
   * 
   * Updates a model. Basically only deletes the old one and creates a new one, but if the CAE Code
   * Generation Service is used, it validates the model and prevents updating if it would break the
   * semantics.
   * 
   * @param modelName a string containing the model name
   * @param inputModel the model as a JSON string
   * 
   * @return HttpResponse containing the status code of the request
   * 
   */
  @PUT
  @Path("/{modelName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Updates a model.", notes = "Updates a model.")
  @ApiResponses(
      value = {@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model is updated"),
          @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Model does not exist"),
          @ApiResponse(code = HttpURLConnection.HTTP_CONFLICT,
              message = "Model name may not be changed"),
      @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR,
          message = "Internal server error")})
  public HttpResponse updateModel(@PathParam("modelName") String modelName,
      @ContentParam String inputModel) {
    L2pLogger.logEvent(Event.SERVICE_MESSAGE,
        "updateModel: trying to update model with name: " + modelName);
    Model model;
    // first parse the updated model and check for correctness of format
    try {
      model = new Model(inputModel);
      // the model name is its "id", it may not be changed
      if (!model.getAttributes().getName().equals(modelName)) {
        L2pLogger.logEvent(Event.SERVICE_MESSAGE, "updateModel: posted model name " + modelName
            + " is different from posted model name attribute " + model.getAttributes().getName());
        HttpResponse r =
            new HttpResponse("Model name is different!", HttpURLConnection.HTTP_CONFLICT);
        return r;
      }
    } catch (ParseException e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "updateModel: exception parsing JSON input: " + e);
      HttpResponse r = new HttpResponse("JSON parsing exception, file not valid!",
          HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    } catch (Exception e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "updateModel: something went seriously wrong: " + e);
      logger.printStackTrace(e);
      HttpResponse r =
          new HttpResponse("Internal server error!", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }

    // call code generation service
    if (this.useCodeGenerationService) {
      try {
        L2pLogger.logEvent(Event.SERVICE_MESSAGE,
            "updateModel: invoking code generation service..");
        model = callCodeGenerationService("updateRepositoryOfModel", model);
      } catch (CGSInvocationException e) {
        HttpResponse r = new HttpResponse("Model not valid: " + e.getMessage(),
            HttpURLConnection.HTTP_INTERNAL_ERROR);
        return r;
      }
    }

    // if this has thrown no exception, we can delete the "old" model and persist the new one
    Connection connection = null;
    try {
      connection = dbm.getConnection();
      // load and delete the old model from the database
      L2pLogger.logEvent(Event.SERVICE_MESSAGE,
          "updateModel: loading and deleting old model with name " + modelName);
      new Model(modelName, connection).deleteFromDatabase(connection);
      // check if the "old" model did exist
    } catch (ModelNotFoundException e) {
      L2pLogger.logEvent(Event.SERVICE_MESSAGE,
          "updateModel: there exists no model with name: " + modelName);
      HttpResponse r = new HttpResponse("Model not found!", HttpURLConnection.HTTP_NOT_FOUND);
      return r;
    } catch (SQLException e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "updateModel: error deleting old model: " + e);
      logger.printStackTrace(e);
      HttpResponse r =
          new HttpResponse("Internal server error..", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }
    // always close connections
    finally {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.printStackTrace(e);
      }
    }
    try {
      connection = dbm.getConnection();
      // save the model to the database
      model.persist(connection);
      int modelId = model.getId();
      L2pLogger.logEvent(Event.SERVICE_MESSAGE, "updateModel: model with new id " + modelId
          + " and name "
          + model.getAttributes().getName() + " stored!");
      HttpResponse r = new HttpResponse("Model updated!", HttpURLConnection.HTTP_OK);
      return r;
    } catch (SQLException e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "updateModel: exception persisting model: " + e);
      logger.printStackTrace(e);
      HttpResponse r = new HttpResponse("Could not persist, database rejected model!",
          HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    } catch (Exception e) {
      L2pLogger.logEvent(Event.SERVICE_ERROR, "updateModel: something went seriously wrong: " + e);
      logger.printStackTrace(e);
      HttpResponse r =
          new HttpResponse("Internal server error..", HttpURLConnection.HTTP_INTERNAL_ERROR);
      return r;
    }
    // always close connections
    finally {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.printStackTrace(e);
      }
    }
  }


  ////////////////////////////////////////////////////////////////////////////////////////
  // Methods special to the CAE. Feel free to ignore them:-)
  ///////////////////////////////////////////////////////////////////////////////////////

  /**
   * 
   * Loads a model from the database (by calling the respective resource) and sends it to the code
   * generation service, requesting a Communication Model view to be displayed in SyncMeta's
   * application editor view.
   * 
   * TODO: Not tested..
   * 
   * @param modelName the name of the model to be loaded.
   * 
   * @return HttpResponse containing the status code of the request and the communication view model
   *         as a JSON string
   */
  @GET
  @Path("/commView/{modelName}")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets a CAE communication view model.",
      notes = "Gets a CAE communication view model.")
  @ApiResponses(
      value = {@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model found"),
          @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Model does not exist"),
          @ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR,
              message = "Internal server error")})
  public HttpResponse getCAECommunicationModel(@PathParam("modelName") String modelName) {
    // load the application model from the database
    SimpleModel appModel;
    Connection connection = null;
    try {
      connection = dbm.getConnection();
      L2pLogger.logEvent(Event.SERVICE_MESSAGE,
          "getCAECommunicationModel: Loading model " + modelName + " from the database");
      appModel = (SimpleModel) new Model(modelName, connection).getMinifiedRepresentation();
    } catch (SQLException e) {
      // model might not exist
      logger.printStackTrace(e);
      L2pLogger.logEvent(Event.SERVICE_ERROR,
          "getCAECommunicationModel: model " + modelName + " not found");
      HttpResponse r = new HttpResponse("Model " + modelName + " does not exist!",
          HttpURLConnection.HTTP_NOT_FOUND);
      return r;
    } finally {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.printStackTrace(e);
      }
    }
    // load submodules of application model from the database
    Serializable[] modelsToSend = null;
    for (SimpleEntityAttribute attribute : appModel.getAttributes()) {
      if (attribute.getName().equals("type") && attribute.getValue().equals("application")) {
        modelsToSend = new SimpleModel[appModel.getNodes().size() + 1];
        modelsToSend[0] = appModel; // first is always "application" model itself
        int modelsToSendIndex = 1;
        // iterate through the nodes and add corresponding models to array
        for (SimpleNode node : appModel.getNodes()) {
          // send application models only have one attribute with its label
          String subModelName = node.getAttributes().get(0).getValue();
          try {
            connection = dbm.getConnection();
            modelsToSend[modelsToSendIndex] =
                new Model(subModelName, connection).getMinifiedRepresentation();
          } catch (SQLException e) {
            // model might not exist
            logger.printStackTrace(e);
            L2pLogger.logEvent(Event.SERVICE_ERROR,
                "getCAECommunicationModel: Error loading application component: " + subModelName);
            HttpResponse r =
                new HttpResponse("Internal server error..", HttpURLConnection.HTTP_INTERNAL_ERROR);
            return r;
          } finally {
            try {
              connection.close();
            } catch (SQLException e) {
              logger.printStackTrace(e);
            }
          }
          modelsToSendIndex++;
        }
        // invoke code generation service
        try {
          Serializable[] payload = {modelsToSend};
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "getCAECommunicationModel: Invoking code generation service now..");
          SimpleModel communicationModel = (SimpleModel) this.invokeServiceMethod(
              "i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1",
              "getCommunicationViewOfApplicationModel", payload);
          L2pLogger.logEvent(Event.SERVICE_MESSAGE,
              "getCAECommunicationModel: Got communication model from code generation service..");
          Model returnModel = new Model(communicationModel);
          L2pLogger.logEvent(Event.SERVICE_MESSAGE, "getCAECommunicationModel: Created model "
              + modelName
              + "from simple model, now converting to JSONObject and returning");
          JSONObject jsonModel = returnModel.toJSONObject();
          HttpResponse r = new HttpResponse(jsonModel.toJSONString(), HttpURLConnection.HTTP_OK);
          return r;
        } catch (Exception e) {
          L2pLogger.logEvent(Event.SERVICE_ERROR,
              "getCAECommunicationModel: Internal error " + e.getMessage());
          logger.printStackTrace(e);
          HttpResponse r =
              new HttpResponse("Internal server error..", HttpURLConnection.HTTP_INTERNAL_ERROR);
          return r;
        }
      }
    }
    L2pLogger.logEvent(Event.SERVICE_ERROR,
        "getCAECommunicationModel: model " + modelName + " is not an application");
    HttpResponse r =
        new HttpResponse("Internal server error..", HttpURLConnection.HTTP_INTERNAL_ERROR);
    return r;
  }


  /**
   * 
   * Calls the code generation service to see if the model is a valid CAE model. Also implements a
   * bit of CAE logic by checking if the code generation service needs additional models (in case of
   * an application model) and serves them automatically, such that the rest of this service does
   * not have to deal with this "special case".
   * 
   * @param methodName the method name of the code generation service
   * @param a {@link Model}
   * 
   * @return the model
   * 
   * @throws CGSInvocationException if something went wrong invoking the service
   * 
   */
  private Model callCodeGenerationService(String methodName, Model model)
      throws CGSInvocationException {
    Connection connection = null;
    Serializable[] modelsToSend = null;
    SimpleModel simpleModel = (SimpleModel) model.getMinifiedRepresentation();
    boolean isApplication = false;
    for (SimpleEntityAttribute attribute : simpleModel.getAttributes()) {
      if (attribute.getName().equals("type")) {
        // handle special case of application model
        if (attribute.getValue().equals("application")) {
          isApplication = true;
          break;
        }
      }
    }
    if (isApplication) {
      modelsToSend = new SimpleModel[simpleModel.getNodes().size() + 1];
      modelsToSend[0] = simpleModel; // first is always "application" model itself
      int modelsToSendIndex = 1;
      // iterate through the nodes and add corresponding models to array
      for (SimpleNode node : simpleModel.getNodes()) {
        // since application models only have one attribute with its label
        String modelName = node.getAttributes().get(0).getValue();
        try {
          connection = dbm.getConnection();
          modelsToSend[modelsToSendIndex] =
              new Model(modelName, connection).getMinifiedRepresentation();
        } catch (SQLException e) {
          // model might not exist
          logger.printStackTrace(e);
          throw new CGSInvocationException("Error loading application component: " + modelName);
        } finally {
          try {
            connection.close();
          } catch (SQLException e) {
            logger.printStackTrace(e);
          }
        }
        modelsToSendIndex++;
      }
    } else {
      modelsToSend = new SimpleModel[1];
      modelsToSend[0] = simpleModel;
    }
    // actual invocation
    try {
      Serializable[] payload = {modelsToSend};
      String answer = (String) this.invokeServiceMethod(
          "i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1", methodName, payload);
      if (!answer.equals("done")) {
        throw new CGSInvocationException(answer);
      }
      return model;
    } catch (Exception e) {
      logger.printStackTrace(e);
      throw new CGSInvocationException(e.getMessage());
    }
  }


  ////////////////////////////////////////////////////////////////////////////////////////
  // Methods required by the LAS2peer framework.
  ////////////////////////////////////////////////////////////////////////////////////////

  /**
   * 
   * This method is needed for every RESTful application in LAS2peer.
   * 
   * @return the mapping
   * 
   */
  public String getRESTMapping() {
    String result = "";
    try {
      result = RESTMapper.getMethodsAsXML(this.getClass());
    } catch (Exception e) {
      logger.printStackTrace(e);
    }
    return result;
  }


  ////////////////////////////////////////////////////////////////////////////////////////
  // Methods providing a Swagger documentation of the service API.
  ////////////////////////////////////////////////////////////////////////////////////////

  /**
   * 
   * Returns the API documentation for a specific annotated top level resource for purposes of the
   * Swagger documentation.
   * 
   * Note: If you do not intend to use Swagger for the documentation of your Service API, this
   * method may be removed.
   * 
   * Trouble shooting: Please make sure that the endpoint URL below is correct with respect to your
   * service.
   * 
   * @return the resource's documentation
   * 
   */
  @GET
  @Path("/swagger.json")
  @Produces(MediaType.APPLICATION_JSON)
  public HttpResponse getSwaggerJSON() {
    Swagger swagger = new Reader(new Swagger()).read(this.getClass());
    if (swagger == null) {
      return new HttpResponse("Swagger API declaration not available!",
          HttpURLConnection.HTTP_NOT_FOUND);
    }
    try {
      return new HttpResponse(Json.mapper().writeValueAsString(swagger), HttpURLConnection.HTTP_OK);
    } catch (JsonProcessingException e) {
      logger.printStackTrace(e);
      return new HttpResponse(e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
    }
  }

}
