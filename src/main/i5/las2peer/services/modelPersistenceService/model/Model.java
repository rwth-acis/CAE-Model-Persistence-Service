package i5.las2peer.services.modelPersistenceService.model;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.SimpleModel;
import i5.cae.simpleModel.edge.SimpleEdge;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.services.modelPersistenceService.model.edge.Edge;
import i5.las2peer.services.modelPersistenceService.model.modelAttributes.ModelAttributes;
import i5.las2peer.services.modelPersistenceService.model.node.Node;
import i5.las2peer.services.modelPersistenceService.model.node.NodePosition;

/**
 * 
 * (Data-)Class for Models. Provides means to convert JSON to Object and Object to JSON. Also
 * provides means to persist the object to a database.
 *
 */
public class Model {
  private int id = -1;
  private ArrayList<Node> nodes;
  private ArrayList<Edge> edges;
  private ModelAttributes attributes;

  /**
   * 
   * Creates a new model from a given JSON representation.
   * 
   * @param jsonModel the attribute as (SyncMeta-compatible) JSON String
   * 
   * @throws ParseException if the parameter is not well formatted
   * 
   */
  public Model(String jsonModel) throws ParseException {

    JSONObject completeJsonModel = (JSONObject) JSONValue.parseWithException(jsonModel);

    // attributes
    JSONObject jsonAttribute = (JSONObject) completeJsonModel.get("attributes");
    this.attributes = new ModelAttributes(jsonAttribute);

    // nodes
    JSONObject jsonNodes = (JSONObject) completeJsonModel.get("nodes");
    this.nodes = new ArrayList<Node>(jsonNodes.size());
    @SuppressWarnings("unchecked")
    Iterator<Map.Entry<String, Object>> nodesEntries = jsonNodes.entrySet().iterator();
    while (nodesEntries.hasNext()) {
      Map.Entry<String, Object> entry = nodesEntries.next();
      String key = entry.getKey();
      JSONObject value = (JSONObject) entry.getValue();
      nodes.add(new Node(key, value));
    }

    // edges
    JSONObject jsonEdges = (JSONObject) completeJsonModel.get("edges");
    this.edges = new ArrayList<Edge>(jsonEdges.size());
    @SuppressWarnings("unchecked")
    Iterator<Map.Entry<String, Object>> edgesEntries = jsonEdges.entrySet().iterator();
    while (edgesEntries.hasNext()) {
      Map.Entry<String, Object> entry = edgesEntries.next();
      String key = entry.getKey();
      JSONObject value = (JSONObject) entry.getValue();
      edges.add(new Edge(key, value));
    }
  }


  /**
   * 
   * Creates a new model by loading it from the database.
   * 
   * @param modelName the name of the model that resides in the database
   * @param connection a Connection Object
   * 
   * @throws SQLException if the model is not found or something else went wrong
   * 
   */
  public Model(String modelName, Connection connection) throws SQLException {
    PreparedStatement statement;
    // create empty node and edge lists
    this.nodes = new ArrayList<Node>();
    this.edges = new ArrayList<Edge>();

    // load attributes
    this.attributes = new ModelAttributes(modelName, connection);

    // now we know the model name, set own id
    statement = connection.prepareStatement(
        "SELECT modelId FROM ModelToModelAttributes WHERE modelAttributesName = ?;");
    statement.setString(1, this.attributes.getName());
    ResultSet queryResult = statement.executeQuery();
    while (queryResult.next()) {
      this.id = queryResult.getInt(1);
    }
    statement.close();

    // load nodes
    statement = connection.prepareStatement("SELECT nodeId FROM NodeToModel WHERE modelId = ?;");
    statement.setInt(1, this.id);
    queryResult = statement.executeQuery();
    while (queryResult.next()) {
      this.nodes.add(new Node(queryResult.getString(1), connection));
    }
    statement.close();

    // load edges
    statement = connection.prepareStatement("SELECT edgeId FROM EdgeToModel WHERE modelId = ?;");
    statement.setInt(1, this.id);
    queryResult = statement.executeQuery();
    while (queryResult.next()) {
      this.edges.add(new Edge(queryResult.getString(1), connection));
    }
    statement.close();
  }

