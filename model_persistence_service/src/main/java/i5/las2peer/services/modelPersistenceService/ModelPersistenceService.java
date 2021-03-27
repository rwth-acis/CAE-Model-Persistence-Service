package i5.las2peer.services.modelPersistenceService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceNotAuthorizedException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.modelPersistenceService.database.DatabaseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;

import i5.las2peer.services.modelPersistenceService.modelServices.*;
import i5.las2peer.services.modelPersistenceService.projectMetadata.ProjectMetadata;
import i5.las2peer.services.modelPersistenceService.versionedModel.Commit;
import i5.las2peer.services.modelPersistenceService.versionedModel.VersionedModel;

/**
 * 
 * CAE Model Persistence Service
 * 
 * A LAS2peer service used for persisting (and validating) application models.
 * Part of the CAE.
 * 
 */
@Api
@SwaggerDefinition(info = @Info(title = "CAE Model Persistence Service", version = "0.1", description = "A LAS2peer service used for persisting (and validating) application models. Part of the CAE.", termsOfService = "none", contact = @Contact(name = "Peter de Lange", url = "https://github.com/PedeLa/", email = "lange@dbis.rwth-aachen.de"), license = @License(name = "BSD", url = "https://github.com/PedeLa/CAE-Model-Persistence-Service//blob/master/LICENSE.txt")))
@ServicePath("CAE")
@ManualDeployment
public class ModelPersistenceService extends RESTService {
	
	private static final String PROJECT_SERVICE = "i5.las2peer.services.projectService.ProjectService";

	/*
	 * Database configuration
	 */
	private String jdbcDriverClassName;
	private String jdbcLogin;
	private String jdbcPass;
	private String jdbcUrl;
	private String jdbcSchema;
	private String semanticCheckService = "";
	private String codeGenerationService = "";
	private String deploymentUrl = "";
	private DatabaseManager dbm;

	private MetadataDocService metadataDocService;

	/*
	 * Global variables
	 */
	private final static L2pLogger logger = L2pLogger.getInstance(ModelPersistenceService.class.getName());

	public ModelPersistenceService() {
		// read and set properties values
		setFieldValues();
		// instantiate a database manager to handle database connection pooling
		// and credentials
		dbm = new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass, jdbcUrl, jdbcSchema);
		metadataDocService = new MetadataDocService(this.dbm, this.logger);
	}

	@Override
	protected void initResources() {
		getResourceConfig().register(RESTResources.class);
	}
	
	public String getSemanticCheckService() {
		return semanticCheckService;
	}
	
	public String getCodeGenerationService() {
		return codeGenerationService;
	}

	public String getDeploymentUrl() {
		return deploymentUrl;
	}
	
	public DatabaseManager getDbm(){
		return dbm;
	}

	public MetadataDocService getMetadataService(){
		return metadataDocService;
	}
	
	/**
	 * Used by Code Generation Service. When the Live Code Editor widget changes a file and creates a new 
	 * commit, then also this method gets called which stores the commit message and the sha identifier of
	 * the commit to the database.
	 * @param commitSha Sha identifier of the commit.
	 * @param commitMessage Message of the commit.
	 * @param versionedModelId Id of the versioned model where the commit should be added to.
	 * @return
	 */
	public String addAutoCommitToVersionedModel(String commitSha, String commitMessage, int versionedModelId) {
		Connection connection = null;
		try {
			connection = dbm.getConnection();
			
			// there always exists a commit for "uncommited changes"
			// that one needs to be removed first
			VersionedModel versionedModel = new VersionedModel(versionedModelId, connection);
			Commit uncommitedChanges = versionedModel.getCommitForUncommitedChanges();
			uncommitedChanges.delete(connection);
			
			// now create a new commit
			Commit commit = new Commit(commitMessage);
			commit.persist(versionedModelId, connection, true);
			commit.persistSha(commitSha, connection);
			
			// readd uncommited changes commit
			uncommitedChanges.persist(versionedModelId, connection, true);
			
			return "done";
		} catch (SQLException e) {
			return "error";
		} finally {
			try {
			    connection.close();
			} catch (SQLException e) {
				return "error";
			}
		}
	}
	
	/**
	 * Used by Code Generation Service. Stores the given tag to the given commit.
	 * @param commitSha Sha identifier of the commit, which should be tagged.
	 * @param versionedModelId Id of the versioned model, which the commit belongs to.
	 * @param tag Tag that should be set to the commit.
	 * @return
	 */
	public String addTagToCommit(String commitSha, int versionedModelId, String tag) {
		Connection connection = null;
		try {
			connection = dbm.getConnection();
			
			VersionedModel versionedModel = new VersionedModel(versionedModelId, connection);
			Commit commit = versionedModel.getCommitBySha(commitSha);
			if(commit == null) return "error";
			
			commit.setVersionTag(tag, connection);
			return "done";
		} catch (SQLException e) {
			return "error";
		} finally {
			try {
			    connection.close();
			} catch (SQLException e) {
				return "error";
			}
		}
	}
	
	/**
	 * Returns a list containing the version tags of the versioned model with the given id.
	 * @param versionedModelId Id of the versioned model, where the version tags should be searched for.
	 * @return ArrayList containing the version tags of the versioned model as strings.
	 */
	public ArrayList<String> getVersionsOfVersionedModel(int versionedModelId) {
		ArrayList<String> versions = new ArrayList<>();
		Connection connection = null;
		try {
			connection = dbm.getConnection();
			
			VersionedModel versionedModel = new VersionedModel(versionedModelId, connection);
			return versionedModel.getVersions();
		} catch (SQLException e) {
			return versions;
		} finally {
			try {
			    connection.close();
			} catch (SQLException e) {
				return versions;
			}
		}
	}
	
	/**
	 * Method gets called by las2peer-project-service when a new project got created.
	 * @param project JSON representation of the newly created project.
	 */
	public void _onProjectCreated(JSONObject project) {
		Connection connection = null;
		try {
			connection = this.getDbm().getConnection();
			String projectName = (String) project.get("name");
			// create initial metadata for the project 
			ProjectMetadata metadata = new ProjectMetadata(connection, projectName, 
					Context.getCurrent().getMainAgent().getIdentifier());
			// update project and set this as the new metadata
			JSONObject o = new JSONObject();
			o.put("projectName", projectName);
			o.put("oldMetadata", new JSONObject());
			o.put("newMetadata", metadata.toJSONObject());
			Context.get().invoke(PROJECT_SERVICE, "changeMetadataRMI", "CAE", o.toJSONString());
		} catch (SQLException | ServiceNotFoundException | ServiceNotAvailableException | InternalServiceException | 
				ServiceMethodNotFoundException | ServiceInvocationFailedException | ServiceAccessDeniedException |
				ServiceNotAuthorizedException e) {
			logger.printStackTrace(e);
			System.out.println("ERROR:");
			System.out.println(e.toString());
			System.out.println(e.getMessage());
		} finally {
			try {
				if(connection != null) connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}
	
	/**
	 * Methods gets called by las2peer-project-service when a project got deleted.
	 * @param project JSON representation of the deleted project.
	 */
	public void _onProjectDeleted(JSONObject project) {
		
	}
	
}
