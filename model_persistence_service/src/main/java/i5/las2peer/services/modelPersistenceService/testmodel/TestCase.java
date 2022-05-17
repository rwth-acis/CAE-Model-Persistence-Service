package i5.las2peer.services.modelPersistenceService.testmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Represents one test case of a test suite.
 * @author Philipp
 *
 */
public class TestCase {
	
	/**
	 * Id of the test case.
	 * Different versions of the same test case have the same id.
	 */
	private int id;
	
	/**
	 * Id of the test model, that the test case belongs to.
	 * Different versions of the same test case have different model ids.
	 */
	private int modelId;
	
	/**
	 * Name of the test case.
	 */
	private String name;
	
	/**
	 * List of requests that are part of the test case.
	 */
	private List<TestRequest> requests;
	
	/**
	 * Creates a TestCase object given a JSONObject representing it.
	 * @param testCase JSON representation of the test case.
	 */
	public TestCase(JSONObject testCase) {
		this.id = (int) ((long) testCase.get("id"));
		this.name = (String) testCase.get("name");
		
		// get requests
		JSONArray requestsJSON = (JSONArray) testCase.get("requests");
		this.requests = new ArrayList<>();
		for(Object requestObj : requestsJSON) {
			JSONObject request = (JSONObject) requestObj;
			// create TestRequest from JSONObject
			this.requests.add(new TestRequest(request, this.id));
		}
	}
	
	/**
	 * Stores the current TestCase (and its requests) to the database.
	 * @param connection
	 * @param modelId Id of the TestModel, that the test case belongs to.
	 * @throws SQLException If storing the test case or one of its requests failed.
	 */
	public void persist(Connection connection, int modelId) throws SQLException {
		this.modelId = modelId;
		
		PreparedStatement statement = connection.prepareStatement("INSERT INTO TestCase (testCaseId, modelId, name) VALUES (?,?,?);");
		statement.setInt(1, this.id);
		statement.setInt(2, this.modelId);
		statement.setString(3, this.name);
		statement.executeUpdate();
		statement.close();
		
		// persist requests
		for(TestRequest request : this.requests) {
			request.persist(connection, this.modelId);
		}
	}
}
