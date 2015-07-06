package i5.las2peer.services.modelPersistenceService.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.simple.JSONObject;

/**
 * 
 * (Data-)Class for EntityAttributes. Provides means to convert JSON to Object and Object to JSON.
 * Also provides means to persist the object to a database.
 *
 */
public class EntityAttribute {
  private int id = -1; // given by the database
  private String syncMetaId; // each attribute type shares the same id according to its metamodel
  private String name;
  private String value;

  /**
   * 
   * Creates a new EntityAttribute.
   * 
   * @param syncMetaId the id of the object created by SyncMeta
   * 
   * @param jsonAttribute the content of the attribute in the (JSON-represented) model
   * 
   */
  public EntityAttribute(String syncMetaId, JSONObject jsonAttribute) {
    // fetch "value" entry from attributes (JSON format is a bit redundant due to frontend reasons)
    jsonAttribute = (JSONObject) jsonAttribute.get("value");
    this.syncMetaId = syncMetaId;
    this.name = (String) jsonAttribute.get("name");
    this.value = (String) jsonAttribute.get("value");
  }

  public int getId() {
    return id;
  }

  public String getSyncMetaId() {
    return syncMetaId;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  /**
   * Persists an Entity Attribute.
   * 
   * @param connection a Connection object
   * 
   * @throws SQLException if something went wrong persisting the entity attribute
   */
  public void persist(Connection connection) throws SQLException {
    // Attribute entry
    if (this.id == -1) {
      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO Attribute (syncMetaId, name, value) VALUES (?,?,?);",
          Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, this.syncMetaId);
      statement.setString(2, this.name);
      statement.setString(3, this.value);
      statement.executeUpdate();
      ResultSet genKeys = statement.getGeneratedKeys();
      genKeys.next();
      // set given id
      this.id = genKeys.getInt(1);
      statement.close();
    } else {
      PreparedStatement statement = connection.prepareStatement(
          "UPDATE Attribute SET syncMetaId = ?, name = ?, value = ? WHERE attributeId=?;");
      statement.setString(1, this.syncMetaId);
      statement.setString(2, this.name);
      statement.setString(3, this.value);
      statement.setInt(4, this.id);
      statement.executeUpdate();
      statement.close();
    }
  }

}