  /**
   * 
   * Creates a new model from a passed on {@link i5.cae.simpleModel.SimpleModel}. Please be aware
   * that the positioning of the objects does not take into account overlapping edges between them.
   * 
   * @param simpleModel a {@link i5.cae.simpleModel.SimpleModel} containing the model that is to be
   *        created
   * 
   */
  public Model(SimpleModel simpleModel) {
    // create empty node and edge lists
    this.nodes = new ArrayList<Node>();
    this.edges = new ArrayList<Edge>();
    // create attributes
    this.attributes = new ModelAttributes(simpleModel.getName(), simpleModel.getAttributes());

    // now "initialize" the first node position (starting values are derived from observations and
    // experiments;-) )
    NodePosition currentNodePosition = new NodePosition(4000, 4000, 100, 60, 16001);

    // create nodes
    for (SimpleNode node : simpleModel.getNodes()) {
      this.nodes.add(new Node(node, currentNodePosition));
      // change next node position
      currentNodePosition = new NodePosition(currentNodePosition.getLeft() + 50,
          currentNodePosition.getTop() + 50, 100, 100, currentNodePosition.getzIndex() + 1);
    }

    // create edges
    for (SimpleEdge edge : simpleModel.getEdges()) {
      this.edges.add(new Edge(edge));
    }
  }


  public int getId() {
    return id;
  }


  public ArrayList<Node> getNodes() {
    return nodes;
  }


  public ArrayList<Edge> getEdges() {
    return edges;
  }


  public ModelAttributes getAttributes() {
    return attributes;
  }


