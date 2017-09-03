package i5.las2peer.services.modelPersistenceService.model.metadata;

import java.util.Date;
import java.util.ArrayList;

/**
 * 
 * Model of component to component connection
 * 
 */
public class ComponentToComponent {
    private String id;
    private String firstComponentId;
    private String secondComponentId;
    private Date timeCreated;
    private Date timeEdited;
    private ArrayList<ElementToElement> elementConnections;
    
    /**
     * Default empty object constructor
     */
    public ComponentToComponent() {}

    /**
     * Constructor with values
     * @param firstComponentId first component id
     * @param secondComponentId second component id
     * @param timeCreated created time
     * @param timeEdited edited time
     */
    public ComponentToComponent(
        String firstComponentId, 
        String secondComponentId, 
        Date timeCreated,
        Date timeEdited) {
            this.firstComponentId = firstComponentId;
            this.secondComponentId = secondComponentId;
            this.timeCreated = timeCreated;
            this.timeEdited = timeEdited;
        }

    /**
     * Constructor with values
     * @param id component to component id
     * @param firstComponentId first component id
     * @param secondComponentId second component id
     * @param timeCreated created time
     * @param timeEdited edited time
     */
    public ComponentToComponent(
        String id,
        String firstComponentId, 
        String secondComponentId, 
        Date timeCreated,
        Date timeEdited) {
            this.id = id;
            this.firstComponentId = firstComponentId;
            this.secondComponentId = secondComponentId;
            this.timeCreated = timeCreated;
            this.timeEdited = timeEdited;
        }

    /**
     * Get set current model id
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get set first component id
     * @return first component id string
     */
    public String getFirstComponentId() {
        return firstComponentId;
    }

    public void setFirstComponentId(String firstComponentId) {
        this.firstComponentId = firstComponentId;
    }

    /**
     * Get second component id
     * @return second component id string
     */
    public String getSecondComponentId() {
        return secondComponentId;
    }

    public void setSecondComponentId(String secondComponentId) {
        this.secondComponentId = secondComponentId;
    }

    /**
     * Get all contained element connections
     * @return array list of all contained element connections
     */
    public ArrayList<ElementToElement> getElementConnections() {
        return elementConnections;
    }

    public void setElementConnections(ArrayList<ElementToElement> elementConnections) {
        this.elementConnections = elementConnections;
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