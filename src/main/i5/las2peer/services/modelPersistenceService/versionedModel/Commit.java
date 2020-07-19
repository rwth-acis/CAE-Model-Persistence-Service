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
	 * Commits with commitType set to COMMIT_TYPE_MODEL belong to
	 * changes that were done to the model, i.e. changes created in 
	 * the modeling editor.
	 * These commits are connected to a model (which already contains
	 * the changes of the commit).
	 */
	public static final int COMMIT_TYPE_MODEL = 0;
	
	/**
	 * Commits with commitType set to COMMIT_TYPE_CODE belong to 
	 * changes that were done to the code, i.e. changes created in 
	 * the live code editor.
	 * These commits are NOT connected to a model. Only the commit
	 * sha identifier is stored to link the commit to GitHub.
	 */
	public static final int COMMIT_TYPE_CODE = 1;

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
	
	/**
	 * Commit sha identifier.
	 */
	private String sha;
	
	/**
	 * Tags thats connected to the commit.
	 * Might be null, if no tag is connected to the commit.
	 */
	private String versionTag;
	
	/**
	 * Type of the commit. Either COMMIT_TYPE_MODEL or
	 * COMMIT_TYPE_CODE.
	 */
	private int commitType;
	
	/**
	 * Constructor used to create commits of type COMMIT_TYPE_MODEL.
	 * @param jsonCommit
	 * @param commitForUncommitedChanges
	 * @throws ParseException
	 */
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
    	
    	// check if a version tag is included
    	if(completeJsonCommit.containsKey("versionTag") && !commitForUncommitedChanges) {
    		this.versionTag = (String) completeJsonCommit.get("versionTag");
    	}
    	
    	// set commit type for a commit that changes the model
    	this.commitType = COMMIT_TYPE_MODEL;
    	
    	this.model = new Model(((JSONObject) completeJsonCommit.get("model")).toJSONString());
	}
	
	/**
	 * Constructor used to create commits of type COMMIT_TYPE_CODE.
	 * @param commitMessage
	 */
	public Commit(String commitMessage) {
		this.message = commitMessage;
		this.commitType = COMMIT_TYPE_CODE;
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
		PreparedStatement statement = connection.prepareStatement("SELECT * FROM Commit WHERE id = ?;");
		statement.setInt(1, commitId);
		
		ResultSet queryResult = statement.executeQuery();
		if(queryResult.next()) {
			this.message = queryResult.getString("message");
			this.timestamp = queryResult.getString("timestamp");
			this.sha = queryResult.getString("sha");
			this.commitType = queryResult.getInt("commitType");
		} else {
			throw new CommitNotFoundException();
		}
		statement.close();
		
		// load model
		if(this.commitType == COMMIT_TYPE_MODEL) {
			statement = connection.prepareStatement("SELECT modelId FROM CommitToModel WHERE commitId = ?;");
			statement.setInt(1, commitId);
			queryResult = statement.executeQuery();
			if(queryResult.next()) {
				this.model = new Model(queryResult.getInt(1), connection);
			} else {
				throw new CommitNotFoundException();
			}
			statement.close();	
		}
		
		// load version tag if one exists
		statement = connection.prepareStatement("SELECT * FROM VersionTag WHERE commitId = ?;");
		statement.setInt(1, commitId);
		queryResult = statement.executeQuery();
		if(queryResult.next()) {
			this.versionTag = queryResult.getString("tag");
		}
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject jsonCommit = new JSONObject();
		
		jsonCommit.put("id", this.id);
		jsonCommit.put("commitType", this.commitType);
		if(this.commitType == COMMIT_TYPE_MODEL) {
		  jsonCommit.put("model", this.model.toJSONObject());
		}
		jsonCommit.put("message", this.message);
		jsonCommit.put("timestamp", this.timestamp);
		if(this.versionTag != null) {
			jsonCommit.put("versionTag", this.versionTag);
		}
		jsonCommit.put("sha", this.sha);
		
		return jsonCommit;
	}
	
	/**
	 * 
	 * @param versionedModelId
	 * @param connection
	 * @param commit Whether the changes to the database should be commited or not.
	 * @throws SQLException
	 */
	public void persist(int versionedModelId, Connection connection, boolean commit) throws SQLException {
		PreparedStatement statement;
		boolean autoCommitBefore = connection.getAutoCommit();
		try {
			connection.setAutoCommit(false);
			
			statement = connection.prepareStatement("INSERT INTO Commit (message, timestamp, commitType) " + 
			    "VALUES (?, CURRENT_TIMESTAMP, ?);", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, this.message);
			statement.setInt(2, this.commitType);
			// execute query
			statement.executeUpdate();
		    // get the generated id and close statement
			ResultSet genKeys = statement.getGeneratedKeys();
			genKeys.next();
			this.id = genKeys.getInt(1);
		    statement.close();
		    
		    // store version tag if there exists one
		    if(this.versionTag != null) {
		    	statement = connection.prepareStatement("INSERT INTO VersionTag (tag, commitId) VALUE (?, ?);");
		    	statement.setString(1, this.versionTag);
		    	statement.setInt(2, this.id);
		    	statement.executeUpdate();
		    	statement.close();
		    }
		    
		    // store model
		    if(this.commitType == COMMIT_TYPE_MODEL) {
		        this.model.persist(connection, false);
		        
		        // add CommitToModel entry
			    statement = connection.prepareStatement("INSERT INTO CommitToModel (commitId, modelId) VALUES (?, ?);");
			    statement.setInt(1, this.id);
			    statement.setInt(2, this.model.getId());
			    statement.executeUpdate();
			    statement.close();
		    }
		    
		    // add CommitToVersionedModel entry
		    statement = connection.prepareStatement("INSERT INTO CommitToVersionedModel (versionedModelId, commitId) VALUES (?,?);");
		    statement.setInt(1, versionedModelId);
		    statement.setInt(2, this.id);
		    statement.executeUpdate();
		    statement.close();
		    
		    // no errors occurred
		    if(commit) {
		        connection.commit();
		    }
		} catch (SQLException e) {
			// roll back the whole stuff
			connection.rollback();
			throw e;
		} finally {
			// reset auto commit
			connection.setAutoCommit(autoCommitBefore);
		}
	}
	
	/**
	 * Updates the sha identifier of the commit in the database.
	 * @param sha Commit sha identifier.
	 * @param connection Connection object
	 * @throws SQLException If something with the database went wrong.
	 */
	public void persistSha(String sha, Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("UPDATE Commit SET sha = ? WHERE id = ?;");
		statement.setString(1, sha);
		statement.setInt(2, this.id);
		statement.executeUpdate();
		statement.close();
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
	
	public String getSha() {
		return this.sha;
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
	
	public String getVersionTag() {
		return this.versionTag;
	}
	
	public int getCommitType() {
		return this.commitType;
	}
	
}
