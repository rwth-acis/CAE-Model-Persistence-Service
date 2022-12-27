package i5.las2peer.services.modelPersistenceService.projectMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Helper class for initializing new components, i.e. store an empty
 * versioned model with an empty model.
 * @author Philipp
 *
 */
public class ComponentInitHelper {
	
	/**
	 * Creates an empty versioned model, with an empty commit and and empty model.
	 * @param connection Connection object
	 * @throws SQLException If something with the database went wrong.
	 * @return Id of the created empty versioned model.
	 */
	public static int createEmptyVersionedModel(Connection connection, boolean isMicroservice) throws SQLException {
		// create versioned model entry first
		int versionedModelId = createEmptyVersionedModelEntry(connection);
		
		// create empty commit (which gets used to store the state of the model which is not yet commited)
		// the called method also creates an entry in CommitToVersionedModel table
		int commitId = createEmptyCommit(versionedModelId, connection);
		
		// now the empty model needs to be created and connected with the commit
		createEmptyModel(commitId, connection);
		
		if(isMicroservice) {
			createEmptyTestModel(commitId, connection);
		}
		
		return versionedModelId;
	}
	
	/**
	 * Creates an empty entry in the Model table and connects it with the given commit.
	 * @param commitId Id of the commit which should be connected with the model.
	 * @param connection Connection object
	 * @throws SQLException If something with the database went wrong
	 */
	private static void createEmptyModel(int commitId, Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("INSERT INTO Model () VALUES ();", Statement.RETURN_GENERATED_KEYS);
		statement.executeUpdate();
		ResultSet genKeys = statement.getGeneratedKeys();
		genKeys.next();
		int modelId = genKeys.getInt(1);
		statement.close();
		
		// create entry in CommitToModel table
		statement = connection.prepareStatement("INSERT INTO CommitToModel (commitId, modelId) VALUES (?,?);");
		statement.setInt(1, commitId);
		statement.setInt(2, modelId);
		statement.executeUpdate();
		statement.close();
	}
	
	private static void createEmptyTestModel(int commitId, Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("INSERT INTO TestModel () VALUES ();", Statement.RETURN_GENERATED_KEYS);
		statement.executeUpdate();
		ResultSet genKeys = statement.getGeneratedKeys();
		genKeys.next();
		int testModelId = genKeys.getInt(1);
		statement.close();
		
		// create entry in CommitToTestModel table
		statement = connection.prepareStatement("INSERT INTO CommitToTestModel (commitId, testModelId) VALUES (?,?);");
		statement.setInt(1, commitId);
		statement.setInt(2, testModelId);
		statement.executeUpdate();
		statement.close();
	}
	
	/**
	 * Creates an empty commit (which can be used to store model which is not yet commited)
	 * and connects it with the versioned model with the given id.
	 * @param versionedModelId Id of the versioned model which should be connected with the new commit.
	 * @param connection Connection object
	 * @return Id of the newly created commit.
	 * @throws SQLException If something with the database went wrong.
	 */
	private static int createEmptyCommit(int versionedModelId, Connection connection) throws SQLException {
		// create empty commit (this can be used to store model which is not yet commited)
		PreparedStatement statement = connection
				.prepareStatement("INSERT INTO Commit (commitType) VALUES (0);", Statement.RETURN_GENERATED_KEYS);
		statement.executeUpdate();
	    ResultSet genKeys = statement.getGeneratedKeys();
		genKeys.next();
		int commitId = genKeys.getInt(1);
		statement.close();
		
		// connect commit with versioned model
		statement = connection
				.prepareStatement("INSERT INTO CommitToVersionedModel (versionedModelId, commitId) VALUES (?,?);");
		statement.setInt(1, versionedModelId);
		statement.setInt(2, commitId);
		statement.executeUpdate();
		statement.close();
		return commitId;
	}
	
	
	/**
	 * Creates an entry in the VersionedModel table.
	 * @param connection Connection object
	 * @return Id of the created table entry.
	 * @throws SQLException If something with the database went wrong.
	 */
	private static int createEmptyVersionedModelEntry(Connection connection) throws SQLException {
		// first create an empty versioned model
		PreparedStatement statement = connection
				.prepareStatement("INSERT INTO VersionedModel () VALUES ();", Statement.RETURN_GENERATED_KEYS);
	    statement.executeUpdate();
	    ResultSet genKeys = statement.getGeneratedKeys();
		genKeys.next();
		int versionedModelId = genKeys.getInt(1);
		statement.close();
		return versionedModelId;
	}
	
}