  /**
   * 
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
    for (int nodeIndex = 0; nodeIndex < this.nodes.size(); nodeIndex++) {
      jsonNodes.put(this.nodes.get(nodeIndex).getId(), this.nodes.get(nodeIndex).toJSONObject());
    }
    jsonModel.put("nodes", jsonNodes);

    // add edges
    JSONObject jsonEdges = new JSONObject();
    for (int edgeIndex = 0; edgeIndex < this.edges.size(); edgeIndex++) {
      jsonEdges.put(this.edges.get(edgeIndex).getId(), this.edges.get(edgeIndex).toJSONObject());
    }
    jsonModel.put("edges", jsonEdges);

    return jsonModel;
  }


  /**
   * 
   * Persists a model. For a complete understanding how the model is persisted in a database, please
   * take a look at the SQL script located in the folder "databases". Please note, that the model's
   * name is taken from the label of the model attribute (since there exists no way to add an id to
   * a model directly without breaking the general SyncMeta structure).
   * 
   * @param connection a Connection Object
   * 
   * @throws SQLException if something with the database has gone wrong
   * 
   */
  public void persist(Connection connection) throws SQLException {
    PreparedStatement statement;
    try {
      connection.setAutoCommit(false);

      // first store the model itself: formulate empty statement
      statement = connection.prepareStatement("INSERT INTO Model () VALUES ();",
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
      for (int i = 0; i < this.nodes.size(); i++) {
        nodes.get(i).persist(connection);
        // nodeToModel entry ("connect" them)
        statement =
            connection.prepareStatement("INSERT INTO NodeToModel (nodeId, modelId) VALUES (?, ?);");
        statement.setString(1, nodes.get(i).getId());
        statement.setInt(2, this.id);
        statement.executeUpdate();
        statement.close();
      }

      // and edges
      for (int i = 0; i < this.edges.size(); i++) {
        edges.get(i).persist(connection);
        // EdgeToModel entry ("connect" them)
        statement =
            connection.prepareStatement("INSERT INTO EdgeToModel (edgeId, modelId) VALUES (?, ?);");
        statement.setString(1, edges.get(i).getId());
        statement.setInt(2, this.id);
        statement.executeUpdate();
        statement.close();
      }

      // we got here without errors, so commit now
      connection.commit();

    } catch (SQLException e) {
      // roll back the whole stuff
      connection.rollback();
      throw e;
    }
  }


  /**
   * 
   * Deletes a model from the database;
   * 
   * @param connection a Connection object
   * 
   * @return true, if model was be deleted
   * 
   */
  public boolean deleteFromDatabase(Connection connection) {

    // first test if model contains something
    // (was "created" either by database access or JSONObject)
    if (this.getAttributes() == null) {
      return false;
    }
    // test, if was model is already "loaded" (synchronized)
    if (this.id == -1) {
      // model not loaded, try loading it and call delete again
      Model model;
      try {
        model = new Model(this.getAttributes().getName(), connection);
      } catch (SQLException e) {
        return false;
      }
      model.deleteFromDatabase(connection);
    }
    // actually delete the model
    else {
      // delete model
      PreparedStatement statement;
      try {
        connection.setAutoCommit(false);
        statement = connection.prepareStatement("DELETE FROM Model WHERE modelId = ?;");
        statement.setInt(1, this.id);
        statement.executeUpdate();
        statement.close();

        // delete model attributes
        this.attributes.deleteFromDatabase(connection);
        // now to the nodes
        for (int i = 0; i < this.nodes.size(); i++) {
          nodes.get(i).deleteFromDatabase(connection);
        }
        // and edges
        for (int i = 0; i < this.edges.size(); i++) {
          edges.get(i).deleteFromDatabase(connection);
        }

        // we got here without errors, so commit now
        connection.commit();
      } catch (SQLException e) {
        e.printStackTrace();
        try {
          connection.rollback();
        } catch (SQLException e1) {
          e1.printStackTrace();
        }
        return false;
      }
    }
    this.id = -1; // model does not exist in the database anymore
    return true;
  }


  /**
   * 
   * Simplifies a model to send it to the CAE-Code-Generation-Service. Removes all obsolete
   * attributes and methods and returns a serializable ready-to send-model representation.
   * 
   * 
   * @return a {@link java.io.Serializable} representation of a {@link SimpleModel}
   * 
   */
  public Serializable getMinifiedRepresentation() {
    ArrayList<SimpleNode> simpleNodes = new ArrayList<SimpleNode>(this.nodes.size());
    ArrayList<SimpleEdge> simpleEdges = new ArrayList<SimpleEdge>(this.edges.size());
    ArrayList<SimpleEntityAttribute> simpleModelAttributes =
        new ArrayList<SimpleEntityAttribute>(this.attributes.getAttributes().size());

    // "simplify" nodes
    for (int i = 0; i < this.nodes.size(); i++) {
      Node node = this.nodes.get(i);
      // "simplify" attributes of node
      ArrayList<SimpleEntityAttribute> simpleAttributesOfNode =
          new ArrayList<SimpleEntityAttribute>(node.getAttributes().size());
      for (int j = 0; j < node.getAttributes().size(); j++) {
        EntityAttribute attribute = node.getAttributes().get(j);
        SimpleEntityAttribute simpleAttribute = new SimpleEntityAttribute(attribute.getSyncMetaId(),
            attribute.getName(), attribute.getValue());
        simpleAttributesOfNode.add(simpleAttribute);
      }
      SimpleNode simpleNode = new SimpleNode(node.getId(), node.getType(), simpleAttributesOfNode);
      simpleNodes.add(simpleNode);
    }

    // "simplify" edges
    for (int i = 0; i < this.edges.size(); i++) {
      Edge edge = this.edges.get(i);
      // "simplify" attributes of edge
      ArrayList<SimpleEntityAttribute> simpleAttributesOfEdge =
          new ArrayList<SimpleEntityAttribute>(edge.getAttributes().size());
      for (int j = 0; j < edge.getAttributes().size(); j++) {
        EntityAttribute attribute = edge.getAttributes().get(j);
        SimpleEntityAttribute simpleAttribute = new SimpleEntityAttribute(attribute.getSyncMetaId(),
            attribute.getName(), attribute.getValue());
        simpleAttributesOfEdge.add(simpleAttribute);
      }
      SimpleEdge simpleEdge = new SimpleEdge(edge.getId(), edge.getSourceNode(),
          edge.getTargetNode(), edge.getType(), edge.getLabelValue(), simpleAttributesOfEdge);
      simpleEdges.add(simpleEdge);
    }

    // "simplify" modelAttributes
    for (int i = 0; i < this.attributes.getAttributes().size(); i++) {
      EntityAttribute attribute = this.attributes.getAttributes().get(i);
      SimpleEntityAttribute simpleAttribute = new SimpleEntityAttribute(attribute.getSyncMetaId(),
          attribute.getName(), attribute.getValue());
      simpleModelAttributes.add(simpleAttribute);
    }

    SimpleModel simpleModel =
        new SimpleModel(this.attributes.getName(), simpleNodes, simpleEdges, simpleModelAttributes);
    return simpleModel;
  }

}
