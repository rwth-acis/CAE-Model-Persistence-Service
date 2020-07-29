package i5.las2peer.services.modelPersistenceService.versionedModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import i5.las2peer.services.modelPersistenceService.exception.CommitNotFoundException;
import i5.las2peer.services.modelPersistenceService.exception.VersionedModelNotFoundException;

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
	 * @param versionedModelId Id of the versioned model to search for.
	 * @param connection Connection object
	 * @throws SQLException If something with the database went wrong (VersionedModelNotFoundException if model not found).
	 */
	public VersionedModel(int versionedModelId, Connection connection) throws SQLException {
		this.id = versionedModelId;
		
		// check if a versioned model with the given id exists
		PreparedStatement statement = connection.prepareStatement("SELECT * FROM VersionedModel WHERE id = ?;");
		statement.setInt(1, versionedModelId);
		
		ResultSet queryResult = statement.executeQuery();
		if(!queryResult.next()) {
			// there does not exist a versioned model with the given id
			throw new VersionedModelNotFoundException();
		}
		statement.close();
		
		// create empty list for commits
		this.commits = new ArrayList<>();
		
		// load commits (order by id descending, then the latest commit is the first in the list)
		statement = connection.prepareStatement("SELECT commitId FROM CommitToVersionedModel WHERE versionedModelId = ? ORDER BY commitId DESC;");
		statement.setInt(1, versionedModelId);
		
		queryResult = statement.executeQuery();
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
	
	/**
	 * Searches for a commit without a message.
	 * That one is the commit for "uncommited changes".
	 * @return Commit that gets used to store uncommited changes.
	 * @throws CommitNotFoundException If there does not exist a commit without a commit message.
	 */
	public Commit getCommitForUncommitedChanges() throws CommitNotFoundException {
		for(Commit commit : this.commits) {
			if(commit.getMessage() == null) return commit;
		}
		throw new CommitNotFoundException();
		
	}
	
	/**
	 * Returns a list of strings containing the version tags that are set to commits of the versioned model.
	 * @return ArrayList containing the version tags of the versioned model as strings.
	 */
	public ArrayList<String> getVersions() {
		ArrayList<String> versions = new ArrayList<>();
		
		for(Commit commit : this.commits) {
			// skip the commit for "uncommited changes"
			if(commit.getMessage() == null) continue;
			
			if(commit.getVersionTag() != null) versions.add(commit.getVersionTag());
		}
		
		return versions;
	}
	
	public int getId() {
		return this.id;
	}
	
	public ArrayList<Commit> getCommits() {
		return this.commits;
	}
	
	public Commit getCommitBySha(String sha) {
		for(Commit commit : this.getCommits()) {
			if(commit.getSha() == null) continue;
			if(commit.getSha().equals(sha)) return commit;
		}
		return null;
	}
	
}
