package i5.las2peer.services.modelPersistenceService.projectMetadata;

import org.json.simple.JSONObject;

/**
 * (Data-)Class for Roles. Provides means to convert JSON to Object and Object
 * to JSON.
 */
public class Role {
	
	/**
	 * Name of the role.
	 */
	private String name;
	
	/**
	 * Contains information on which widgets are enabled for this role.
	 */
	private String widgetConfig;
	
	/**
	 * Whether the role is the default one of the project.
	 */
	private boolean isDefault;
	
	public Role(String name, String widgetConfig, boolean isDefault) {
	    this.name = name;	
	    this.widgetConfig = widgetConfig;
	    this.isDefault = isDefault;
	}
	
	/**
	 * Returns the JSON representation of this role.
	 * @return a JSON object representing a role
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject jsonRole = new JSONObject();
		
		jsonRole.put("name", this.name);
		jsonRole.put("widgetConfig", this.widgetConfig);
		jsonRole.put("isDefault", this.isDefault);
		
		return jsonRole;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getWidgetConfig() {
		return this.widgetConfig;
	}
	
	public boolean isDefault() {
		return this.isDefault;
	}
}