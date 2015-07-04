package i5.las2peer.services.modelPersistenceService.models.modelAttribute;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;

import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;

public class ModelAttributes {
  private String name; // serves also as unique id
  private EntityAttribute[] attributes; // meta-data
  
  /*
   * Creates a new model attribute entry.
   * 
   * @param jsonAttribute the attribute as (SyncMeta) JSON file
   * 
   */
  public ModelAttributes(JSONObject jsonModelAttribute) {
    // get the name (never mind input structure here, its non straight-forward..)
    this.name =
        (String) ((JSONObject) ((JSONObject) jsonModelAttribute.get("label")).get("value")).get("value");
    
    // parse attributes
    JSONObject jsonAttributes = (JSONObject) jsonModelAttribute.get("attributes");
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

  public String getName() {
    return this.name;
  }

  public EntityAttribute[] getAttributes() {
    return this.attributes;
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
    JSONObject attributes = new JSONObject();
    for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
      EntityAttribute currentAttribute = this.attributes[attributeIndex];
      JSONObject attributeContent = new JSONObject();
      attributeContent.put("id", "modelAttributes[" + currentAttribute.getName() + "]");
      attributeContent.put("name", currentAttribute.getName());

      // value of attribute
      JSONObject attributeValue = new JSONObject();
      attributeValue.put("id", "modelAttributes[" + currentAttribute.getName() + "]");
      attributeValue.put("name", currentAttribute.getName());
      attributeValue.put("value", currentAttribute.getValue());
      attributeContent.put("value", attributeValue);

      // add attribute to attribute list with the attribute's id as key
      attributes.put(currentAttribute.getId(), attributeContent);
    }
    modelAttribute.put("attributes", attributes);
    return modelAttribute;
  }
}
