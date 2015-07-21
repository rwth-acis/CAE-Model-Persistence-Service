package i5.las2peer.services.modelPersistenceService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.Consumes;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.DELETE;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.POST;
import i5.las2peer.restMapper.annotations.PUT;
import i5.las2peer.restMapper.annotations.Path;
import i5.las2peer.restMapper.annotations.PathParam;
import i5.las2peer.restMapper.annotations.Produces;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.annotations.swagger.ApiInfo;
import i5.las2peer.restMapper.annotations.swagger.ApiResponse;
import i5.las2peer.restMapper.annotations.swagger.ApiResponses;
import i5.las2peer.restMapper.annotations.swagger.ResourceListApi;
import i5.las2peer.restMapper.annotations.swagger.Summary;
import i5.las2peer.services.modelPersistenceService.database.DatabaseManager;
import i5.las2peer.services.modelPersistenceService.database.exception.ModelNotFoundException;
import i5.las2peer.services.modelPersistenceService.model.Model;

/**
 * 
 * CAE Model Persistence Service
 * 
 * A LAS2peer service used for persisting (and validating) application models. Part of the CAE.
 * 
 */
@Path("CAE/models")
@Version("0.1")
@ApiInfo(title = "CAE Model Persistence Service",
    description = "A LAS2peer service used for persisting (and validating) application models. Part of the CAE.",
    termsOfServiceUrl = "none", contact = "lange@dbis.rwth-aachen.de", license = "BSD",
    licenseUrl = "https://github.com/PedeLa/CAE-Model-Persistence-Service/LICENSE.txt")
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
   * WebConnector configuration (required by Swagger)
   */
  private String webconnectorProtocol = "http";
  private String webconnectorIpAdress = "localhost";
  private String webconnectorPort = "8080";

  /*
   * Global variables
   */
  private boolean useCodeGenerationService;

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
  @ResourceListApi(description = "Entry point for storing a model to the database.")
  @ApiResponses(value = {@ApiResponse(code = 201, message = "OK, model stored"),
      @ApiResponse(code = 400, message = "Input model was not valid"),
      @ApiResponse(code = 409,
          message = "Tried to save a model that already had a name and thus was not new"),
      @ApiResponse(code = 500, message = "Internal server error")})
  @Summary("Entry point for storing a model to the database")
  public HttpResponse postModel(@ContentParam String inputModel) {
    logMessage("postModel: trying to store new model");
    Model model;
    try {
      // create the model
      model = new Model(inputModel);
    } catch (ParseException e) {
      logError("postModel: exception parsing JSON input: " + e);
      HttpResponse r = new HttpResponse("JSON parsing exception, file not valid!", 400);
      return r;
    } catch (Exception e) {
      logError("postModel: something went seriously wrong: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Internal server error!", 500);
      return r;
    }

    // check if model name is already taken
    if (this.getModel(model.getAttributes().getName()).getStatus() != 404) {
      logMessage("postModel: model name " + model.getAttributes().getName() + " is already taken");
      HttpResponse r = new HttpResponse(
          "Model with name " + model.getAttributes().getName() + " already exists!", 409);
      return r;
    }
    // call code generation service
    if (this.useCodeGenerationService) {
      try {
        logMessage("postModel: invoking code generation service..");
        String returnMessage = (String) this.invokeServiceMethod(
            "i5.las2peer.services.codeGenerationService.CodeGenerationService", "createFromModel",
            model.getMinifiedRepresentation());
        if (!returnMessage.equals("done")) {
          HttpResponse r = new HttpResponse("Model not valid: " + returnMessage, 500);
          return r;
        }
      } catch (Exception e) {
        e.printStackTrace();
        HttpResponse r = new HttpResponse("Internal server error..", 500);
        return r;
      }
    }

    // save the model to the database
    Connection connection = null;
    try {
      connection = dbm.getConnection();
      model.persist(connection);
      int modelId = model.getId();
      logMessage("postModel: model with id " + modelId + " and name "
          + model.getAttributes().getName() + " stored!");
      HttpResponse r = new HttpResponse("Model stored!", 201);
      return r;
    } catch (SQLException e) {
      logError("postModel: exception persisting model: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Could not persist, database rejected model!", 400);
      return r;
    } catch (Exception e) {
      logError("postModel: something went seriously wrong: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Internal server error..", 500);
      return r;
    }
    // always close connections
    finally {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
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
  @ResourceListApi(
      description = "Searches for a model in the database. Takes the modelName as search parameter")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK, model found"),
      @ApiResponse(code = 404, message = "Model could not be found."),
      @ApiResponse(code = 500, message = "Internal server error")})
  @Summary("Searches for a model in the database.")
  public HttpResponse getModel(@PathParam("modelName") String modelName) {
    logMessage("getModel: searching for model with name " + modelName);
    Model model = null;
    Connection connection = null;
    try {
      connection = dbm.getConnection();
      model = new Model(modelName, connection);
    } catch (ModelNotFoundException e) {
      logMessage("getModel: did not find model with name " + modelName);
      HttpResponse r = new HttpResponse("Model not found!", 404);
      return r;
    } catch (SQLException e) {
      logError("getModel: exception fetching model: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Database error!", 500);
      return r;
    } catch (Exception e) {
      logError("getModel: something went seriously wrong: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Server error!", 500);
      return r;
    } finally {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    logMessage(
        "getModel: found model " + modelName + ", now converting to JSONObject and returning");
    JSONObject jsonModel = model.toJSONObject();

    HttpResponse r = new HttpResponse(jsonModel.toJSONString(), 200);
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
  @ResourceListApi(
      description = "Retrieves a list of all models stored in the database. Returns a list of model names.")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK, model list is returned"),
      @ApiResponse(code = 404, message = "No models in the database"),
      @ApiResponse(code = 500, message = "Internal server error")})
  @Summary("Retrieves a list of models from the database.")
  public HttpResponse getModels() {
    ArrayList<String> modelNames = new ArrayList<String>();
    Connection connection = null;
    try {
      connection = dbm.getConnection();
      // search for all models
      PreparedStatement statement =
          connection.prepareStatement("SELECT modelName FROM ModelAttributes;");
      logMessage("getModels: retrieving all models..");
      ResultSet queryResult = statement.executeQuery();
      while (queryResult.next()) {
        modelNames.add(queryResult.getString(1));
      }
      if (modelNames.isEmpty()) {
        logMessage("getModels: database is empty!");
        HttpResponse r = new HttpResponse("Database is empty!", 404);
        return r;
      }
      connection.close();
    } catch (SQLException e) {
      logError("getModels: exception fetching model: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Database error!", 500);
      return r;
    } catch (Exception e) {
      logError("getModels: something went seriously wrong: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Server error!", 500);
      return r;
    } finally {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    logMessage("getModels: created list of models, now converting to JSONObject and returning");
    JSONArray jsonModelList = new JSONArray();
    jsonModelList.addAll(modelNames);

    HttpResponse r = new HttpResponse(jsonModelList.toJSONString(), 200);
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
  @ResourceListApi(description = "Deletes a model given by its name.")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK, model is deleted"),
      @ApiResponse(code = 404, message = "Model does not exist"),
      @ApiResponse(code = 500, message = "Internal server error")})
  @Summary("Deletes a model given by its name.")
  public HttpResponse deleteModel(@PathParam("modelName") String modelName) {
    Connection connection = null;
    logMessage("deleteModel: trying to delete model with name: " + modelName);
    try {
      connection = dbm.getConnection();
      Model model = new Model(modelName, connection);
      model.deleteFromDatabase(connection);
      logMessage("deleteModel: eleted model " + modelName);
      HttpResponse r = new HttpResponse("Model deleted!", 200);
      return r;
    } catch (ModelNotFoundException e) {
      logMessage("deleteModel: did not find model with name " + modelName);
      HttpResponse r = new HttpResponse("Model not found!", 404);
      return r;
    } catch (SQLException e) {
      logError("deleteModel: exception deleting model: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Internal server error..", 500);
      return r;
    } finally {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
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
  @ResourceListApi(description = "Updates a model.")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK, model is updated"),
      @ApiResponse(code = 404, message = "Model does not exist"),
      @ApiResponse(code = 409, message = "Model name may not be changed"),
      @ApiResponse(code = 500, message = "Internal server error")})
  @Summary("Updates a model.")
  public HttpResponse updateModel(@PathParam("modelName") String modelName,
      @ContentParam String inputModel) {
    logMessage("updateModel: trying to update model with name: " + modelName);
    Model model;
    // first parse the updated model and check for correctness of format
    try {
      model = new Model(inputModel);
      // the model name is its "id", it may not be changed
      if (!model.getAttributes().getName().equals(modelName)) {
        logMessage("updateModel: posted model name " + modelName
            + " is different from posted model name attribute " + model.getAttributes().getName());
        HttpResponse r = new HttpResponse("Model name is different!", 409);
        return r;
      }
    } catch (ParseException e) {
      logError("updateModel: exception parsing JSON input: " + e);
      HttpResponse r = new HttpResponse("JSON parsing exception, file not valid!", 500);
      return r;
    } catch (Exception e) {
      logError("updateModel: something went seriously wrong: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Internal server error!", 500);
      return r;
    }

    // call code generation service
    if (this.useCodeGenerationService) {
      try {
        this.invokeServiceMethod("i5.las2peer.services.codeGenerationService.CodeGenerationService",
            "needToDiscussMethodNames", model.getMinifiedRepresentation());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // if this has thrown no exception, we can delete the "old" model and persist the new one
    Connection connection = null;
    try {
      connection = dbm.getConnection();
      // load and delete the old model from the database
      logMessage("updateModel: loading and deleting old model with name " + modelName);
      new Model(modelName, connection).deleteFromDatabase(connection);
      // check if the "old" model did exist
    } catch (ModelNotFoundException e) {
      logMessage("updateModel: there exists no model with name: " + modelName);
      HttpResponse r = new HttpResponse("Model not found!", 404);
      return r;
    } catch (SQLException e) {
      logError("updateModel: error deleting old model: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Internal server error..", 500);
      return r;
    }
    // always close connections
    finally {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    try {
      connection = dbm.getConnection();
      // save the model to the database
      model.persist(connection);
      int modelId = model.getId();
      logMessage("updateModel: model with new id " + modelId + " and name "
          + model.getAttributes().getName() + " stored!");
      HttpResponse r = new HttpResponse("Model updated!", 200);
      return r;
    } catch (SQLException e) {
      logError("updateModel: exception persisting model: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Could not persist, database rejected model!", 500);
      return r;
    } catch (Exception e) {
      logError("updateModel: something went seriously wrong: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Internal server error..", 500);
      return r;
    }
    // always close connections
    finally {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
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
      e.printStackTrace();
    }
    return result;
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // Methods providing a Swagger documentation of the service API.
  ////////////////////////////////////////////////////////////////////////////////////////

  /**
   * 
   * Returns a listing of all annotated top level resources for purposes of the Swagger
   * documentation.
   * 
   * @return listing of all top level resources
   * 
   */
  @GET
  @Path("api-docs")
  @Produces(MediaType.APPLICATION_JSON)
  public HttpResponse getSwaggerResourceListing() {
    return RESTMapper.getSwaggerResourceListing(this.getClass());
  }


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
   * @param tlr A top level resource name.
   * 
   * @return The resource's documentation.
   * 
   */
  @GET
  @Path("api-docs/{tlr}")
  @Produces(MediaType.APPLICATION_JSON)
  public HttpResponse getSwaggerApiDeclaration(@PathParam("tlr") String tlr) {
    HttpResponse res;
    Class<ModelPersistenceService> c = ModelPersistenceService.class;
    if (!c.isAnnotationPresent(Path.class)) {
      res = new HttpResponse("Swagger API declaration not available. Service path is not defined.");
      res.setStatus(404);
    } else {
      Path path = (Path) c.getAnnotation(Path.class);
      String endpoint = webconnectorProtocol + "://" + webconnectorIpAdress + ":" + webconnectorPort
          + path.value() + "/";
      System.out.println(endpoint);
      res = RESTMapper.getSwaggerApiDeclaration(this.getClass(), tlr, endpoint);
    }
    return res;
  }
}
