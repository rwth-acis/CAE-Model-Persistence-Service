package i5.las2peer.services.modelPersistenceService.testmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.json.simple.JSONObject;

/**
 * Represents a part of a BodyAssertion.
 * @author Philipp
 *
 */
public class BodyAssertionOperator {
	
	/**
	 * Id of the operator.
	 * Different versions of the same operator have the same id.
	 */
	private int id;
	
	/**
	 * Id of the test model, that the operator belongs to.
	 * Different versions of the same operator have different model ids.
	 */
	private int modelId;
	
	/**
	 * Id of the input for the operator, e.g., no input, input field or a data type.
	 */
	private int inputType;
	
	/**
	 * Value that has been entered or selected.
	 */
	private String inputValue;
	
	/**
	 * The following operator, if there exists one.
	 */
	private BodyAssertionOperator followedByOperator = null;
	
	/**
	 * Creates a BodyAssertionOperator object given its JSON representation.
	 * @param operator JSON representation of the operator.
	 */
	public BodyAssertionOperator(JSONObject operator) {
		this.id = (int) ((long) operator.get("id"));
		
		// get operator input
		JSONObject input = (JSONObject) operator.get("input");
		this.inputType = (int) ((long) input.get("id"));
		this.inputValue = (String) input.get("value");
		
		// check if operator has a following operator
		if(operator.containsKey("followedBy")) {
			this.followedByOperator = new BodyAssertionOperator((JSONObject) operator.get("followedBy"));
		}
	}
	
	/**
	 * Stores the current operator (and its following operator, if there exists one) to the database.
	 * @param connection
	 * @param modelId Id of the model, that the operator belongs to.
	 * @throws SQLException If storing the operator failed.
	 */
	public void persist(Connection connection, int modelId) throws SQLException {
		this.modelId = modelId;
		
		String statementStr = "INSERT INTO BodyAssertionOperator (operatorId, modelId, inputType, inputValue) VALUES (?,?,?,?);";
		if(this.hasFollowingOperator()) {
			statementStr = "INSERT INTO BodyAssertionOperator (operatorId, modelId, inputType, inputValue, followedBy) VALUES (?,?,?,?,?);";
		}
		
		PreparedStatement statement = connection.prepareStatement(statementStr);
		statement.setInt(1, this.id);
		statement.setInt(2, this.modelId);
		statement.setInt(3, this.inputType);
		statement.setString(4, this.inputValue);
		if(this.hasFollowingOperator()) {
			statement.setInt(5, this.followedByOperator.getId());
		}
		
		statement.executeUpdate();
		statement.close();
		
		// persist following operator if there exists one
		if(this.hasFollowingOperator()) {
			this.followedByOperator.persist(connection, modelId);
		}
	}
	
	/**
	 * Whether the current operator is followed by another operator.
	 * @return Whether the current operator is followed by another operator.
	 */
	public boolean hasFollowingOperator() {
		return this.followedByOperator != null;
	}
	
	public int getId() {
		return this.id;
	}
}
