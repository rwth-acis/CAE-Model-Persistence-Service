package i5.las2peer.services.modelPersistenceService.versionedModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
	
	public Commit(String jsonCommit) throws ParseException {
		JSONObject completeJsonCommit = (JSONObject) JSONValue.parseWithException(jsonCommit);
		
		// commit message
		if(!completeJsonCommit.containsKey("message")) {
			throw new ParseException(0, "Attribute 'message' of commit is missing.");
		}
    	this.message = (String) completeJsonCommit.get("message");
    	
    	// model of the commit
    	if(!completeJsonCommit.containsKey("model")) {
    		throw new ParseException(0, "Attribute 'model' of commit is missing.");
    	}
    	this.model = new Model((String) completeJsonCommit.get("model"));
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
		statement = connection.prepareStatement("SELECT modelId, FROM CommitToModel WHERE commitId = ?;");
		statement.setInt(1, commitId);
		queryResult = statement.executeQuery();
		if(queryResult.next()) {
			//this.model = new Model(queryResult.getInt(1), connection);
			// TODO: heres the problem that models are identified by their name now
		} else {
			throw new CommitNotFoundException();
		}
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
