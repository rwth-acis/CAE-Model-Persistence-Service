package i5.las2peer.services.modelPersistenceService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.PrintStream;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.WebConnector;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;


/**
 * Example Test Class demonstrating a basic JUnit test structure.
 * 
 * 
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


  /**
   * Called before the tests start.
   * 
   * Sets up the node and initializes connector and users that can be used throughout the tests.
   * 
   * @throws Exception
   */
  @BeforeClass
  public static void startServer() throws Exception {

    // start node
    node = LocalNode.newNode();
    node.storeAgent(MockAgentFactory.getAdam());
    node.launch();

    ServiceAgent testService = ServiceAgent.generateNewAgent(testTemplateService, "a pass");
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
   * Called after the tests have finished. Shuts down the server and prints out the connector log
   * file for reference.
   * 
   * @throws Exception
   */
  @AfterClass
  public static void shutDownServer() throws Exception {

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
      String FILE_NAME = "exampleModels\\example_microservice_model.json";
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
      ClientResponse result = c.sendRequest("POST", mainPath, payload.toJSONString(),
          MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, new Pair[] {});
      assertEquals(201, result.getHttpCode());
      System.out.println("Result of 'testModelPosting': " + result.getResponse().trim());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception: " + e);
    }

  }


  /**
   * Test the TemplateService for valid rest mapping. Important for development.
   */
  @Test
  public void testDebugMapping() {
    ModelPersistenceService cl = new ModelPersistenceService();
    assertTrue(cl.debugMapping());
  }
}
