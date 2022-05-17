package i5.las2peer.services.modelPersistenceService.testmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 * Stores the test cases that are part of a test suite.
 * @author Philipp
 *
 */
public class TestModel {
	
	/**
	 * Id of the test model.
	 * Different versions of the same test suite have different ids.
	 */
	private int id;
	
	/**
	 * List of test cases that are part of the test suite.
	 */
	private List<TestCase> testCases;

	/**
	 * Given a JSON representation of the test suite, this constructor
	 * parses it and creates an object for it.
	 * @param jsonTestModel JSON representation of the test suite, given as a String.
	 * @throws ParseException If parsing the given JSON String failed.
	 */
	public TestModel(String jsonTestModel) throws ParseException {
		// parse JSON string
		JSONObject testModel = (JSONObject) JSONValue.parseWithException(jsonTestModel);
		
		// get test cases 
		JSONArray testCasesJSON = (JSONArray) testModel.get("testCases");
		this.testCases = new ArrayList<>();
		for (Object testCaseObj : testCasesJSON) {
			JSONObject testCase = (JSONObject) testCaseObj;
			// create test case from JSONObject
			this.testCases.add(new TestCase(testCase));
		}
	}

	/**
	 * Stores the current TestModel (and its test cases) to the database.
	 * @param connection 
	 * @throws SQLException If storing the test model or one of its test cases failed.
	 */
	public void persist(Connection connection) throws SQLException {
		// store current value of auto commit
		boolean autoCommitBefore = connection.getAutoCommit();

		// disable auto commit
		connection.setAutoCommit(false);

		try {
			// insert into TestModel to get id of model
			PreparedStatement statement = connection.prepareStatement("INSERT INTO TestModel () VALUES ();",
					Statement.RETURN_GENERATED_KEYS);
			statement.executeUpdate();

			// get id of model
			ResultSet genKeys = statement.getGeneratedKeys();
			genKeys.next();
			this.id = genKeys.getInt(1);
			statement.close();

			// persist test cases
			for (TestCase testCase : this.testCases) {
				testCase.persist(connection, this.id);
			}
			
			// no errors occurred, so commit
		    connection.commit();
		} catch (SQLException e) {
			throw e;
		} finally {
			// reset auto commit to previous value
			connection.setAutoCommit(autoCommitBefore);
		}
	}
}
