package i5.las2peer.services.modelPersistenceService.model.modelAttributes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;

import i5.las2peer.services.modelPersistenceService.database.exception.ModelNotFoundException;
import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;

/**
 * 
 * (Data-)Class for model attributes. Provides means to convert JSON to Object and Object to JSON.
 * Also provides means to persist the object to a database.
 *
 */
public class ModelAttributes {
  private String name; // serves also as unique id
  private ArrayList<EntityAttribute> attributes; // meta-data

  /**
   * 
   * Creates a new ModelAttribute entry.
   * 
   * @param jsonModelAttribute the attribute as (SyncMeta-compatible) JSON file
   * 
   */
  public ModelAttributes(JSONObject jsonModelAttribute) {
    // get the name (never mind input structure here, its non straight-forward..)
    this.name = (String) ((JSONObject) ((JSONObject) jsonModelAttribute.get("label")).get("value"))
        .get("value");

    // parse attributes
    JSONObject jsonAttributes = (JSONObject) jsonModelAttribute.get("attributes");
    this.attributes = new ArrayList<EntityAttribute>(jsonAttributes.size());
    @SuppressWarnings("unchecked")
    Iterator<Map.Entry<String, Object>> jsonAttribute = jsonAttributes.entrySet().iterator();
    while (jsonAttribute.hasNext()) {
      Map.Entry<String, Object> entry = jsonAttribute.next();
      String attributeId = entry.getKey();
      JSONObject attribute = (JSONObject) entry.getValue();
      this.attributes.add(new EntityAttribute(attributeId, attribute));
    }
  }


  /**
   * 
   * Creates a new ModelAttributes entry by loading it from the database.
   * 
   * @param modelName the name of the model
   * @param connection a Connection Object
   * 
   * @throws SQLException if something went wrong loading the ModelAttributes
   * 
   */
  public ModelAttributes(String modelName, Connection connection) throws SQLException {
    // first create empty attribute list
    this.attributes = new ArrayList<EntityAttribute>();

    // search for modelAttributes
    PreparedStatement statement =
        connection.prepareStatement("SELECT * FROM ModelAttributes WHERE modelName=?;");
    statement.setString(1, modelName);
    // execute query
    ResultSet queryResult = statement.executeQuery();
    // process result set
    if (queryResult.next()) {
      this.name = queryResult.getString(1);
    } else {
      throw new ModelNotFoundException("Model with name " + modelName + " is not in database!");
    }
    statement.close();

    // attribute entries
    statement = connection.prepareStatement(
        "SELECT attributeId FROM AttributeToModelAttributes WHERE modelAttributesName = ?;");
    statement.setString(1, this.name);
    queryResult = statement.executeQuery();
    while (queryResult.next()) {
      this.attributes.add(new EntityAttribute(queryResult.getInt(1), connection));
    }
    statement.close();
  }


  public String getName() {
    return this.name;
  }


  public ArrayList<EntityAttribute> getAttributes() {
    return this.attributes;
  }


  /**
   * 
   * Returns the JSON representation of this model attribute. The representation is rather specific
   * to SyncMeta and should not be taken as a generic example of a JSON object representation.
   * 
   * @return a JSON object representing a (SyncMeta) compatible model attribute representation
   * 
   * 
   */
  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    // main object
    JSONObject modelAttribute = new JSONObject();
    // start with the (empty) position elements and type
    modelAttribute.put("left", "0");
    modelAttribute.put("top", "0");
    modelAttribute.put("width", "0");
    modelAttribute.put("height", "0");
    modelAttribute.put("zIndex", "0");
    modelAttribute.put("type", "ModelAttributesNode");


    // label element of modelAttributeContent
    Map<String, Object> label = new HashMap<String, Object>();
    label.put("id", "modelAttributes[label]");
    label.put("name", "Label");
    Map<String, Object> labelValue = new HashMap<String, Object>();
    labelValue.put("id", "modelAttributes[label]");
    labelValue.put("name", "Label");
    labelValue.put("value", this.getName());
    label.put("value", labelValue);
    modelAttribute.put("label", label);

    // attribute element of modelAttributeContent (currently empty)
    JSONObject attributes = new JSONObject();
    for (int attributeIndex = 0; attributeIndex < this.attributes.size(); attributeIndex++) {
      EntityAttribute currentAttribute = this.attributes.get(attributeIndex);
      JSONObject attributeContent = new JSONObject();
      attributeContent.put("id", "modelAttributes[" + currentAttribute.getName() + "]");
      attributeContent.put("name", currentAttribute.getName());

      // value of attribute
      JSONObject attributeValue = new JSONObject();
      attributeValue.put("id", "modelAttributes[" + currentAttribute.getName() + "]");
      attributeValue.put("name", currentAttribute.getName());
      attributeValue.put("value", currentAttribute.getValue());
      attributeContent.put("value", attributeValue);

      // add attribute to attribute list with the attribute's id as key
      attributes.put(currentAttribute.getSyncMetaId(), attributeContent);
    }
    modelAttribute.put("attributes", attributes);
    return modelAttribute;
  }


  /**
   * 
   * Method to persist a ModelAttribute to a database.
   * 
   * @param connection a Connection element passed on from the model class
   * 
   * @throws SQLException thrown if something goes wrong persisting the model attributes
   * 
   */
  public void persist(Connection connection) throws SQLException {
    // formulate query
    PreparedStatement statement =
        connection.prepareStatement("INSERT INTO ModelAttributes (modelName) VALUES (?);");
    statement.setString(1, this.name);
    // execute query
    statement.executeUpdate();
    statement.close();
    // attributes entries
    for (int i = 0; i < this.attributes.size(); i++) {
      this.attributes.get(i).persist(connection);
      // AttributeToModelAttributes entry ("connect" them)
      statement = connection.prepareStatement(
          "INSERT INTO AttributeToModelAttributes (attributeId, modelAttributesName) VALUES (?, ?);");
      statement.setInt(1, this.attributes.get(i).getId());
      statement.setString(2, this.name);
      statement.executeUpdate();
      statement.close();
    }
  }


  /**
   * Deletes this ModelAttribute from the database.
   * 
   * @param connection a ConnectionObject
   * @throws SQLException if something went wrong during deletion
   */
  public void deleteFromDatabase(Connection connection) throws SQLException {
    PreparedStatement statement =
        connection.prepareStatement("DELETE FROM ModelAttributes WHERE modelName = ?;");
    statement.setString(1, this.name);
    statement.executeUpdate();
    statement.close();
    for (int i = 0; i < this.attributes.size(); i++) {
      this.attributes.get(i).deleteFromDatabase(connection);
    }
  }
}
