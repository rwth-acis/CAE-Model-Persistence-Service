package i5.las2peer.services.modelPersistenceService.testmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.json.simple.JSONObject;

/**
 * Represents an assertion which can either be a StatusCodeAssertion
 * or BodyAssertion.
 * @author Philipp
 *
 */
public class RequestAssertion {
	
	/**
	 * Id of the assertion.
	 * Different versions of the same assertion have the same id.
	 */
	protected int id;
	
	/**
	 * Id of the test model, that the assertion belongs to.
	 * Different versions of the same assertion have different model ids.
	 */
	private int modelId;
	
	/**
	 * Id of the request, that the assertion belongs to.
	 */
	private int testRequestId;
	
	/**
	 * Type of the assertion.
	 * 0: StatusCodeAssertion
	 * 1: BodyAssertion
	 */
	private int assertionType;
	
	public RequestAssertion(int id, int testRequestId, int assertionType) {
		this.id = id;
		this.testRequestId = testRequestId;
		this.assertionType = assertionType;
	}
	
	/**
	 * Creates the correct RequestAssertion (either StatusCodeAssertion or
	 * BodyAssertion) for the given JSONObject.
	 * @param assertion JSON representation of the assertion.
	 * @param testRequestId Id of the request, that the assertion belongs to.
	 * @return RequestAssertion object.
	 */
	public static RequestAssertion fromJSONObject(JSONObject assertion, int testRequestId) {
		int id = (int) ((long) assertion.get("id"));
		int assertionType = (int) ((long) assertion.get("assertionType"));
		
		JSONObject operator = (JSONObject) assertion.get("operator");
		
		if(assertionType == 0) {
			return new StatusCodeAssertion(id, testRequestId, operator);
		} else {
			return new BodyAssertion(id, testRequestId, operator);
		}
	}
	
	/**
	 * Stores the current assertion to the database.
	 * @param connection
	 * @param modelId Id of the model, that the assertion belongs to.
	 * @throws SQLException If storing the assertion failed.
	 */
	public void persist(Connection connection, int modelId) throws SQLException {
		this.modelId = modelId;
		
		PreparedStatement statement = connection.prepareStatement("INSERT INTO RequestAssertion (requestAssertionId, modelId, testRequestId, assertionType) VALUES (?,?,?,?);");
		statement.setInt(1, this.id);
		statement.setInt(2, this.modelId);
		statement.setInt(3, this.testRequestId);
		statement.setInt(4, this.assertionType);
		
		statement.executeUpdate();
		statement.close();
	}
}
