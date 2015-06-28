package i5.las2peer.services.modelPersistenceService.models;

import i5.las2peer.services.modelPersistenceService.models.edges.Edge;
import i5.las2peer.services.modelPersistenceService.models.nodes.Node;

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

}
