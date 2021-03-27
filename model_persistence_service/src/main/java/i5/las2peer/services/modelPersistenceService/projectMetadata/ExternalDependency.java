package i5.las2peer.services.modelPersistenceService.projectMetadata;

import org.json.simple.JSONObject;

public class ExternalDependency {
	
	/**
	 * URL to the GitHub repository where the external dependency is hosted on.
	 */
	private String gitHubURL;
	
	/**
	 * Type, either "frontend" or "microservice".
	 */
	private String type;
	
	/**
	 * Constructor that creates a new ExternalDependency object from the given GitHub URL and type.
	 * @param gitHubURL URL to the corresponding GitHub repository.
	 * @param type Type of the external dependencies.
	 */
	public ExternalDependency(String gitHubURL, String type) {
		this.gitHubURL = gitHubURL;
		this.type = type;
	}
	
	/**
	 * Returns the JSON representation of this dependency.
	 * @return a JSON object representing a dependency.
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject jsonExternalDependency = new JSONObject();
		
		jsonExternalDependency.put("gitHubURL", this.gitHubURL);
		jsonExternalDependency.put("type", this.type);
		
		return jsonExternalDependency;
	}
	
	public String getGitHubRepoOwner() {
		return this.gitHubURL.split(".com/")[1].split("/")[0];
	}
	
	public String getGitHubRepoName() {
		String repoName = this.gitHubURL.split(".com/")[1].split("/")[1];
		if(repoName.endsWith(".git")) repoName = repoName.replace(".git", "");
		return repoName;
	}

}
