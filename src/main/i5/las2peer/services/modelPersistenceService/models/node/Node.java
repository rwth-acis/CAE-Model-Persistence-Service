package i5.las2peer.services.modelPersistenceService.models.node;

import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;

import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;

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

    // parse attributes
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
    for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
      EntityAttribute currentAttribute = this.attributes[attributeIndex];
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
      attributes.put(currentAttribute.getId(), attributeContent);

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

}
