package i5.las2peer.services.modelPersistenceService.modelServices;

import java.util.Date;
import java.util.ArrayList;
import java.util.UUID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.modelPersistenceService.model.metadata.ComponentToComponent;

public class ComponentService {
    private Connection _connection;
    private L2pLogger _logger;
    private String _logPrefix = "[Component Service] - %s";

    /**
     * Constructor
     * @param connection database connection
     * @param logger logger
     */
    public ComponentService(Connection connection, L2pLogger logger) {
        _connection = connection;
        _logger = logger;
        _logger.info(String.format(_logPrefix, "Construct new component service"));
    }

    /**
     * Map sql result set to object
     * @param queryResult result set to convert 
     */
    private ComponentToComponent mapResultSetToObject(ResultSet queryResult) throws SQLException {
        _logger.info(String.format(_logPrefix, "Mapping result set to ComponentToComponent object"));
        try {
            String id = queryResult.getString("id");
            String firstComponentId = queryResult.getString("firstComponentId");
            String secondComponentId = queryResult.getString("secondComponentId");
            Date timeCreated = queryResult.getDate("timeCreated");
            Date timeEdited = queryResult.getDate("timeEdited");
            ComponentToComponent model = new ComponentToComponent(id, firstComponentId, secondComponentId, timeCreated, timeEdited);
            return model;
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }

        return new ComponentToComponent();
    }

    /**
     * Get list of all component to component connection
     * @return list of all component to component connection
     */
    public ArrayList<ComponentToComponent> getAll() throws SQLException {
        ArrayList<ComponentToComponent> result = new ArrayList<ComponentToComponent>();
        try {
            PreparedStatement sqlQuery;
            String query = "SELECT * FROM ComponentToComponent";
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
     * Get component to component connection by id
     * @param queryId id of component to component connection
     * @return founded component to component connection
     */
    public ComponentToComponent getById(String queryId) throws SQLException {
        try {
            PreparedStatement sqlQuery;
            sqlQuery = _connection.prepareStatement("SELECT * FROM ComponentToComponent WHERE id = '?';");
            sqlQuery.setString(1, queryId);
            _logger.info(String.format(_logPrefix, "Executing GET BY ID query with id " + queryId));
            ResultSet queryResult = sqlQuery.executeQuery();
            if(queryResult.next()) {
                ComponentToComponent model = mapResultSetToObject(queryResult);
                sqlQuery.close();
                return model;
            } else {
            	throw new SQLException("Could not find element to element connection!");
            }
            
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
        
        return new ComponentToComponent();
    }

    /**
	 * Insert new component to component connection
	 * @param insertModel model to insert
	 */
    public void create(ComponentToComponent insertModel) throws SQLException {
        try {
            String uniqueID = UUID.randomUUID().toString();
            PreparedStatement sqlQuery = _connection.prepareStatement(
                "INSERT INTO ComponentToComponent(id, firstComponentId, secondComponentId) VALUES (?,?,?);");
            sqlQuery.setString(1, uniqueID);
            sqlQuery.setString(2, insertModel.getFirstComponentId());
            sqlQuery.setString(3, insertModel.getSecondComponentId());
            _logger.info(String.format(_logPrefix, "Executing CREATE query"));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
	}

    /**
	 * Update component to component in database
	 * @param updateModel model to update
	 */
    public void update(ComponentToComponent updateModel) throws SQLException {
        try {
            PreparedStatement sqlQuery = _connection.prepareStatement(
                "UPDATE ComponentToComponent SET firstComponentId=?, secondComponentId=? WHERE id=?;");
            sqlQuery.setString(1, updateModel.getFirstComponentId());
            sqlQuery.setString(2, updateModel.getSecondComponentId());
            sqlQuery.setString(3, updateModel.getId());
            _logger.info(String.format(_logPrefix, "Executing UPDATE query for id " + updateModel.getId()));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
	}

    /**
	 * Deletes component to component connection from database.
	 * @param queryId id to delete
	 */
	public void delete(String queryId) throws SQLException {
        try {
            PreparedStatement sqlQuery = _connection.prepareStatement("DELETE FROM ComponentToComponent WHERE id = ?;");
            sqlQuery.setString(1, queryId);
            _logger.info(String.format(_logPrefix, "Executing DELETE query with id " + queryId));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
        }
	}
}