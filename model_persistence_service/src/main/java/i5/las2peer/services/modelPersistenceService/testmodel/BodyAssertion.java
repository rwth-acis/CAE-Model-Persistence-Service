package i5.las2peer.services.modelPersistenceService.testmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.json.simple.JSONObject;

/**
 * Assertion on the body of the response.
 * @author Philipp
 *
 */
public class BodyAssertion extends RequestAssertion {
	
	private static final int ASSERTION_TYPE_ID = 1;
	
	/**
	 * First operator of the assertion.
	 */
	private BodyAssertionOperator operator;
	
	public BodyAssertion(int id, int testRequestId, JSONObject operator) {
		super(id, testRequestId, ASSERTION_TYPE_ID);
		
		this.operator = new BodyAssertionOperator(operator);
	}
	
	/**
	 * Stores the part of the assertion that is specific for the BodyAssertion to the database.
	 */
	public void persist(Connection connection, int modelId) throws SQLException {
		super.persist(connection, modelId);
		
		PreparedStatement statement = connection.prepareStatement("INSERT INTO BodyAssertion (modelId, requestAssertionId, operatorId) VALUES (?,?,?);");
		statement.setInt(1, modelId);
		statement.setInt(2, this.id);
		statement.setInt(3, this.operator.getId());
		
		statement.executeUpdate();
		statement.close();
		
		// also persist the operator
		operator.persist(connection, modelId);
	}
}
