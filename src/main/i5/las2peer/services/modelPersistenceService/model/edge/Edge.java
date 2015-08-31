package i5.las2peer.services.modelPersistenceService.model.edge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.edge.SimpleEdge;
import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;

/**
 * 
 * (Data-)Class for Edges. Provides means to convert JSON to Object and Object to JSON. Also
 * provides means to persist the object to a database.
 *
 */
public class Edge {
  private String id;
  private String sourceNode;
  private String targetNode;
  private String type;
  // an edge has a label value (in comparison to a node which uses the "name" value here)
  private String labelValue;
  private ArrayList<EntityAttribute> attributes;

  /**
   * Creates a new edge entity.
   * 
   * @param edgeId the edge id
   * 
   * @param jsonEdge the content of the edge entry in the (JSON-represented) model
   * 
   */
  public Edge(String edgeId, JSONObject jsonEdge) {
    this.id = edgeId;
    this.type = (String) jsonEdge.get("type");
    this.sourceNode = (String) jsonEdge.get("source");
    this.targetNode = (String) jsonEdge.get("target");

    // dig deep into the object to get the value of the label..remaining part of the inner objects
    // is not needed to save (since redundant information is stored there, needed only for frontend
    // (synchronization) reasons)
    this.labelValue =
        (String) ((JSONObject) ((JSONObject) jsonEdge.get("label")).get("value")).get("value");

    // parse attributes
    JSONObject jsonAttributes = (JSONObject) jsonEdge.get("attributes");
    this.attributes = new ArrayList<EntityAttribute>(jsonAttributes.size());
    @SuppressWarnings("unchecked")
    Iterator<Map.Entry<String, Object>> jsonAttribute = jsonAttributes.entrySet().iterator();
    while (jsonAttribute.hasNext()) {
      Map.Entry<String, Object> entry = jsonAttribute.next();
      String attributeId = entry.getKey();
      JSONObject attribute = (JSONObject) entry.getValue();
      attributes.add(new EntityAttribute(attributeId, attribute));
    }
  }


  /**
   * 
   * Creates a new edge entity from the database.
   * 
   * @param edgeId the edge id
   * @param connection a Connection object
   * 
   * @throws SQLException if something went wrong fetching the edge from the database
   * 
   */
  public Edge(String edgeId, Connection connection) throws SQLException {
    // first create empty attribute list
    this.attributes = new ArrayList<EntityAttribute>();

    // fetch edge
    PreparedStatement statement =
        connection.prepareStatement("SELECT * FROM Edge WHERE edgeId = ?;");
    statement.setString(1, edgeId);
    ResultSet queryResult = statement.executeQuery();
    if (queryResult.next()) {
      this.id = queryResult.getString(1);
      this.sourceNode = queryResult.getString(2);
      this.targetNode = queryResult.getString(3);
      this.labelValue = queryResult.getString(4);
      this.type = queryResult.getString(5);
      statement.close();
    } else {
      throw new SQLException("Could not find node!");
    }

    // attribute entries
    statement =
        connection.prepareStatement("SELECT attributeId FROM AttributeToEdge WHERE edgeId = ?;");
    statement.setString(1, this.id);
    queryResult = statement.executeQuery();
    while (queryResult.next()) {
      this.attributes.add(new EntityAttribute(queryResult.getInt(1), connection));
    }
    statement.close();
  }


  /**
   * 
   * Creates an edge from a passed on {@link i5.cae.simpleModel.edge.SimpleEdge}.
   * 
   * @param edge a {@link i5.cae.simpleModel.edge.SimpleEdge}
   * 
   */
  public Edge(SimpleEdge edge) {
    this.id = edge.getId();
    this.sourceNode = edge.getSourceNode();
    this.targetNode = edge.getTargetNode();
    this.labelValue = edge.getLabelValue();
    this.type = edge.getType();
    this.attributes = new ArrayList<EntityAttribute>();
    for (SimpleEntityAttribute attribute : edge.getAttributes()) {
      this.attributes.add(new EntityAttribute(attribute));
    }
  }


