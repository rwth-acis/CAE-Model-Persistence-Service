package i5.las2peer.services.modelPersistenceService.projectMetadata;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.UserAgent;

public class ProjectMetadata {

	/**
	 * Roles that are available in this project.
	 */
	private ArrayList<Role> roles;
	
	/**
	 * Maps user agent id to the name of the role assigned to the user.
	 */
	private HashMap<String, String> mapUserRole; 
	
	/**
	 * List of components that belong to this project.
	 */
	private ArrayList<Component> components;
	
	/**
	 * List of dependencies that belong to this project.
	 */
	private ArrayList<Component> dependencies;
	
	/**
	 * List of external dependencies that belong to this project.
	 */
	private ArrayList<ExternalDependency> externalDependencies;
	
	public ProjectMetadata(Connection connection, String projectName, String projectCreatorAgentId, String codeGenService) throws SQLException {
	    this.roles = PredefinedRoles.get();	
	    
	    this.mapUserRole = new HashMap<>();
	    // add the project creator to the user role map
	    this.mapUserRole.put(projectCreatorAgentId, PredefinedRoles.getDefaultRoleName());
	    
	    this.components = new ArrayList<>();
	    // store empty application model
	    createApplicationComponent(connection, projectName, codeGenService);
	    
	    this.dependencies = new ArrayList<>();
	    this.externalDependencies = new ArrayList<>();
	}
	
	private void createApplicationComponent(Connection connection, String projectName, String codeGenService) throws SQLException {
		String applicationComponentName = projectName + "-application";
		Component applicationComponent = new Component(applicationComponentName, Component.TYPE_APPLICATION);
		
		// create versioned model for the component
		applicationComponent.createEmptyVersionedModel(connection, false);

		// create GitHub repo
		String repoName = "application-" +  applicationComponent.getVersionedModelId();
		try {
			Context.get().invoke(codeGenService, "createRepo", new Serializable[] { repoName });
		} catch (Exception e) {

		}

		// also create category in Requirements Bazaar
		// TODO
		
		this.components.add(applicationComponent);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject jsonMetadata = new JSONObject();
		
		JSONArray jsonRoles = new JSONArray();
		for(Role role : roles) {
			jsonRoles.add(role.toJSONObject());
		}
		jsonMetadata.put("roles", jsonRoles);
		
		JSONArray jsonUserRole = new JSONArray();
		for(Map.Entry<String, String> entry : this.mapUserRole.entrySet()) {
			String agentId = entry.getKey();
			String roleName = entry.getValue();
			String loginName = "";
			try {
				loginName = ((UserAgent) Context.get().requestAgent(agentId)).getLoginName();
			} catch (Exception e) {
				e.printStackTrace();
			}
			JSONObject o = new JSONObject();
			o.put("agentId", agentId);
			o.put("loginName", loginName);
			o.put("roleName", roleName);
			jsonUserRole.add(o);
		}
		jsonMetadata.put("mapUserRole", jsonUserRole);
		
		JSONArray jsonComponents = new JSONArray();
		for(Component component : components) {
			jsonComponents.add(component.toJSONObject());
		}
		jsonMetadata.put("components", jsonComponents);
		
		JSONArray jsonDependencies = new JSONArray();
		for(Component dependency : dependencies) {
			jsonDependencies.add(dependency.toJSONObject());
		}
		jsonMetadata.put("dependencies", jsonDependencies);
		
		JSONArray jsonExternalDependencies = new JSONArray();
		for(ExternalDependency externalDependency : externalDependencies) {
			jsonExternalDependencies.add(externalDependency.toJSONObject());
		}
		jsonMetadata.put("externalDependencies", jsonExternalDependencies);
		
		return jsonMetadata;
	}

	public ArrayList<Component> getComponents() {
		return components;
	}
}
