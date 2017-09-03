package i5.las2peer.services.modelPersistenceService.modelServices;

import java.util.Date;
import java.util.ArrayList;
import java.util.UUID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.modelPersistenceService.model.metadata.ElementToElement;

public class ElementService {
    private Connection _connection;
    private L2pLogger _logger;
    private String _logPrefix = "[Element Service] - %s";

    /**
     * Constructor
     * @param connection database connection
     * @param logger logger
     */
    public ElementService(Connection connection, L2pLogger logger) {
        _connection = connection;
        _logger = logger;
        _logger.info(String.format(_logPrefix, "Construct new element service"));
    }

    /**
     * Map sql result set to object
     * @param queryResult result set to convert 
     */
    private ElementToElement mapResultSetToObject(ResultSet queryResult) throws SQLException {
        _logger.info(String.format(_logPrefix, "Mapping result set to ElementToElement object"));
        try {
            String id = queryResult.getString("id");
            String firstElementId = queryResult.getString("firstElementId");
            String secondElementId = queryResult.getString("secondElementId");
            Date timeCreated = queryResult.getDate("timeCreated");
            Date timeEdited = queryResult.getDate("timeEdited");
            ElementToElement model = new ElementToElement(id, firstElementId, secondElementId, timeCreated, timeEdited);
            return model;
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }

        return new ElementToElement();
    }

    /**
     * Get list of all element to element connection
     * @return list of all element to element connection
     */
    public ArrayList<ElementToElement> getAll() throws SQLException {
        ArrayList<ElementToElement> result = new ArrayList<ElementToElement>();
        try {
            String query = "SELECT * FROM ElementToElement";
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
     * Get element to element connection by id
     * @param queryId id of element to element connection
     * @return founded element to element connection
     */
    public ElementToElement getById(String queryId) throws SQLException {
        try {
            PreparedStatement sqlQuery;
            sqlQuery = _connection.prepareStatement("SELECT * FROM ElementToElement WHERE id = ?;");
            sqlQuery.setString(1, queryId);
            _logger.info(String.format(_logPrefix, "Executing GET BY ID query with id " + queryId));
            ResultSet queryResult = sqlQuery.executeQuery();
            if(queryResult.next()) {
                ElementToElement model = mapResultSetToObject(queryResult);
                sqlQuery.close();
                return model;
            } else {
            	throw new SQLException("Could not find element to element connection!");
            }
            
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
        
        return new ElementToElement();
    }

    /**
	 * Insert new element to element connection
	 * @param insertModel model to insert
	 */
    public void create(ElementToElement insertModel) throws SQLException {
        try {
            String uniqueID = UUID.randomUUID().toString();
            PreparedStatement sqlQuery = _connection.prepareStatement(
				"INSERT INTO ElementToElement(id, firstElementId, secondElementId) VALUES (?,?,?);");
            sqlQuery.setString(1, uniqueID);
            sqlQuery.setString(2, insertModel.getFirstElementId());
            sqlQuery.setString(3, insertModel.getSecondElementId());
            _logger.info(String.format(_logPrefix, "Executing CREATE query"));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
	}

    /**
	 * Update element to element in database
	 * @param updateModel model to update
	 */
    public void update(ElementToElement updateModel) throws SQLException {
        try {
            PreparedStatement sqlQuery = _connection.prepareStatement(
				"UPDATE ElementToElement SET firstElementId=?, secondElementId=? WHERE id=?;");
            sqlQuery.setString(1, updateModel.getFirstElementId());
            sqlQuery.setString(2, updateModel.getSecondElementId());
            sqlQuery.setString(3, updateModel.getId());
            _logger.info(String.format(_logPrefix, "Executing UPDATE query for id " + updateModel.getId()));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
	}

    /**
	 * Deletes element to element connection from database.
	 * @param queryId id to delete
	 */
	public void delete(String queryId) throws SQLException {
        try {
            PreparedStatement sqlQuery = _connection.prepareStatement("DELETE FROM ElementToElement WHERE id = ?;");
            sqlQuery.setString(1, queryId);
            _logger.info(String.format(_logPrefix, "Executing DELETE query with id " + queryId));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
	}
}