package i5.las2peer.services.modelPersistenceService.models.edges;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;

import i5.las2peer.services.modelPersistenceService.models.EntityAttribute;

public class Edge {
  private String id;
  private String sourceNode;
  private String targetNode;
  private String type;
  // an edge has a label value (in comparison to a node which uses the "name" value here)
  private String labelValue;
  private EntityAttribute[] attributes;

  /*
   * Creates a new edge.
   * 
   * @param id the node id
   * 
   * @param jsonEdge the content of the edge entry in the (JSON-represented) model
   * 
   */
  public Edge(String id, JSONObject jsonEdge) {
    this.id = id;
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
    this.attributes = new EntityAttribute[jsonAttributes.size()];
    @SuppressWarnings("unchecked")
    Iterator<Map.Entry<String, Object>> jsonAttribute = jsonAttributes.entrySet().iterator();
    int attributeIndex = 0;
    while (jsonAttribute.hasNext()) {
      Map.Entry<String, Object> entry = jsonAttribute.next();
      String attributeId = entry.getKey();
      JSONObject attribute = (JSONObject) entry.getValue();
      attributes[attributeIndex] = new EntityAttribute(attributeId, attribute);
      attributeIndex++;
    }
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

  public EntityAttribute[] getAttributes() {
    return this.attributes;
  }

  public String getLabelValue() {
    return this.labelValue;
  }

  /*
   * Returns the JSON representation of this edge. This representation is rather specific to
   * SyncMeta and should not be taken as a generic example of a JSON object representation.
   * 
   * @return a JSON object as a string representing a (SyncMeta) compatible edge representation
   * 
   */
  @SuppressWarnings("unchecked")
  public String toJSONString() {
    // main object
    JSONObject edge = new JSONObject();
    // content of main object
    Map<String, Object> edgeContent = new HashMap<String, Object>();

    // start with the source, target and type
    edgeContent.put("source", this.sourceNode);
    edgeContent.put("target", this.targetNode);
    edgeContent.put("type", this.type);

    // label element of edgeContent
    Map<String, Object> label = new HashMap<String, Object>();
    label.put("id", this.id + "[label]");
    // currently, SyncMeta supports only "Label" as label for edges.
    label.put("name", "Label");
    Map<String, Object> labelValue = new HashMap<String, Object>();
    labelValue.put("id", this.id + "[label]");
    labelValue.put("name", "Label");
    labelValue.put("value", this.getLabelValue());
    label.put("value", labelValue);
    edgeContent.put("label", label);

    // attribute element of edgeContent
    Map<String, Object> attributes = new HashMap<String, Object>();
    for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
      EntityAttribute currentAttribute = this.attributes[attributeIndex];
      Map<String, Object> attributeContent = new HashMap<String, Object>();
      attributeContent.put("id", this.id + "[" + currentAttribute.getName() + "]");
      attributeContent.put("name", currentAttribute.getName());

      // value of attribute
      Map<String, Object> attributeValue = new HashMap<String, Object>();
      attributeValue.put("id", this.id + "[" + currentAttribute.getName() + "]");
      attributeValue.put("name", currentAttribute.getName());
      attributeValue.put("value", currentAttribute.getValue());
      attributeContent.put("value", attributeValue);

      // add attribute to attribute list with the attribute's id as key
      attributes.put(currentAttribute.getId(), attributeContent);
    }
    edgeContent.put("attributes", attributes);

    // finally add content to edge
    edge.put(this.id, edgeContent);
    return edge.toJSONString();
  }

}
