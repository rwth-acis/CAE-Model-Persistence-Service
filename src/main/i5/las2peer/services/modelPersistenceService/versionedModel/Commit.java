package i5.las2peer.services.modelPersistenceService.versionedModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import i5.las2peer.services.modelPersistenceService.exception.CommitNotFoundException;
import i5.las2peer.services.modelPersistenceService.model.Model;

public class Commit {

	/**
	 * Id of the commit is set to -1 before the commit gets persisted.
	 */
	private int id = -1;
	
	/**
	 * The model which gets described at a specific state by the commit.
	 */
	private Model model;
	
	/**
	 * Commit message that gets stored together with the commit.
	 */
	private String message;

	/**
	 * The timestamp of when the commit got created.
	 */
	private String timestamp;
	
	public Commit(String jsonCommit, boolean commitForUncommitedChanges) throws ParseException {
		JSONObject completeJsonCommit = (JSONObject) JSONValue.parseWithException(jsonCommit);
		
		// commit message
		if(!commitForUncommitedChanges) {
		    if(!completeJsonCommit.containsKey("message")) {
			    throw new ParseException(0, "Attribute 'message' of commit is missing.");
		    }
    	    this.message = (String) completeJsonCommit.get("message");
		}
    	
    	// model of the commit
    	if(!completeJsonCommit.containsKey("model")) {
    		throw new ParseException(0, "Attribute 'model' of commit is missing.");
    	}
    	this.model = new Model(((JSONObject) completeJsonCommit.get("model")).toJSONString());
	}
	
	/**
	 * Creates a new commit by loading it from the database.
	 * @param commitId Id of the commit to search for.
	 * @param connection Connection object
	 * @throws SQLException If something with the database went wrong (or CommitNotFoundException).
	 */
	public Commit(int commitId, Connection connection) throws SQLException {
		this.id = commitId;
		
		// load commit attributes
		PreparedStatement statement = connection.prepareStatement("SELECT message, timestamp FROM Commit WHERE id = ?;");
		statement.setInt(1, commitId);
		
		ResultSet queryResult = statement.executeQuery();
		if(queryResult.next()) {
			this.message = queryResult.getString(1);
			this.timestamp = queryResult.getString(2);
		} else {
			throw new CommitNotFoundException();
		}
		statement.close();
		
		// load model
		statement = connection.prepareStatement("SELECT modelId FROM CommitToModel WHERE commitId = ?;");
		statement.setInt(1, commitId);
		queryResult = statement.executeQuery();
		if(queryResult.next()) {
			this.model = new Model(queryResult.getInt(1), connection);
		} else {
			throw new CommitNotFoundException();
		}
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject jsonCommit = new JSONObject();
		
		jsonCommit.put("id", this.id);
		jsonCommit.put("model", this.model.toJSONObject());
		jsonCommit.put("message", this.message);
		jsonCommit.put("timestamp", this.timestamp);
		
		return jsonCommit;
	}
	
	public void persist(int versionedModelId, Connection connection) throws SQLException {
		PreparedStatement statement;
		boolean autoCommitBefore = connection.getAutoCommit();
		try {
			connection.setAutoCommit(false);
			
			statement = connection.prepareStatement("INSERT INTO Commit (message, timestamp) VALUES (?, CURRENT_TIMESTAMP);", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, this.message);
			// execute query
			statement.executeUpdate();
		    // get the generated id and close statement
			ResultSet genKeys = statement.getGeneratedKeys();
			genKeys.next();
			this.id = genKeys.getInt(1);
		    statement.close();
		    
		    // store model
		    this.model.persist(connection);
		    
		    // add CommitToModel entry
		    statement = connection.prepareStatement("INSERT INTO CommitToModel (commitId, modelId) VALUES (?, ?);");
		    statement.setInt(1, this.id);
		    statement.setInt(2, this.model.getId());
		    statement.executeUpdate();
		    statement.close();
		    
		    // add CommitToVersionedModel entry
		    statement = connection.prepareStatement("INSERT INTO CommitToVersionedModel (versionedModelId, commitId) VALUES (?,?);");
		    statement.setInt(1, versionedModelId);
		    statement.setInt(2, this.id);
		    statement.executeUpdate();
		    statement.close();
		    
		    // no errors occurred
		    connection.commit();
		} catch (SQLException e) {
			// roll back the whole stuff
			connection.rollback();
			throw e;
		} finally {
			// reset auto commit
			connection.setAutoCommit(autoCommitBefore);
		}
	}
	
	public void delete(Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("DELETE FROM Commit WHERE id = ?;");
		statement.setInt(1, this.id);
		statement.executeUpdate();
		statement.close();
	}
	

	public int getId() {
		return this.id;
	}
	
	public Model getModel() {
		return this.model;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public String getTimestamp() {
		return this.timestamp;
	}
	
}
