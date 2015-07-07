package i5.las2peer.services.modelPersistenceService;

import java.sql.Connection;
import java.sql.SQLException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.Consumes;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.POST;
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


  public ModelPersistenceService() {
    // read and set properties values
    setFieldValues();
    // instantiate a database manager to handle database connection pooling and credentials
    dbm = new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass, jdbcUrl, jdbcSchema);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // Service methods.
  ////////////////////////////////////////////////////////////////////////////////////////

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
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ResourceListApi(description = "Entry point for storing a model to the database.")
  @ApiResponses(value = {@ApiResponse(code = 201, message = "OK, model stored"),
      @ApiResponse(code = 409,
          message = "Tried to save a model that already had a name and thus was not new."),
      @ApiResponse(code = 500, message = "Internal server error")})
  @Summary("Entry point for storing a model to the database.")
  public HttpResponse postModel(@ContentParam String inputModel) {
    logMessage("trying to store new model");
    Model model;
    try {
      // create the model
      model = new Model(inputModel);
    } catch (ParseException e) {
      logError("Exception parsing JSON input: " + e);
      HttpResponse r = new HttpResponse("JSON parsing exception, file not valid!");
      r.setStatus(500);
      return r;
    } catch (Exception e) {
      logError("Something got seriously wrong: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Internal server error..");
      r.setStatus(500);
      return r;
    }

    // check if model name is already taken
    if (this.getModel(model.getAttributes().getName()).getStatus() != 404) {
      logMessage(
          "model name " + model.getAttributes().getName() + " is already taken, cannot store");
      HttpResponse r = new HttpResponse(
          "Model with name " + model.getAttributes().getName() + " already exists!");
      r.setStatus(409);
      return r;
    }
    // save the model to the database
    try {
      Connection connection = dbm.getConnection();
      model.persist(connection);
      int modelId = model.getId();
      logMessage(
          "Model with id " + modelId + " and name " + model.getAttributes().getName() + " stored!");
      HttpResponse r = new HttpResponse("Model stored!");
      r.setStatus(201);
      return r;
    } catch (SQLException e) {
      logError("Exception persisting model: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Could not persist, database rejected model!");
      r.setStatus(500);
      return r;
    } catch (Exception e) {
      logError("Something got seriously wrong: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Internal server error..");
      r.setStatus(500);
      return r;
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
  @Consumes(MediaType.TEXT_PLAIN)
  @ResourceListApi(
      description = "Searches for a model in the database. Takes the modelName as search parameter")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK, model found"),
      @ApiResponse(code = 404, message = "Model could not be found."),
      @ApiResponse(code = 500, message = "Internal server error")})
  @Summary("Searches for a model in the database.")
  public HttpResponse getModel(@PathParam("modelName") String modelName) {
    logMessage("searching for model with name " + modelName);
    Model model = null;
    Connection connection;
    try {
      connection = dbm.getConnection();
      model = new Model(modelName, connection);
    } catch (ModelNotFoundException e) {
      logMessage("did not find model with name " + modelName);
      HttpResponse r = new HttpResponse("Model not found!");
      r.setStatus(404);
      return r;
    } catch (SQLException e) {
      logError("Exception fetching model: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Could not persist, database rejected model!");
      r.setStatus(500);
      return r;
    } catch (Exception e) {
      logError("Something got seriously wrong: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Could not persist, database rejected model!");
      r.setStatus(500);
      return r;
    }
    logMessage("found model " + modelName + ", now converting to JSONObject and returning");
    JSONObject jsonModel = model.toJSONObject();

    HttpResponse r = new HttpResponse(jsonModel.toJSONString(), 200);
    return r;
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // Methods required by the LAS2peer framework.
  ////////////////////////////////////////////////////////////////////////////////////////

  /**
   * This method is needed for every RESTful application in LAS2peer. There is no need to change!
   * 
   * @return the mapping
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
   * Returns a listing of all annotated top level resources for purposes of the Swagger
   * documentation.
   * 
   * Note: If you do not intend to use Swagger for the documentation of your Service API, this
   * method may be removed.
   * 
   * @return Listing of all top level resources.
   */
  @GET
  @Path("api-docs")
  @Produces(MediaType.APPLICATION_JSON)
  public HttpResponse getSwaggerResourceListing() {
    return RESTMapper.getSwaggerResourceListing(this.getClass());
  }

  /**
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
   * @return The resource's documentation.
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
