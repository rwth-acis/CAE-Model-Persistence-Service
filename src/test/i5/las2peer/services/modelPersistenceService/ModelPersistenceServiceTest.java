package i5.las2peer.services.modelPersistenceService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.modelPersistenceService.database.DatabaseManager;
import i5.las2peer.services.modelPersistenceService.model.Model;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.WebConnector;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;


/**
 * 
 * Central test class for the CAE-Model-Persistence-Service. Only tests on a service interface /
 * REST level.
 *
 */
public class ModelPersistenceServiceTest {

  private static final String HTTP_ADDRESS = "http://127.0.0.1";
  private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

  private static LocalNode node;
  private static WebConnector connector;
  private static ByteArrayOutputStream logStream;

  private static UserAgent testAgent;
  private static final String testPass = "adamspass";

  private static final String testTemplateService =
      ModelPersistenceService.class.getCanonicalName();

  private static final String mainPath = "CAE/models/";

  private static Connection connection;
  private static Model testModel1;
  private static Model testModel2;
  private static Model testModel1_updated;

  /**
   * Called before the tests start.
   * 
   * First initializes a database connection according to the service property file. Then adds some
   * data to the database. Sets up the node and initializes connector and users that can be used
   * throughout the tests.
   * 
   * @throws Exception
   */
  @BeforeClass
  public static void startServer() throws Exception {
    // paths to properties and models
    Properties properties = new Properties();
    String propertiesFile =
        "./etc/i5.las2peer.services.modelPersistenceService.ModelPersistenceService.properties";
    String FILE_NAME1 = "./exampleModels/example_microservice_model_1.json";
    String FILE_NAME1_UPDATED = "./exampleModels/example_microservice_model_1_updated.json";
    String FILE_NAME2 = "./exampleModels/example_microservice_model_2.json";

    String jsonModel1 = null;
    String jsonModel1_updated = null;
    String jsonModel2 = null;
    String jdbcDriverClassName = null;
    String jdbcUrl = null;
    String jdbcSchema = null;
    String jdbcLogin = null;
    String jdbcPass = null;

    // load properties and models
    try {
      FileReader reader = new FileReader(propertiesFile);
      properties.load(reader);

      jdbcDriverClassName = properties.getProperty("jdbcDriverClassName");
      jdbcUrl = properties.getProperty("jdbcUrl");
      jdbcSchema = properties.getProperty("jdbcSchema");
      jdbcLogin = properties.getProperty("jdbcLogin");
      jdbcPass = properties.getProperty("jdbcPass");

      JSONParser parser = new JSONParser();

      jsonModel1 = ((JSONObject) parser.parse(new FileReader(FILE_NAME1))).toJSONString();
      jsonModel1_updated =
          ((JSONObject) parser.parse(new FileReader(FILE_NAME1_UPDATED))).toJSONString();

      jsonModel2 = ((JSONObject) parser.parse(new FileReader(FILE_NAME2))).toJSONString();

    } catch (Exception e) {
      e.printStackTrace();
      fail("File loading problems: " + e);
    }

    DatabaseManager databaseManager =
        new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass, jdbcUrl, jdbcSchema);
    connection = databaseManager.getConnection();
    connection.setAutoCommit(false);
    // now add some models (model 1 and 2, not the updated one
    // read in (test-)model
    testModel1 = new Model(jsonModel1);
    testModel1_updated = new Model(jsonModel1_updated);
    testModel2 = new Model(jsonModel2);
    testModel1.persist(connection);
    testModel2.persist(connection);
    connection.commit();

    // start node
    node = LocalNode.newNode();
    node.storeAgent(MockAgentFactory.getAdam());
    node.launch();

    ServiceAgent testService = ServiceAgent.createServiceAgent(testTemplateService, "a pass");
    testService.unlockPrivateKey("a pass");

    node.registerReceiver(testService);

    // start connector
    logStream = new ByteArrayOutputStream();

    connector = new WebConnector(true, HTTP_PORT, false, 1000);
    connector.setLogStream(new PrintStream(logStream));
    connector.start(node);
    Thread.sleep(1000); // wait a second for the connector to become ready
    testAgent = MockAgentFactory.getAdam();

