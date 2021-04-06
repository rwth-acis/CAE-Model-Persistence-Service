package i5.las2peer.services.modelPersistenceService.projectMetadata;

/**
 * Contains the information about a Requirements Bazaar category which is connected 
 * to a CAE component.
 * @author Philipp
 *
 */
public class ReqBazCategory {

	/**
	 * Id of the category.
	 */
	private int id;
	
	/**
	 * Id of the project which the category belongs to.
	 * This might be needed, if the project used changes and old
	 * components still use the old project id.
	 */
	private int projectId;
	
	public ReqBazCategory(int id, int projectId) {
		this.id = id;
		this.projectId = projectId;
	}
	
	public int getId() {
		return id;
	}
	
	public int getProjectId() {
		return projectId;
	}
}