  public String getId() {
    return this.id;
  }


  public String getSourceNode() {
    return this.sourceNode;
  }


  public String getTargetNode() {
    return this.targetNode;
  }


  public String getType() {
    return this.type;
  }


  public ArrayList<EntityAttribute> getAttributes() {
    return this.attributes;
  }


  public String getLabelValue() {
    return this.labelValue;
  }


  /**
   * 
   * Returns the JSON representation of this edge. This representation is rather specific to
   * SyncMeta and should not be taken as a generic example of a JSON object representation.
   * 
   * @return a JSON object representing a (SyncMeta) compatible edge representation
   * 
   */
  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    // main object
    JSONObject jsonEdge = new JSONObject();

    // start with the source, target and type
    jsonEdge.put("source", this.sourceNode);
    jsonEdge.put("target", this.targetNode);
    jsonEdge.put("type", this.type);

    // label element of edgeContent
    JSONObject label = new JSONObject();
    label.put("id", this.id + "[label]");
    // currently, SyncMeta supports only "Label" as label for edges.
    label.put("name", "Label");
    JSONObject labelValue = new JSONObject();
    labelValue.put("id", this.id + "[label]");
    labelValue.put("name", "Label");
    labelValue.put("value", this.getLabelValue());
    label.put("value", labelValue);
    jsonEdge.put("label", label);

    // attribute element of edgeContent
    JSONObject attributes = new JSONObject();
    for (int attributeIndex = 0; attributeIndex < this.attributes.size(); attributeIndex++) {
      EntityAttribute currentAttribute = this.attributes.get(attributeIndex);
      JSONObject attributeContent = new JSONObject();
      attributeContent.put("id", this.id + "[" + currentAttribute.getName() + "]");
      attributeContent.put("name", currentAttribute.getName());
      // value of attribute
      JSONObject attributeValue = new JSONObject();
      attributeValue.put("id", this.id + "[" + currentAttribute.getName() + "]");
      attributeValue.put("name", currentAttribute.getName());
      attributeValue.put("value", currentAttribute.getValue());
      attributeContent.put("value", attributeValue);
      // add attribute to attribute list with the attribute's id as key
      attributes.put(currentAttribute.getSyncMetaId(), attributeContent);
    }
    jsonEdge.put("attributes", attributes);
    return jsonEdge;
  }


  /**
   * 
   * Persists the Edge entity.
   * 
   * @param connection a Connection object
   * 
   * @throws SQLException if something goes wrong persisting the Edge entity
   * 
   */
  public void persist(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO Edge (edgeId, sourceNode, targetNode, labelValue, type) VALUES (?,?,?,?,?);");
    statement.setString(1, this.id);
    statement.setString(2, this.sourceNode);
    statement.setString(3, this.targetNode);
    statement.setString(4, this.labelValue);
    statement.setString(5, this.type);
    statement.executeUpdate();
    statement.close();
    // attributes entries
    for (int i = 0; i < this.attributes.size(); i++) {
      this.attributes.get(i).persist(connection);
      // AttributeToEdge entry ("connect" them)
      statement = connection
          .prepareStatement("INSERT INTO AttributeToEdge (attributeId, edgeId) VALUES (?, ?);");
      statement.setInt(1, this.attributes.get(i).getId());
      statement.setString(2, this.getId());
      statement.executeUpdate();
      statement.close();
    }
  }


  /**
   * 
   * Deletes this Edge from the database.
   * 
   * @param connection a ConnectionObject
   * @throws SQLException if something went wrong during deletion
   * 
   */
  public void deleteFromDatabase(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("DELETE FROM Edge WHERE edgeId = ?;");
    statement.setString(1, this.id);
    statement.executeUpdate();
    statement.close();
    for (int i = 0; i < this.attributes.size(); i++) {
      this.attributes.get(i).deleteFromDatabase(connection);
    }
  }
}
