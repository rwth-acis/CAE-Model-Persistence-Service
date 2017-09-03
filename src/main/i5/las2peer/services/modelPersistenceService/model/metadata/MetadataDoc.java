package i5.las2peer.services.modelPersistenceService.model.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Date;
import java.io.IOException;
/**
 * 
 * Model of documentation string of a component
 * 
 */
public class MetadataDoc {
    private String id;
    private String componentId;
    private String docType;
    private String docString;
    private Date timeCreated;
    private Date timeEdited;
    
    /**
     * Default empty object constructor
     */
    public MetadataDoc() {}
    
    /**
     * Constructor with values without id
     * @param componentId component id
     * @param docString string of the component doc
     * @param timeCreated created time
     * @param timeEdited edited time
     */
    public MetadataDoc(
        String componentId, 
        String docType,
        String docString, 
        Date timeCreated,
        Date timeEdited) {
            this.componentId = componentId;
            this.docType = docType;
            this.docString = docString;
            this.timeCreated = timeCreated;
            this.timeEdited = timeEdited;
        }
    
    /**
     * Constructor with values with id
     * @param id
     * @param componentId component id
     * @param docString string of the component doc
     * @param timeCreated created time
     * @param timeEdited edited time
     */
    public MetadataDoc(
        String id,
        String componentId, 
        String docType, 
        String docString, 
        Date timeCreated,
        Date timeEdited) {
            this.id = id;
            this.componentId = componentId;
            this.docType = docType;
            this.docString = docString;
            this.timeCreated = timeCreated;
            this.timeEdited = timeEdited;
        }

    /**
     * Get current model id
     */
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

	/**
     * Get component id
     * @return component id string
     */
    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
    	this.componentId = componentId;
    }

    /**
     * Get component documentation as string
     * @return string of component documentation
     */
    public String getDocString() {
        return docString;
    }

    public void setDocString(String docString) {
    	this.docString = docString;
    }

    /**
     * Get component documentation type (JSON or YAML)
     * @return string of component type
     */
    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
    	this.docType = docType;
    }
    
    /**
     * Get created time
     * @return created time date
     */
    public Date getTimeCreated() {
    	return timeCreated;
    }

    public void setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
    }
    
    /**
     * Get edited time
     * @return edited time date
     */
    public Date getTimeEdited() {
    	return timeEdited;
    }

    public void setTimeEdited(Date timeEdited) {
        this.timeEdited = timeEdited;
    }
}