package i5.las2peer.services.modelPersistenceService.testmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.json.simple.JSONObject;

/**
 * Assertion on the status code of the response.
 * @author Philipp
 *
 */
public class StatusCodeAssertion extends RequestAssertion {
	
	private static final int ASSERTION_TYPE_ID = 0;
	
	private int comparisonOperator;
	
	/**
	 * Status code that gets compared to the response status code.
	 */
	private int statusCodeValue;
	
	public StatusCodeAssertion(int id, int testRequestId, JSONObject operator) {
		super(id, testRequestId, ASSERTION_TYPE_ID);
		
		this.comparisonOperator = (int) ((long) operator.get("id"));
		this.statusCodeValue = (int) ((long) ((JSONObject) operator.get("input")).get("value"));
	}
	
	/**
	 * Stores the part of the assertion that is specific for the StatusCodeAssertion to the database.
	 */
	public void persist(Connection connection, int modelId) throws SQLException {
		super.persist(connection, modelId);
		
		PreparedStatement statement = connection.prepareStatement("INSERT INTO StatusCodeAssertion (modelId, requestAssertionId, comparisonOperator, statusCodeValue) VALUES (?,?,?,?);");
		statement.setInt(1, modelId);
		statement.setInt(2, this.id);
		statement.setInt(3, this.comparisonOperator);
		statement.setInt(4, this.statusCodeValue);
		
		statement.executeUpdate();
		statement.close();
	}
}
