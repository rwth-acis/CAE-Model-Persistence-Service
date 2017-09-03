package i5.las2peer.services.modelPersistenceService.model.metadata;

import java.util.Date;
/**
 * 
 * Model of element to element connection inside component to component connection
 * 
 */
public class ElementToElement {
    private String id;
    private String firstElementId;
    private String secondElementId;
    private Date timeCreated;
    private Date timeEdited;
    private String dataType;
    
    /**
     * Default empty object constructor
     */
    public ElementToElement() {}
    
    /**
     * Constructor with values
     * @param firstElementId first element id
     * @param secondElementId second element id
     * @param timeCreated created time
     * @param timeEdited edited time
     */
    public ElementToElement(
        String firstElementId, 
        String secondElementId, 
        Date timeCreated,
        Date timeEdited) {
            this.firstElementId = firstElementId;
            this.secondElementId = secondElementId;
            this.timeCreated = timeCreated;
            this.timeEdited = timeEdited;
        }

    /**
     * Constructor with values
     * @param id element to element id
     * @param firstElementId first element id
     * @param secondElementId second element id
     * @param timeCreated created time
     * @param timeEdited edited time
     */
    public ElementToElement(
        String id,
        String firstElementId, 
        String secondElementId, 
        Date timeCreated,
        Date timeEdited) {
            this.id = id;
            this.firstElementId = firstElementId;
            this.secondElementId = secondElementId;
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
     * Get first element id
     * @return first element id string
     */
    public String getFirstElementId() {
        return firstElementId;
    }

    public void setFirstElementId(String firstElementId) {
        this.firstElementId = firstElementId;
    }

    /**
     * Get second element id
     * @return second element id string
     */
    public String getSecondElementId() {
        return secondElementId;
    }

    public void setSecondElementId(String secondElementId) {
        this.secondElementId = secondElementId;
    }

    /**
     * Get data type of element to element connection
     * @return string of connection data type
     */
    public String getDataType() {
        return dataType;
    }
    
    /**
     * Set element to element connection data type
     * @param dataType
     */
    public void setDataType(String dataType) {
    	this.dataType = dataType;
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