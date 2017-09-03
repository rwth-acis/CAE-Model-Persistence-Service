package i5.las2peer.services.modelPersistenceService.modelServices;

import java.util.Date;
import java.util.ArrayList;
import java.util.UUID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.modelPersistenceService.model.metadata.MetadataDoc;

public class MetadataDocService {
    private Connection _connection;
    private L2pLogger _logger;
    private String _logPrefix = "[MetadataDoc Service] - %s";

    /**
     * Constructor
     * @param connection database connection
     * @param logger logger
     */
    public MetadataDocService(Connection connection, L2pLogger logger) {
        _connection = connection;
        _logger = logger;
        _logger.info(String.format(_logPrefix, "Construct new element service"));
    }

    /**
     * Map sql result set to object
     * @param queryResult result set to convert 
     */
    private MetadataDoc mapResultSetToObject(ResultSet queryResult) throws SQLException {
        _logger.info(String.format(_logPrefix, "Mapping result set to MetadataDoc object"));
        try {
            String id = queryResult.getString("id");
            String componentId = queryResult.getString("componentId");
            String docType = queryResult.getString("docType");
            String docString = queryResult.getString("docString");
            Date timeCreated = queryResult.getDate("timeCreated");
            Date timeEdited = queryResult.getDate("timeEdited");
            MetadataDoc model = new MetadataDoc(id, componentId, docType, docString, timeCreated, timeEdited);
            return model;
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }

        return new MetadataDoc();
    }

    /**
     * Get list of all metadata doc
     * @return list of all metadata doc
     */
    public ArrayList<MetadataDoc> getAll() throws SQLException {
        ArrayList<MetadataDoc> result = new ArrayList<MetadataDoc>();
        try {
            String query = "SELECT * FROM MetadataDoc";
            PreparedStatement sqlQuery;
            sqlQuery = _connection.prepareStatement(query);
            _logger.info(String.format(_logPrefix, "Executing GET ALL query " + query));
            ResultSet queryResult = sqlQuery.executeQuery();
            while(queryResult.next()) {
                result.add(mapResultSetToObject(queryResult));
            }
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
        
        return result;
    }
    
    /**
     * Get metadata doc by id
     * @param queryId id of metadata doc
     * @return founded metadata doc
     */
    public MetadataDoc getById(String queryId) throws SQLException {
        try {
            PreparedStatement sqlQuery;
            sqlQuery = _connection.prepareStatement("SELECT * FROM MetadataDoc WHERE id = ?;");
            sqlQuery.setString(1, queryId);
            _logger.info(String.format(_logPrefix, "Executing GET BY ID query with id " + queryId));
            ResultSet queryResult = sqlQuery.executeQuery();
            if(queryResult.next()) {
                MetadataDoc model = mapResultSetToObject(queryResult);
                sqlQuery.close();
                return model;
            } else {
            	throw new SQLException("Could not find metadata doc!");
            }
            
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
        
        return new MetadataDoc();
    }

    /**
     * Get metadata doc connection by component id
     * @param queryId id of metadata doc
     * @return founded metadata doc
     */
    public MetadataDoc getByComponentId(String queryId) throws SQLException {
        try {
            PreparedStatement sqlQuery;
            sqlQuery = _connection.prepareStatement("SELECT * FROM MetadataDoc WHERE componentId = ?;");
            sqlQuery.setString(1, queryId);
            _logger.info(String.format(_logPrefix, "Executing GET BY ID query with componentId " + queryId));
            ResultSet queryResult = sqlQuery.executeQuery();
            if(queryResult.next()) {
                MetadataDoc model = mapResultSetToObject(queryResult);
                sqlQuery.close();
                return model;
            } else {
            	throw new SQLException("Could not find metadata doc!");
            }
            
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
        
        return new MetadataDoc();
    }

    /**
     * Get metadata doc string by component id
     * @param queryId id of metadata doc
     * @return founded metadata doc string
     */
    public String getMetadataDocStringByComponentId(String queryId) {
        try {
            return getByComponentId(queryId).getDocString();
        } catch (SQLException e) {
            return "";
        }
    }


    /**
	 * Insert new metadata doc
	 * @param insertModel model to insert
	 */
    public void create(MetadataDoc insertModel) throws SQLException {
        try {
            String uniqueID = UUID.randomUUID().toString();
            PreparedStatement sqlQuery = _connection.prepareStatement(
				"INSERT INTO MetadataDoc(id, componentId, docString, docType) VALUES (?,?,?,?);");
            sqlQuery.setString(1, uniqueID);
            sqlQuery.setString(2, insertModel.getComponentId());
            sqlQuery.setString(3, insertModel.getDocString());
            sqlQuery.setString(4, insertModel.getDocType());
            _logger.info(String.format(_logPrefix, "Executing CREATE query"));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
	}

    /**
	 * Update metadata doc in database
	 * @param updateModel model to update
	 */
    public void update(MetadataDoc updateModel) throws SQLException {
        try {
            PreparedStatement sqlQuery = _connection.prepareStatement(
				"UPDATE MetadataDoc SET componentId=?, docString=?, docType=? WHERE id=?;");
            sqlQuery.setString(1, updateModel.getComponentId());
            sqlQuery.setString(2, updateModel.getDocString());
            sqlQuery.setString(3, updateModel.getDocType());
            sqlQuery.setString(4, updateModel.getId());
            _logger.info(String.format(_logPrefix, "Executing UPDATE query for id " + updateModel.getId()));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
	}

    /**
	 * Deletes doc from database.
	 * @param queryId id to delete
	 */
	public void delete(String queryId) throws SQLException {
        try {
            PreparedStatement sqlQuery = _connection.prepareStatement("DELETE FROM MetadataDoc WHERE id = ?;");
            sqlQuery.setString(1, queryId);
            _logger.info(String.format(_logPrefix, "Executing DELETE query with id " + queryId));
            _logger.info(String.format(_logPrefix, "Executing DELETE query with id " + queryId));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
	}
}