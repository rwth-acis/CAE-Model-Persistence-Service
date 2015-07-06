package i5.las2peer.services.modelPersistenceService.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.simple.JSONObject;

import i5.las2peer.services.modelPersistenceService.models.edge.Edge;
import i5.las2peer.services.modelPersistenceService.models.modelAttribute.ModelAttributes;
import i5.las2peer.services.modelPersistenceService.models.node.Node;

public class Model {
  private int id;
  private Node[] nodes;
  private Edge[] edges;
  private ModelAttributes attributes;

  public Model(ModelAttributes attributes, Node[] nodes, Edge[] edges) {
    this.nodes = nodes;
    this.edges = edges;
    this.attributes = attributes;
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
          "insert into ModelToModelAttributes (modelId, modelAttributesName) VALUES (?, ?);");
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
            connection.prepareStatement("insert into NodeToModel (nodeId, modelId) VALUES (?, ?);");
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
            connection.prepareStatement("insert into EdgeToModel (edgeId, modelId) VALUES (?, ?);");
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
