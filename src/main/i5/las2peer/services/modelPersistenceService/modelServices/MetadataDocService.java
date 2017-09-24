package i5.las2peer.services.modelPersistenceService.modelServices;

import java.util.Date;
import java.util.ArrayList;
import java.util.UUID;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;
import i5.las2peer.services.modelPersistenceService.model.Model;
import i5.las2peer.services.modelPersistenceService.model.node.Node;
import i5.las2peer.services.modelPersistenceService.model.edge.Edge;
import i5.las2peer.services.modelPersistenceService.model.metadata.MetadataDoc;
import i5.las2peer.services.modelPersistenceService.model.modelAttributes.ModelAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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

    private String modelToSwagger(Model model) {
        String swaggerString = "";
        ObjectMapper mapper = new ObjectMapper();

        // maps for model to http methods, payloads, responses, path
        // simple entry path and object method
        HashMap<String, SimpleEntry<String, SimpleEntry<String, ObjectNode>>> httpMethodNodes = new HashMap<String, SimpleEntry<String, SimpleEntry<String, ObjectNode>>>();
        HashMap<String, ObjectNode> httpPayloadNodes = new HashMap<String, ObjectNode>();
        // simple entry code and response node
        HashMap<String, SimpleEntry<String, ObjectNode>> httpResponseNodes = new HashMap<String, SimpleEntry<String, ObjectNode>>();

        // maps http methods to payload and reponse nodes
        HashMap<String, ArrayList<ObjectNode>> httpMethodParameterNodes = new HashMap<String, ArrayList<ObjectNode>>();
        HashMap<String, ArrayList<SimpleEntry<String, ObjectNode>>> httpMethodResponsesNodes = new HashMap<String, ArrayList<SimpleEntry<String, ObjectNode>>>();
        ObjectNode pathsObject = mapper.createObjectNode();

        // maps http methods to path
        HashMap<String, ArrayList<SimpleEntry<String, ObjectNode>>> pathToHttpMethod = new HashMap<String, ArrayList<SimpleEntry<String, ObjectNode>>>();

        // array nodes for consumes produces
        ArrayList<String> consumes = new ArrayList<String>();
        ArrayList<String> produces = new ArrayList<String>();
        
        // create root, restful resource node, only 1 per microservice model
        ObjectNode rootObject = mapper.createObjectNode();
        
        //get basic attributes for first level
        rootObject.put("swagger", "2.0");

        // info object
        ModelAttributes attributes = model.getAttributes();
        ObjectNode infoObject = mapper.createObjectNode();
        infoObject.put("title", attributes.getName());

        // TODO generate this from widget
        infoObject.put("description", "Generated description");
        infoObject.put("version", "1.0");

        // Fixed value
        infoObject.put("termsOfService", "LICENSE.txt");
        rootObject.put("basePath", "/");
        ArrayNode schemes = mapper.createArrayNode();
        schemes.add("http");
        rootObject.put("schemes", schemes);

        // ==================== PROCESS NODES ======================
        ArrayList<Node> nodes = model.getNodes();
        for(Node node: nodes) {
            switch(node.getType()) {
                // process base restful node
                case "RESTful Resource":
                    // get attributes
                    ArrayList<EntityAttribute> restAttributes = node.getAttributes();
                    for(EntityAttribute attribute: restAttributes) {
                        switch(attribute.getName()) {
                            case "path":
                                rootObject.put("host", attribute.getValue());
                                break;
                            case "developer":
                                ObjectNode contactNode = mapper.createObjectNode();
                                contactNode.put("name", attribute.getValue());
                                infoObject.put("contact", contactNode);
                                break;
                            default:
                                break;
                        };
                    };
                    break;
                case "HTTP Method":
                    httpMethodNodes.put(node.getId(), nodeToHttpMethod(node));
                    break;
                case "HTTP Payload":
                    // parameters
                    httpPayloadNodes.put(node.getId(), nodeToHttpPayload(node));
                    break;
                case "HTTP Response":
                    // produces
                    break;
                default:
                    break;
            };
        };

        // ==================== PROCESS EDGES ======================
        ArrayList<Edge> edges = model.getEdges();
        for(Edge edge: edges) {
            String sourceId = edge.getSourceNode();
            String targetId = edge.getTargetNode();
            switch(edge.getType()) {
                case "RESTful Resource to HTTP Method":
                    // do not process since we're going to put every http method into the root anyway
                    break;
                case "HTTP Method to HTTP Payload":
                    //get the http method & payload object node
                    if (httpMethodNodes.get(sourceId) != null) {
                        // get payload object node
                        ObjectNode httpPayloadNode = httpPayloadNodes.get(targetId);
                        if (httpPayloadNode != null) {
                            // add to parameters list
                            if (httpMethodParameterNodes.get(sourceId) != null) {
                                httpMethodParameterNodes.get(sourceId).add(httpPayloadNode);
                            } else {
                                ArrayList<ObjectNode> payloadList = new ArrayList<ObjectNode>();
                                payloadList.add(httpPayloadNode);
                                httpMethodParameterNodes.put(sourceId, payloadList);
                            }
                            
                        }
                    }
                    break;
                case "HTTP Method to HTTP Response":
                    //get the http method & payload object node
                    if (httpMethodNodes.get(sourceId) != null) {
                        // get payload object node
                        SimpleEntry<String, ObjectNode> httpResponseNode = httpResponseNodes.get(targetId);
                        if (httpResponseNode != null) {
                            // add to payload list
                            if (httpMethodResponsesNodes.get(sourceId) != null) {
                                httpMethodResponsesNodes.get(sourceId).add(httpResponseNode);
                            } else {
                                ArrayList<SimpleEntry<String, ObjectNode>> responseList = new ArrayList<SimpleEntry<String, ObjectNode>>();
                                responseList.add(httpResponseNode);
                                httpMethodResponsesNodes.put(sourceId, responseList);
                            }
                        }
                    }
                    break;
            }

        }

        // add info node
        rootObject.put("info", infoObject);

        // ==================== PROCESS JSON OBJECT HTTP NODES ======================
        for(Map.Entry<String, SimpleEntry<String, SimpleEntry<String, ObjectNode>>> entry: httpMethodNodes.entrySet()) {
            // get the object node
            String methodPath = entry.getKey();
            SimpleEntry<String, SimpleEntry<String, ObjectNode>> methodIdToTypeNode = entry.getValue();
            String methodId = methodIdToTypeNode.getKey();

            SimpleEntry<String, ObjectNode> methodTypeToNode = methodIdToTypeNode.getValue();
            String methodType = methodTypeToNode.getKey();
            ObjectNode methodObjectNode = methodTypeToNode.getValue();

            // get all parameters
            ArrayNode parameters = mapper.createArrayNode();
            ArrayList<ObjectNode> parametersArray = httpMethodParameterNodes.get(methodId);
            for (ObjectNode parameter: parametersArray) {
                parameters.add(parameter);
            }
            methodObjectNode.put("parameters", parameters);

            // get all responses
            ObjectNode responses = mapper.createObjectNode();
            ArrayList<SimpleEntry<String, ObjectNode>> responsesArray = httpMethodResponsesNodes.get(methodId);
            for (SimpleEntry<String, ObjectNode> response: responsesArray) {
                responses.put(response.getKey(), response.getValue());
            }
            methodObjectNode.put("responses", responses);

            // add to path map list
            if (pathToHttpMethod.get(methodPath) != null) {
                pathToHttpMethod.get(methodPath).add(methodTypeToNode);
            } else {
                ArrayList<SimpleEntry<String, ObjectNode>> pathList = new ArrayList<SimpleEntry<String, ObjectNode>>();
                pathList.add(methodTypeToNode);
                pathToHttpMethod.put(methodPath, pathList);
            }            

        }

        // ==================== PROCESS JSON OBJECT PATH NODES ======================
        for(Map.Entry<String, ArrayList<SimpleEntry<String, ObjectNode>>> entry: pathToHttpMethod.entrySet()) {
            String path = entry.getKey();
            ObjectNode pathNode = mapper.createObjectNode();
            ArrayList<SimpleEntry<String, ObjectNode>> methodsArray = entry.getValue();
            for (SimpleEntry<String, ObjectNode> methodTypeAndNode: methodsArray) {
                String methodType = methodTypeAndNode.getKey();
                ObjectNode methodNode = methodTypeAndNode.getValue();
                pathNode.put(methodType, methodNode);
            }
            pathsObject.put(path, pathNode);
        }

        // add path node to root
        rootObject.put("paths", pathsObject);
        
        System.out.println("MODEL TO SWAGGER STRING");
        System.out.println("================================");
        System.out.println(rootObject.toString());

        
        return swaggerString;
    }

    private SimpleEntry<String, SimpleEntry<String, ObjectNode>> nodeToHttpMethod(Node node) {
        ObjectMapper mapper = new ObjectMapper();

        String methodType = "";
        String path = "";
        String operationId = "";
        
        // get attributes
        ArrayList<EntityAttribute> nodeAttributes = node.getAttributes();
        for(EntityAttribute attribute: nodeAttributes) {
            switch (attribute.getName()) {
                case "methodType":
                    methodType = attribute.getValue();
                    break;
                case "name":
                    operationId = attribute.getValue();
                    break;
                case "path":
                    path = attribute.getValue();
                    break;
                default:
                    break;
            }
        }

        ObjectNode methodObject = mapper.createObjectNode();
        methodObject.put("operationId", operationId);

        // create objectnode and store in path key
        SimpleEntry<String, ObjectNode> methodTypeObject = new SimpleEntry<String, ObjectNode>(methodType, methodObject);
        SimpleEntry<String, SimpleEntry<String, ObjectNode>> mapObject = new SimpleEntry<String, SimpleEntry<String, ObjectNode>>(path, methodTypeObject);
        return mapObject;
    }

    private ObjectNode nodeToHttpPayload(Node node) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode nodeObject = mapper.createObjectNode();

        String name = "";
        String type = "";
        
        // get attributes
        ArrayList<EntityAttribute> nodeAttributes = node.getAttributes();
        for(EntityAttribute attribute: nodeAttributes) {
            switch (attribute.getName()) {
                case "name":
                    name = attribute.getValue();
                    break;
                case "payloadType":
                    type = attribute.getValue();
                    break;
                default:
                    break;
            }
        }

        nodeObject.put("name", name);
        nodeObject.put("type", type);
        nodeObject.put("required", true);
        nodeObject.put("description", "Parameter description " + name);
        return nodeObject;
    }

    private String StatusToCode(String status) {
/*HTTP_OK(200), HTTP_CREATED(201), HTTP_BAD_REQUEST(400), HTTP_UNAUTHORIZED(401), HTTP_NOT_FOUND(
        404), HTTP_CONFLICT(409), HTTP_INTERNAL_ERROR(500), HTTP_CUSTOM(-1);*/
        switch (status) {
            case "OK":
              return "200";
            case "CREATED":
              return "201";
            case "BAD_REQUEST":
              return "400";
            case "UNAUTHORIZED":
              return "401";
            case "NOT_FOUND":
              return "404";
            case "CONFLICT":
              return "409";
            case "INTERNAL_ERROR":
              return "500";
            case "CUSTOM":
              return "-1";
            default:
              return "";
          }
    }

    private SimpleEntry<String, ObjectNode> nodeToHttpResponse(Node node) {
        ObjectMapper mapper = new ObjectMapper();

        String code = "";
        String name = "";
        String type = "";
        
        // get attributes
        ArrayList<EntityAttribute> nodeAttributes = node.getAttributes();
        for(EntityAttribute attribute: nodeAttributes) {
            switch (attribute.getName()) {
                case "name":
                    name = attribute.getValue();
                    break;
                case "returnStatusCode":
                    code = StatusToCode(attribute.getValue());
                    break;
                case "resultType":
                    switch (attribute.getValue()) {
                        case "JSON":
                            type = "application/json";
                            break;
                        case "TEXT":
                            type = "string";
                            break;
                        case "CUSTOM":
                            type = "custom";
                            break;
                        default:
                            type = "string";
                            break;
                    }
                    break;
                default:
                    break;
            }
        }

        ObjectNode responseObject = mapper.createObjectNode();
        responseObject.put("type", type);
        responseObject.put("description", "Response description " + name);

        SimpleEntry<String, ObjectNode> mapObject = new SimpleEntry<String, ObjectNode>(code, responseObject);
        return mapObject;
    }
}