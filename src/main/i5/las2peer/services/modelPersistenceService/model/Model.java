package i5.las2peer.services.modelPersistenceService.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import i5.las2peer.services.modelPersistenceService.models.edge.Edge;
import i5.las2peer.services.modelPersistenceService.models.modelAttribute.ModelAttributes;
import i5.las2peer.services.modelPersistenceService.models.node.Node;

/**
 * 
 * (Data-)Class for Models. Provides means to convert JSON to Object and Object to JSON. Also
 * provides means to persist the object to a database.
 *
 */
public class Model {
  private int id;
  private Node[] nodes;
  private Edge[] edges;
  private ModelAttributes attributes;

  /**
   * 
   * Creates a new model attribute entry.
   * 
   * @param jsonModel the attribute as (SyncMeta-compatible) JSON String
   * 
   * @throws ParseException if the parameter is not well formatted
   */
  public Model(String jsonModel) throws ParseException {

    JSONObject completeJsonModel = (JSONObject) JSONValue.parseWithException(jsonModel);

    // attributes
    JSONObject jsonAttribute = (JSONObject) completeJsonModel.get("attributes");
    attributes = new ModelAttributes(jsonAttribute);

    // resolve nodes and edges now

    // nodes
    JSONObject jsonNodes = (JSONObject) completeJsonModel.get("nodes");
    this.nodes = new Node[jsonNodes.size()];
    int index = 0;
    @SuppressWarnings("unchecked")
    Iterator<Map.Entry<String, Object>> nodesEntries = jsonNodes.entrySet().iterator();
    while (nodesEntries.hasNext()) {
      Map.Entry<String, Object> entry = nodesEntries.next();
      String key = entry.getKey();
      JSONObject value = (JSONObject) entry.getValue();
      nodes[index] = new Node(key, value);
      index++;
    }

    // edges
    JSONObject jsonEdges = (JSONObject) completeJsonModel.get("edges");
    this.edges = new Edge[jsonEdges.size()];
    index = 0;
    @SuppressWarnings("unchecked")
    Iterator<Map.Entry<String, Object>> edgesEntries = jsonEdges.entrySet().iterator();
    while (edgesEntries.hasNext()) {
      Map.Entry<String, Object> entry = edgesEntries.next();
      String key = entry.getKey();
      JSONObject value = (JSONObject) entry.getValue();
      edges[index] = new Edge(key, value);
      index++;
    }
  }

  public int getId() {
    return id;
  }

  public Node[] getNodes() {
    return nodes;
  }

  public Edge[] getEdges() {
    return edges;
  }

  public ModelAttributes getAttributes() {
    return attributes;
  }

  /**
   * Returns the JSON representation of this model. The representation is rather specific to
   * SyncMeta and should not be taken as a generic example of a JSON object representation.
   * 
   * @return a JSON object representing a (SyncMeta) compatible model attribute representation
   * 
   */
  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {

    JSONObject jsonModel = new JSONObject();

    // add attributes
    jsonModel.put("attributes", this.attributes.toJSONObject());

    // add nodes
    JSONObject jsonNodes = new JSONObject();
    for (int nodeIndex = 0; nodeIndex < this.nodes.length; nodeIndex++) {
      jsonNodes.put(this.nodes[nodeIndex].getId(), this.nodes[nodeIndex].toJSONObject());
    }
    jsonModel.put("nodes", jsonNodes);

    // add edges
    JSONObject jsonEdges = new JSONObject();
    for (int edgeIndex = 0; edgeIndex < this.edges.length; edgeIndex++) {
      jsonEdges.put(this.edges[edgeIndex].getId(), this.edges[edgeIndex].toJSONObject());
    }
    jsonModel.put("edges", jsonEdges);

    return jsonModel;
  }

  /**
   * Persists a model. For a complete understanding how the model is persisted in a database, please
   * take a look at the SQL script located in the folder "databases". Please note, that the model's
   * name is taken from the label of the model attribute (since there exists no way to add an id to
   * a model directly without breaking the general SyncMeta structure).
   * 
   * @param connection a Connection Object
   * 
   * @return the (new) modelId
   * 
   * @throws SQLException if something with the database has gone wrong
   */
  public int persist(Connection connection) throws SQLException {
    PreparedStatement statement = null;
    try {
      connection.setAutoCommit(false);

      // first store the model itself: formulate empty statement
      statement = connection.prepareStatement("insert into Model () VALUES ();",
          Statement.RETURN_GENERATED_KEYS);
      // execute query
      statement.executeUpdate();
      // get the generated id and close statement
      ResultSet genKeys = statement.getGeneratedKeys();
      genKeys.next();
      this.id = genKeys.getInt(1);
      statement.close();

      // store the model attributes
      this.attributes.persist(connection);
      // modelToModelAttributes entry ("connect" them)
      statement = connection.prepareStatement(
          "INSERT INTO ModelToModelAttributes (modelId, modelAttributesName) VALUES (?, ?);");
      statement.setInt(1, this.id);
      statement.setString(2, this.attributes.getName());
      // execute query
      statement.executeUpdate();
      statement.close();

      // now to the nodes
      for (int i = 0; i < this.nodes.length; i++) {
        nodes[i].persist(connection);
        // nodeToModel entry ("connect" them)
        statement =
            connection.prepareStatement("INSERT INTO NodeToModel (nodeId, modelId) VALUES (?, ?);");
        statement.setString(1, nodes[i].getId());
        statement.setInt(2, this.id);
        statement.executeUpdate();
        statement.close();
      }

      // and edges
      for (int i = 0; i < this.edges.length; i++) {
        edges[i].persist(connection);
        // EdgeToModel entry ("connect" them)
        statement =
            connection.prepareStatement("INSERT INTO EdgeToModel (edgeId, modelId) VALUES (?, ?);");
        statement.setString(1, edges[i].getId());
        statement.setInt(2, this.id);
        statement.executeUpdate();
        statement.close();
      }

      // we got here without errors, so commit now
      connection.commit();
      return this.id;

    } catch (SQLException e) {
      // roll back the whole stuff
      connection.rollback();
      throw e;
    } finally {
      // always free resources
      statement.close();
      connection.close();
    }
  }
}
