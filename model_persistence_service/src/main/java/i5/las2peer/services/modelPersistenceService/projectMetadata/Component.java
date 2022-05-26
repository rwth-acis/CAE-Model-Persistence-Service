package i5.las2peer.services.modelPersistenceService.projectMetadata;

import java.sql.Connection;
import java.sql.SQLException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

public class Component {
	
	public static String TYPE_FRONTEND = "frontend";
	public static String TYPE_MICROSERVICE = "microservice";
	public static String TYPE_APPLICATION = "application";
	
	/**
	 * Name of the component.
	 */
	private String name;
	
	/**
	 * Type of the component.
	 */
	private String type;
	
	/**
	 * Id of the corresponding category in the Requirements Bazaar.
	 */
	private int reqBazCategoryId = -1;
	
	/**
	 * Id of the corresponding project in the Requirements Bazaar.
	 */
	private int reqBazProjectId;
	
	/**
	 * Id of the versioned model which is connected to the component.
	 */
	private int versionedModelId;
	
	/**
	 * Constructor used when creating a totally new component.
	 * @param jsonComponent JSON representation of the component.
	 * @throws ParseException If the given JSON string could not be parsed or attributes are missing.
	 */
	public Component(String jsonComponent) throws ParseException {
		JSONObject component = (JSONObject) JSONValue.parseWithException(jsonComponent);
    	if(!component.containsKey("name")) throw new ParseException(0, "Attribute 'name' of component is missing.");
    	this.name = (String) component.get("name");
    	
    	if(!component.containsKey("type")) throw new ParseException(0, "Attribute 'type' of component is missing.");
    	String typeStr = (String) component.get("type");
        setType(typeStr);
        
        if(component.containsKey("reqBazProjectId")) {
        	this.reqBazProjectId = ((Long) component.get("reqBazProjectId")).intValue();
        }
        
        if(component.containsKey("reqBazCategoryId")) {
        	this.reqBazCategoryId = ((Long) component.get("reqBazCategoryId")).intValue();
        }
	}
	
	/**
	 * Constructor used when creating a totally new component. This constructor gets used internally
	 * when creating the empty application component for a new project.
	 * @param name Name of the component that should get created.
	 * @param type Type of the component that should get created.
	 */
	public Component(String name, String type) {
		this.name = name;
		this.type = type;
	}
	
	/**
	 * Sets the ComponentType to the type given as a string.
	 * @param typeStr Type of the component as string.
	 * @throws ParseException If type does not match the format.
	 */
	private void setType(String typeStr) throws ParseException {
		if(typeStr.equals(Component.TYPE_FRONTEND) || typeStr.equals(Component.TYPE_MICROSERVICE) || typeStr.equals(Component.TYPE_APPLICATION)) {
			this.type = typeStr;
		} else {
			throw new ParseException(0, "Attribute 'type' is not 'frontend', 'microservice' or 'application'.");
		}
	}
	
	public boolean isConnectedToReqBaz() {
		return this.reqBazCategoryId != -1;
	}
	
	public void setReqBazCategory(ReqBazCategory category) {
		this.reqBazProjectId = category.getProjectId();
		this.reqBazCategoryId = category.getId();
	}
	
	public ReqBazCategory getReqBazCategory() {
		return new ReqBazCategory(this.reqBazCategoryId, this.reqBazProjectId);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject jsonComponent = new JSONObject();
		
		jsonComponent.put("name", this.name);
		jsonComponent.put("type", this.type);
		jsonComponent.put("versionedModelId", this.versionedModelId);
		if(this.reqBazCategoryId != -1) {
			jsonComponent.put("reqBazProjectId", this.reqBazProjectId);
			jsonComponent.put("reqBazCategoryId", this.reqBazCategoryId);
		}
		
		return jsonComponent;
	}
	
	public void createEmptyVersionedModel(Connection connection, boolean isMicroservice) throws SQLException {
		// create empty versioned model for this component
		this.versionedModelId = ComponentInitHelper.createEmptyVersionedModel(connection, isMicroservice);
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getType() {
		return this.type;
	}
	
	public int getVersionedModelId() {
		return this.versionedModelId;
	}

}