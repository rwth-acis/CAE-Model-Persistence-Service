package i5.las2peer.services.modelPersistenceService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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
import i5.las2peer.services.modelPersistenceService.model.Model;
import i5.las2peer.services.modelPersistenceService.models.edge.Edge;
import i5.las2peer.services.modelPersistenceService.models.modelAttribute.ModelAttributes;
import i5.las2peer.services.modelPersistenceService.models.node.Node;

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
   * Entry point for all new models.
   * 
   * @param inputModel the model as a JSON string
   * 
   * @return HttpResponse containing the status code of the request
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

    // Generate a new (first only temporary) model
    Model model;
    Node[] nodes;
    Edge[] edges;
    ModelAttributes attributes;

    try {
      // take the whole model, then parse it into model-attributes, nodes and edges
      JSONObject completeJsonModel = (JSONObject) JSONValue.parseWithException(inputModel);
      JSONObject jsonAttribute = (JSONObject) completeJsonModel.get("attributes");
      attributes = new ModelAttributes(jsonAttribute);
      // check for default model name
      if (!attributes.getName().equals("NEW")) {
        // HttpResponse r = new HttpResponse("Model not new, name has to be 'NEW'");
        // r.setStatus(409);
        // return r;
      }
      // resolve nodes and edges now
      JSONObject jsonNodes = (JSONObject) completeJsonModel.get("nodes");
      JSONObject jsonEdges = (JSONObject) completeJsonModel.get("edges");

      nodes = new Node[jsonNodes.size()];
      edges = new Edge[jsonEdges.size()];

      int index = 0;
      @SuppressWarnings("unchecked")
      Iterator<Map.Entry<String, Object>> nodesEntries = jsonNodes.entrySet().iterator();

      while (nodesEntries.hasNext()) {
        Map.Entry<String, Object> entry = nodesEntries.next();
        String key = entry.getKey();
        JSONObject value = (JSONObject) entry.getValue();
        nodes[index] = new Node(key, value);
        index++;
      }

      index = 0;
      @SuppressWarnings("unchecked")
      Iterator<Map.Entry<String, Object>> edgesEntries = jsonEdges.entrySet().iterator();

      while (edgesEntries.hasNext()) {
        Map.Entry<String, Object> entry = edgesEntries.next();
        String key = entry.getKey();
        JSONObject value = (JSONObject) entry.getValue();
        edges[index] = new Edge(key, value);
        index++;
      }

    } catch (Exception e) {
      logError("Exception parsing JSON input: " + e);
      HttpResponse r = new HttpResponse("JSON parsing exception, file not valid!");
      r.setStatus(500);
      return r;
    }

    // create the model
    model = new Model(attributes, nodes, edges);
    // save the model to the database
    try {
      Connection connection = dbm.getConnection();
      int modelId = model.persist(connection);
      logMessage(
          "Model with id " + modelId + " and name " + model.getAttributes().getName() + " stored!");
      HttpResponse r = new HttpResponse("Model stored!");
      r.setResult(model.toJSONObject().toJSONString());
      r.setStatus(201);
      return r;
    } catch (SQLException e) {
      logError("Exception persisting model: " + e);
      e.printStackTrace();
      HttpResponse r = new HttpResponse("Could not persist, database rejected model!");
      r.setStatus(500);
      return r;
    }

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
