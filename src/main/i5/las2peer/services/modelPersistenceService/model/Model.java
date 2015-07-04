package i5.las2peer.services.modelPersistenceService.model;

import org.json.simple.JSONObject;

import i5.las2peer.services.modelPersistenceService.models.edge.Edge;
import i5.las2peer.services.modelPersistenceService.models.modelAttribute.ModelAttributes;
import i5.las2peer.services.modelPersistenceService.models.node.Node;

public class Model {
  private Node[] nodes;
  private Edge[] edges;
  private ModelAttributes attributes;


  public Model(ModelAttributes attributes, Node[] nodes, Edge[] edges) {
    this.nodes = nodes;
    this.edges = edges;
    this.attributes = attributes;
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

  public String getName() {
    return attributes.getName();
  }

  /*
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
}
