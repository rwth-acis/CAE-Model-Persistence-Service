package i5.las2peer.services.modelPersistenceService.models.nodes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;

import i5.las2peer.services.modelPersistenceService.models.EntityAttribute;

public class Node {

  private String id;
  private String name;
  private NodePosition position;
  private String type;
  private EntityAttribute[] attributes;

  /*
   * Creates a new node.
   * 
   * @param id the node id
   * 
   * @param jsonNode the content of the node entry in the (JSON-represented) model
   * 
   */
  public Node(String id, JSONObject jsonNode) {
    this.id = id;
    this.type = (String) jsonNode.get("type");

    this.position = new NodePosition((int) ((Number) jsonNode.get("left")).intValue(),
        ((Number) jsonNode.get("top")).intValue(), ((Number) jsonNode.get("width")).intValue(),
        ((Number) jsonNode.get("height")).intValue(), ((Number) jsonNode.get("zIndex")).intValue());

    JSONObject jsonAttributes = (JSONObject) jsonNode.get("attributes");
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

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public NodePosition getPosition() {
    return position;
  }

  public String getType() {
    return type;
  }


  public EntityAttribute[] getAttributes() {
    return attributes;
  }


  /*
   * Returns the JSON representation of this node. This representation is rather specific to
   * SyncMeta and should not be taken as a generic example of a JSON object representation.
   * 
   * @return a JSON object as a string representing a (SyncMeta) compatible node representation
   * 
   */
  @SuppressWarnings("unchecked")
  public String toJSONString() {
    // main object
    JSONObject node = new JSONObject();
    // content of main object
    Map<String, Object> nodeContent = new HashMap<String, Object>();

    // start with the position elements and type
    nodeContent.put("left", position.getLeft());
    nodeContent.put("top", position.getTop());
    nodeContent.put("width", position.getWidth());
    nodeContent.put("height", position.getHeight());
    nodeContent.put("zIndex", position.getzIndex());
    nodeContent.put("type", type);

    // label element of nodeContent
    Map<String, Object> label = new HashMap<String, Object>();
    label.put("id", this.id + "[name]");
    // currently, SyncMeta supports only "name" as label. Still, entry has to be there.
    label.put("name", "name");
    Map<String, Object> labelValue = new HashMap<String, Object>();
    labelValue.put("id", this.id + "[name]");
    labelValue.put("name", "name");
    // at this point we have to "wait" for the attributes to be read out, since the "value" of the
    // name can be derived from the attribute with the name "name"

    // attribute element of nodeContent
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

      // check for name of label, if found, add value to label
      if (currentAttribute.getName().equals("name")) {
        labelValue.put("value", currentAttribute.getValue());
        label.put("value", labelValue);
      }

    }

    // add label and attributes to node content, then finally add content to node
    nodeContent.put("label", label);
    nodeContent.put("attributes", attributes);
    node.put(this.id, nodeContent);
    return node.toJSONString();
  }


}