    connector.updateServiceList();
    // avoid timing errors: wait for the repository manager to get all services before continuing
    try {
      System.out.println("waiting..");
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  /**
   * Called after the tests have finished. Deletes test-data, shuts down the server and prints out
   * the connector log file for reference.
   * 
   * @throws Exception
   */
  @AfterClass
  public static void shutDownServer() throws Exception {

    // delete the whole (test-)database manually, just to be sure that no remains are left
    PreparedStatement statement;
    try {
      statement = connection.prepareStatement("DELETE FROM model;");
      statement.executeUpdate();
      statement = connection.prepareStatement("DELETE FROM modelAttributes;");
      statement.executeUpdate();
      statement = connection.prepareStatement("DELETE FROM attribute;");
      statement.executeUpdate();
      statement = connection.prepareStatement("DELETE FROM edge;");
      statement.executeUpdate();
      statement = connection.prepareStatement("DELETE FROM node;");
      statement.executeUpdate();
      connection.commit();
    } finally {
      connection.close();
    }

    connector.stop();
    node.shutDown();

    connector = null;
    node = null;

    LocalNode.reset();

    System.out.println("Connector-Log:");
    System.out.println("--------------");

    System.out.println(logStream.toString());

  }

  /**
   * 
   * A basic test for the model posting mechanism.
   * 
   */
  @Test
  public void testModelPosting() {

    JSONObject payload = null;
    MiniClient c = new MiniClient();
    c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

    // read in (test-)model
    try {
      JSONParser parser = new JSONParser();
      String FILE_NAME = "./exampleModels/example_microservice_model_3.json";
      Object obj;
      obj = parser.parse(new FileReader(FILE_NAME));
      payload = (JSONObject) obj;
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception: " + e);
    }

    // test method
    try {
      c.setLogin(Long.toString(testAgent.getId()), testPass);
      @SuppressWarnings("unchecked")
      ClientResponse result = c.sendRequest("POST", mainPath + "", payload.toJSONString(),
          MediaType.APPLICATION_JSON, "", new Pair[] {});
      assertEquals(201, result.getHttpCode());
      System.out.println("Result of 'testModelPosting': " + result.getResponse().trim());
      Model model = new Model(payload.toJSONString());
      model.deleteFromDatabase(connection);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception: " + e);
    }

  }

  /**
   * 
   * A basic test for the model deleting mechanism. First (manually) persists a model in the
   * database, then deletes it via RESTful service access and queries the service then again for the
   * model, which should return a NOT_FOUND then.
   * 
   */

  @SuppressWarnings("unchecked")
  @Test
  public void testModelDeletion() {

    JSONObject payload = null;
    MiniClient c = new MiniClient();
    c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

    // persist (test-)model first
    try {
      JSONParser parser = new JSONParser();
      String FILE_NAME = "./exampleModels/example_microservice_model_3.json";
      Object obj;
      obj = parser.parse(new FileReader(FILE_NAME));
      payload = (JSONObject) obj;
      Model model = new Model(payload.toJSONString());
      model.persist(connection);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception: " + e);
    }

    // test method
    try {
      c.setLogin(Long.toString(testAgent.getId()), testPass);
      ClientResponse result = c.sendRequest("DELETE", mainPath + "Third%20Model", "",
          MediaType.TEXT_PLAIN, "", new Pair[] {});
      assertEquals(200, result.getHttpCode());
      System.out.println("Result of 'testModelDeletion': " + result.getResponse().trim());

      // should return 404 now
      result = c.sendRequest("GET", mainPath + "Third%20Model", "", MediaType.TEXT_PLAIN,
          MediaType.APPLICATION_JSON, new Pair[] {});
      assertEquals(404, result.getHttpCode());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception: " + e);
    }

  }

  /**
   * 
   * A basic test for the model retrieving mechanism.
   * 
   */
  @Test
  public void testModelRetrieving() {

    MiniClient c = new MiniClient();
    c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

    // test method
    try {
      c.setLogin(Long.toString(testAgent.getId()), testPass);
      @SuppressWarnings("unchecked")
      ClientResponse result = c.sendRequest("GET", mainPath + "First%20Model", "",
          MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, new Pair[] {});
      assertEquals(200, result.getHttpCode());
      System.out.println("Result of 'testModelRetrieving': " + result.getResponse().trim());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception: " + e);
    }

  }

  /**
   * 
   * A basic test for the model list retrieving mechanism.
   * 
   */
  @Test
  public void testModelListRetrieving() {

    MiniClient c = new MiniClient();
    c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

    // test method
    try {
      c.setLogin(Long.toString(testAgent.getId()), testPass);
      @SuppressWarnings("unchecked")
      ClientResponse result =
          c.sendRequest("GET", mainPath, "", "", MediaType.APPLICATION_JSON, new Pair[] {});
      assertEquals(200, result.getHttpCode());
      System.out.println("Result of 'testModelListRetrieving': " + result.getResponse().trim());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception: " + e);
    }

  }

  /**
   * 
   * A basic test for the model update mechanism. Updates the "testModel1" with the values of
   * "testModel1_updated". Does NOT revert these changes.
   * 
   */
  @Test
  public void testModelUpdate() {

    JSONObject payload = testModel1_updated.toJSONObject();
    MiniClient c = new MiniClient();
    c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

    // test method
    try {
      c.setLogin(Long.toString(testAgent.getId()), testPass);
      @SuppressWarnings("unchecked")
      ClientResponse result = c.sendRequest("PUT", mainPath + "First%20Model",
          payload.toJSONString(), MediaType.APPLICATION_JSON, "", new Pair[] {});
      assertEquals(200, result.getHttpCode());
      System.out.println("Result of 'testModelUpdate': " + result.getResponse().trim());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception: " + e);
    }

  }

  /**
   * Test the persistence service for valid rest mapping. Important for development.
   */
  @Test
  public void testDebugMapping() {
    ModelPersistenceService cl = new ModelPersistenceService();
    String XML_LOCATION = "./restMapping.xml";
    String xml = cl.getRESTMapping();

    try {
      RESTMapper.writeFile(XML_LOCATION, xml);
    } catch (IOException e) {
      e.printStackTrace();
    }

    XMLCheck validator = new XMLCheck();
    ValidationResult result = validator.validate(xml);

    if (!result.isValid())
      fail();
  }

}
