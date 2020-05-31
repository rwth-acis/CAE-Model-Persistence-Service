package i5.las2peer.services.modelPersistenceService.versionedModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class VersionedModel {

	/**
	 * Id of the versioned model is set to -1 before the versioned model gets persisted.
	 */
	private int id = -1;
	
	/**
	 * List of commits that belong to the versioned model.
	 * Each of the commits describes the model at a specific
	 * time.
	 */
	private ArrayList<Commit> commits;
	
	/**
	 * Constructor that gets used before persisting a 
	 * versioned model.
	 */
	public VersionedModel() {
		this.commits = new ArrayList<>();
	}
	
	/**
	 * Creates a new versioned model by loading it from the database.
	 * @param modelId Id of the model to search for.
	 * @param connection Connection object
	 * @throws SQLException If something with the database went wrong.
	 */
	public VersionedModel(int modelId, Connection connection) throws SQLException {
		this.id = modelId;
		
		// create empty list for commits
		this.commits = new ArrayList<>();
		
		// load commits
		PreparedStatement statement = connection
				.prepareStatement("SELECT commitId FROM CommitToVersionedModel WHERE versionedModelId = ?;");
		statement.setInt(1, modelId);
		
		ResultSet queryResult = statement.executeQuery();
		while (queryResult.next()) {
			this.commits.add(new Commit(queryResult.getInt(1), connection));
		}
		statement.close();
	}
	
	/**
	 * Returns the JSON representation of the versioned model.
	 * @return A JSON representation of the versioned model.
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject jsonVersionedModel = new JSONObject();
		
		jsonVersionedModel.put("id", this.id);
		
		// add commits
		JSONArray jsonCommits = new JSONArray();
		for(Commit commit : this.commits) {
			jsonCommits.add(commit.toJSONObject());
		}
		jsonVersionedModel.put("commits", jsonCommits);
		
		return jsonVersionedModel;
	}
	
	/**
	 * Persists the versioned model itself.
	 * Note: This does not persist the commits.
	 * @param connection Connection object
	 * @throws SQLException If something with the database went wrong.
	 */
	public void persist(Connection connection) throws SQLException {
		PreparedStatement statement;
		// store the versioned model itself
		statement = connection.prepareStatement("INSERT INTO VersionedModel () VALUES ();", Statement.RETURN_GENERATED_KEYS);
		// execute query
		statement.executeUpdate();
		// get the generated id and close statement
		ResultSet genKeys = statement.getGeneratedKeys();
		genKeys.next();
		this.id = genKeys.getInt(1);
		statement.close();
	}
	
	public int getId() {
		return this.id;
	}
	
	public ArrayList<Commit> getCommits() {
		return this.commits;
	}
	
}
