package i5.las2peer.services.modelPersistenceService.models;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

public class ModelAttributes {
  private String name; // serves also as unique id

  /*
   * Creates a new model attribute entry.
   * 
   * @param jsonAttribute the attribute as (SyncMeta) JSON file
   * 
   */
  public ModelAttributes(JSONObject jsonAttribute) {
    // get the name (never mind input structure here, its non straight-forward..)
    this.name =
        (String) ((JSONObject) ((JSONObject) jsonAttribute.get("label")).get("value")).get("value");
  }

  public String getName() {
    return this.name;
  }

  /*
   * Returns the JSON representation of this model attribute. The representation is rather specific
   * to SyncMeta and should not be taken as a generic example of a JSON object representation.
   * 
   * @return a JSON object representing a (SyncMeta) compatible model attribute representation
   * 
   */
  @SuppressWarnings("unchecked")
  public JSONObject toJSONObject() {
    // main object
    JSONObject modelAttribute = new JSONObject();
    // start with the (empty) position elements and type
    modelAttribute.put("left", "0");
    modelAttribute.put("top", "0");
    modelAttribute.put("width", "0");
    modelAttribute.put("height", "0");
    modelAttribute.put("zIndex", "0");
    modelAttribute.put("type", "ModelAttributesNode");


    // label element of modelAttributeContent
    Map<String, Object> label = new HashMap<String, Object>();
    label.put("id", "modelAttributes[label]");
    label.put("name", "Label");
    Map<String, Object> labelValue = new HashMap<String, Object>();
    labelValue.put("id", "modelAttributes[label]");
    labelValue.put("name", "Label");
    labelValue.put("value", this.getName());
    label.put("value", labelValue);
    modelAttribute.put("label", label);

    // attribute element of modelAttributeContent (currently empty)
    modelAttribute.put("attributes", "");

    return modelAttribute;
  }
}
