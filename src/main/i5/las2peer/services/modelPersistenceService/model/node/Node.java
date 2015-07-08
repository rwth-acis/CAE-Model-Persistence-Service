package i5.las2peer.services.modelPersistenceService.model.node;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;

import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;

/**
 * 
 * (Data-)Class for Nodes. Provides means to convert JSON to Object and Object to JSON. Also
 * provides means to persist and load the object to/from a database.
 *
 */
public class Node {

  private String id;
  private NodePosition position;
  private String type;
  private ArrayList<EntityAttribute> attributes;

  /**
   * 
   * Creates a new node entity.
   * 
   * @param nodeId the node id
   * @param jsonNode the content of the node entry in the (JSON-represented) model
   * 
   */
  public Node(String nodeId, JSONObject jsonNode) {
    this.id = nodeId;
    this.type = (String) jsonNode.get("type");
    this.position = new NodePosition((int) ((Number) jsonNode.get("left")).intValue(),
        ((Number) jsonNode.get("top")).intValue(), ((Number) jsonNode.get("width")).intValue(),
        ((Number) jsonNode.get("height")).intValue(), ((Number) jsonNode.get("zIndex")).intValue());

    // parse attributes
    JSONObject jsonAttributes = (JSONObject) jsonNode.get("attributes");
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
   * Creates a new node entity from the database.
   * 
   * @param nodeId the node id
   * @param connection a Connection object
   * 
   * @throws SQLException if something went wrong fetching the node from the database
   * 
   */
  public Node(String nodeId, Connection connection) throws SQLException {
    // first create empty attribute list
    this.attributes = new ArrayList<EntityAttribute>();

    // fetch node
    PreparedStatement statement =
        connection.prepareStatement("SELECT * FROM Node WHERE nodeId = ?;");
    statement.setString(1, nodeId);
    ResultSet queryResult = statement.executeQuery();
    if (queryResult.next()) {
      this.id = queryResult.getString(1);
      this.type = queryResult.getString(2);
      this.position = new NodePosition(queryResult.getInt(3), queryResult.getInt(4),
          queryResult.getInt(5), queryResult.getInt(6), queryResult.getInt(7));
      statement.close();
    } else {
      throw new SQLException("Could not find node!");
    }

    // attribute entries
    statement =
        connection.prepareStatement("SELECT attributeId FROM AttributeToNode WHERE nodeId = ?;");
    statement.setString(1, this.id);
    queryResult = statement.executeQuery();
    while (queryResult.next()) {
      this.attributes.add(new EntityAttribute(queryResult.getInt(1), connection));
    }
    statement.close();
  }

  public String getId() {
    return id;
  }

  public NodePosition getPosition() {
    return position;
  }

  public String getType() {
    return type;
  }

  public ArrayList<EntityAttribute> getAttributes() {
    return attributes;
  }

  /**
   * 
   * Returns the JSON representation of this node. This representation is rather specific to
   * SyncMeta and should not be taken as a generic example of a JSON object representation.
   * 
   * @return a JSON object representing a (SyncMeta) compatible node representation
   * 
   */
  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    // content of main object
    JSONObject jsonNode = new JSONObject();

    // start with the position elements and type
    jsonNode.put("left", position.getLeft());
    jsonNode.put("top", position.getTop());
    jsonNode.put("width", position.getWidth());
    jsonNode.put("height", position.getHeight());
    jsonNode.put("zIndex", position.getzIndex());
    jsonNode.put("type", type);

    // label element of nodeContent
    JSONObject label = new JSONObject();
    label.put("id", this.id + "[name]");
    // currently, SyncMeta supports only "name" as label for nodes.
    // Important: if one has no "name" attribute in the node, this will not work.
    label.put("name", "name");
    JSONObject labelValue = new JSONObject();
    labelValue.put("id", this.id + "[name]");
    labelValue.put("name", "name");
    // at this point we have to "wait" for the attributes to be read out, since the "value" of the
    // name can be derived from the attribute with the name "name"

    // attribute element of nodeContent
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

      // check for name of label, if found, add value to label
      if (currentAttribute.getName().equals("name")) {
        labelValue.put("value", currentAttribute.getValue());
        label.put("value", labelValue);
      }

    }

    // add label and attributes to node content, then finally add content to node
    jsonNode.put("label", label);
    jsonNode.put("attributes", attributes);
    return jsonNode;
  }

  /**
   * 
   * Persists the node object.
   * 
   * @param connection a Connection object
   * 
   * @throws SQLException if something goes wrong persisting the node entity
   * 
   */
  public void persist(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO Node (nodeId, type, pLeft, pTop, pWidth, pHeight, pZIndex) VALUES (?,?,?,?,?,?,?);");
    statement.setString(1, this.id);
    statement.setString(2, this.type);
    statement.setInt(3, this.position.getLeft());
    statement.setInt(4, this.position.getTop());
    statement.setInt(5, this.position.getWidth());
    statement.setInt(6, this.position.getHeight());
    statement.setInt(7, this.position.getzIndex());
    statement.executeUpdate();
    statement.close();
    // attributes entries
    for (int i = 0; i < this.attributes.size(); i++) {
      this.attributes.get(i).persist(connection);
      // AttributeToNode entry ("connect" them)
      statement = connection
          .prepareStatement("INSERT INTO AttributeToNode (attributeId, nodeId) VALUES (?, ?);");
      statement.setInt(1, this.attributes.get(i).getId());
      statement.setString(2, this.getId());
      statement.executeUpdate();
      statement.close();
    }

  }

}
