package i5.las2peer.services.modelPersistenceService.models;

import org.json.simple.JSONObject;

public class EntityAttribute {
  private String id;
  private String name;
  private String value;

  public EntityAttribute(String id, JSONObject jsonAttribute) {
    // fetch "value" entry from attributes (JSON format is a bit redundant due to frontend reasons)
    jsonAttribute = (JSONObject) jsonAttribute.get("value");
    this.id = id;
    this.name = (String) jsonAttribute.get("name");
    this.value = (String) jsonAttribute.get("value");
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

}
