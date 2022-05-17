package i5.las2peer.services.modelPersistenceService.testmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Represents one request of a test case.
 * @author Philipp
 *
 */
public class TestRequest {
	
	/**
	 * Id of the request.
	 * Different versions of the same request have the same id.
	 */
	private int id;
	
	/**
	 * Id of the test model, that the request belongs to.
	 * Different versions of the same request have different model ids.
	 */
	private int modelId;
	
	/**
	 * Id of the test case, that the request belongs to.
	 */
	private int testCaseId;
	
	/**
	 * Request type, e.g., "GET".
	 */
	private String type;
	
	/**
	 * Request URL.
	 */
	private String url;
	
	/**
	 * Agent that was chosen for authorization.
	 * -1 means no authorization.
	 */
	private int authSelectedAgent = -1;
	
	/**
	 * Request body.
	 */
	private String body;
	
	/**
	 * Response assertions.
	 */
	private List<RequestAssertion> assertions;
	
	/**
	 * Creates a TestRequest object given a JSONObject representing it and the id of the test case.
	 * @param request JSON representation of the request.
	 * @param testCaseId Id of the test case that the request belongs to.
	 */
	public TestRequest(JSONObject request, int testCaseId) {
		this.id = (int) ((long) request.get("id"));
		this.testCaseId = testCaseId;
		this.type = (String) request.get("type");
		this.url = (String) request.get("url");
		
		// check if auth is enabled
		if(request.containsKey("auth")) {
			JSONObject auth = (JSONObject) request.get("auth");
			if(auth.containsKey("selectedAgent")) {
				this.authSelectedAgent = (int) ((long) auth.get("selectedAgent"));
			}
		}
		
		// get assertions
		this.assertions = new ArrayList<>();
		JSONArray assertionsJSON = (JSONArray) request.get("assertions");
		for(Object assertionJSON : assertionsJSON) {
			JSONObject assertion = (JSONObject) assertionJSON;
			// create RequestAssertion from the JSONObject
			this.assertions.add(RequestAssertion.fromJSONObject(assertion, this.id));
		}
	}
	
	/**
	 * Stores the current TestRequest (and its assertions) to the database.
	 * @param connection
	 * @param modelId Id of the model, that the request belongs to.
	 * @throws SQLException If storing the request or one of its assertions failed.
	 */
	public void persist(Connection connection, int modelId) throws SQLException {
		this.modelId = modelId;
		
		String statementStr = "INSERT INTO TestRequest (testRequestId, modelId, testCaseId, type, url) VALUES (?,?,?,?,?);";
		if(this.isAuthEnabled()) {
			statementStr = "INSERT INTO TestRequest (testRequestId, modelId, testCaseId, type, url, authSelectedAgent) VALUES (?,?,?,?,?,?);";
		}
		PreparedStatement statement = connection.prepareStatement(statementStr);
		statement.setInt(1, this.id);
		statement.setInt(2, this.modelId);
		statement.setInt(3, this.testCaseId);
		statement.setString(4, this.type);
		statement.setString(5, this.url);
		if(this.isAuthEnabled()) {
			statement.setInt(6, this.authSelectedAgent);
		}
		statement.executeUpdate();
		statement.close();
		
		// persist assertions
		for(RequestAssertion assertion : this.assertions) {
			assertion.persist(connection, this.modelId);
		}
	}
	
	/**
	 * Whether authorization is enabled for the request.
	 * @return Whether authorization is enabled for the request.
	 */
	public boolean isAuthEnabled() {
		return this.authSelectedAgent != -1;
	}
}
