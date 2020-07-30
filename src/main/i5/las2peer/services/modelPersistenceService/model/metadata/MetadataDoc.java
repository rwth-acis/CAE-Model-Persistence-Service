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
    private String componentId;
    private String docType;
    private String docString;
    private String docInput;
    private String urlDeployed;
    private Date timeCreated;
    private String timeEdited;
    private String timeDeployed;
    private String version;
    
    /**
     * Default empty object constructor
     */
    public MetadataDoc() {}
    
    /**
     * Constructor with values without id
     * @param componentId component id
     * @param docType string of the component doc type
     * @param docString string of the component doc
     * @param docInput string of the user inputted component doc
     * @param timeCreated created time
     * @param timeEdited edited time
     */
    public MetadataDoc(
        String componentId, 
        String docType,
        String docString, 
        String docInput,
        Date timeCreated,
        String timeEdited,
        String version) {
            this.componentId = componentId;
            this.docType = docType;
            this.docString = docString;
            this.docInput = docInput;
            this.timeCreated = timeCreated;
            this.timeEdited = timeEdited;
            this.timeDeployed = null;
            this.urlDeployed = null;
            this.version = version;
        }

        public MetadataDoc(
        String componentId, 
        String docType,
        String docString, 
        String docInput,
        String urlDeployed,
        Date timeCreated,
        String timeEdited,
        String timeDeployed,
        String version) {
            this.componentId = componentId;
            this.docType = docType;
            this.docString = docString;
            this.docInput = docInput;
            this.timeCreated = timeCreated;
            this.timeEdited = timeEdited;
            this.timeDeployed = timeDeployed;
            this.urlDeployed = urlDeployed;
            this.version = version;
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
     * Get user inputted component documentation as string
     * @return string of user inputted component documentation
     */
    public String getDocInput() {
        return docInput;
    }

    public void setDocInput(String docInput) {
    	this.docInput = docInput;
    }

    /**
     * Get component deployed url as string
     * @return string of component's deployed url
     */
    public String getUrlDeployed() {
        return urlDeployed;
    }

    public void setUrlDeployed(String urlDeployed) {
    	this.urlDeployed = urlDeployed;
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
     * @return created time String
     */
    public Date getTimeCreated() {
    	return timeCreated;
    }

    public void setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
    }
    
    /**
     * Get edited time
     * @return edited time String
     */
    public String getTimeEdited() {
    	return timeEdited;
    }

    public void setTimeEdited(String timeEdited) {
        this.timeEdited = timeEdited;
    }

    /**
     * Get deployed time
     * @return deployed time String
     */
    public String getTimeDeployed() {
    	return timeDeployed;
    }

    public void setTimeDeployed(String timeDeployed) {
        this.timeDeployed = timeDeployed;
    }

    /**
     * Get deployed time
     * @return deployed time String
     */
    public String getVersion() {
    	return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}