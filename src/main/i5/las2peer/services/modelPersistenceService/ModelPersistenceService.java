package i5.las2peer.services.modelPersistenceService;

import java.io.IOException;
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
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.services.modelPersistenceService.database.DatabaseManager;
import i5.las2peer.services.modelPersistenceService.models.ModelAttributes;
import i5.las2peer.services.modelPersistenceService.models.edges.Edge;
import i5.las2peer.services.modelPersistenceService.models.nodes.Node;

/**
 * CAE Model Persistence Service
 * 
 * A LAS2peer service used for persisting (and validating) application models. Part of the CAE.
 * 
 */
@Path("models")
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
   * @param inputModel the model as a string
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
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 404, message = "User not found"),
      @ApiResponse(code = 500, message = "Internal server error")})
  @Summary("Entry point for storing a model to the database.")
  public HttpResponse postModel(@ContentParam String inputModel) {



    // take the whole model, then parse it into model-attributes, nodes and edges
    JSONObject completeModel = (JSONObject) JSONValue.parse(inputModel);

    JSONObject jsonAttributes = (JSONObject) completeModel.get("attributes");

    // let's see what we got here..
    ModelAttributes attributes = new ModelAttributes(jsonAttributes);

    // TODO: big if case switch thing here, to see what we are dealing with (application, frontend
    // component or microservice)

    // a new home for nodes and edges
    Node[] nodes;
    Edge[] edges;

    try {
      JSONObject jsonNodes = (JSONObject) completeModel.get("nodes");
      JSONObject jsonEdges = (JSONObject) completeModel.get("edges");

      int numberOfNodes = jsonNodes.size();
      int numberofEdges = jsonEdges.size();
      System.out.println("nodes size: " + numberOfNodes);
      nodes = new Node[numberOfNodes];
      System.out.println("edges size: " + jsonEdges.size());
      edges = new Edge[numberofEdges];

      @SuppressWarnings("unchecked")
      Iterator<Map.Entry<String, Object>> nodesEntries = jsonNodes.entrySet().iterator();
      int index = 0;
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
        System.out.println(edges[index].toJSONString());
        index++;
      }

    } catch (Exception e) {
      System.out.println("Exception parsing JSON input: " + e);
    }
    HttpResponse r = new HttpResponse(completeModel.toJSONString());
    r.setStatus(201);
    return r;

  }


  ////////////////////////////////////////////////////////////////////////////////////////
  // Methods required by the LAS2peer framework.
  ////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Method for debugging purposes. Here the concept of restMapping validation is shown. It is
   * important to check, if all annotations are correct and consistent. Otherwise the service will
   * not be accessible by the WebConnector. Best to do it in the unit tests. To avoid being
   * overlooked/ignored the method is implemented here and not in the test section.
   * 
   * @return true, if mapping correct
   */
  public boolean debugMapping() {
    String XML_LOCATION = "./restMapping.xml";
    String xml = getRESTMapping();

    try {
      RESTMapper.writeFile(XML_LOCATION, xml);
    } catch (IOException e) {
      e.printStackTrace();
    }

    XMLCheck validator = new XMLCheck();
    ValidationResult result = validator.validate(xml);

    if (result.isValid())
      return true;
    return false;
  }

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
