package i5.las2peer.services.modelPersistenceService.modelServices;

import java.util.Date;
import java.util.ArrayList;
import java.util.UUID;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;
import i5.las2peer.services.modelPersistenceService.model.Model;
import i5.las2peer.services.modelPersistenceService.model.node.Node;
import i5.las2peer.services.modelPersistenceService.model.edge.Edge;
import i5.las2peer.services.model.metadata.MetadataDoc;
import i5.las2peer.services.modelPersistenceService.model.modelAttributes.ModelAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
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
            String componentId = queryResult.getString("componentId");
            String docType = queryResult.getString("docType");
            String docString = queryResult.getString("docString");
            String docInput = queryResult.getString("docInput");
            Date timeCreated = queryResult.getDate("timeCreated");
            Date timeEdited = queryResult.getDate("timeEdited");
            MetadataDoc model = new MetadataDoc(componentId, docType, docString, docInput, timeCreated, timeEdited);
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
     * Get user inputted metadata doc string by component id
     * @param queryId id of metadata doc
     * @return founded metadata doc string
     */
    public String getUserInputMetadataDocStringByComponentId(String queryId) {
        try {
            return getByComponentId(queryId).getDocInput();
        } catch (SQLException e) {
            return "";
        }
    }

    /****** CREATE UPDATE MODEL GENERATED METADATA DOC */
    public void createUpdateModelGeneratedMetadata(String componentId, String modelGenerateMetadata, String docType) throws SQLException {
        try {
            PreparedStatement sqlQuery = _connection.prepareStatement(
                    " INSERT INTO MetadataDoc(componentId, docString, docType) VALUES (?,?,?) " + 
                    " ON DUPLICATE KEY UPDATE docString=?, docType=?");
            sqlQuery.setString(1, componentId);
            sqlQuery.setString(2, modelGenerateMetadata);
            sqlQuery.setString(3, docType);
            sqlQuery.setString(4, modelGenerateMetadata);
            sqlQuery.setString(5, docType);
            _logger.info(String.format(_logPrefix, "Executing model generated metadata CREATE UPDATE query"));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
            throw e;
        }
    }

    /****** CREATE UPDATE MODEL GENERATED METADATA DOC */
    public void createUpdateUserGeneratedMetadata(MetadataDoc inputModel) throws SQLException {
        try {
            String componentId = inputModel.getComponentId(); 
            String userGenerateMetadata = inputModel.getDocInput();
            String docType = inputModel.getDocType();
            PreparedStatement sqlQuery = _connection.prepareStatement(
                    " INSERT INTO MetadataDoc(componentId, docInput, docType) VALUES (?,?,?) " + 
                    " ON DUPLICATE KEY UPDATE docInput=?, docType=?");
            sqlQuery.setString(1, componentId);
            sqlQuery.setString(2, userGenerateMetadata);
            sqlQuery.setString(3, docType);
            sqlQuery.setString(4, userGenerateMetadata);
            sqlQuery.setString(5, docType);
            _logger.info(String.format(_logPrefix, "Executing user generated metadata CREATE UPDATE query"));
            sqlQuery.executeUpdate();
            sqlQuery.close();
        } catch (SQLException e) {
            _logger.printStackTrace(e);
            throw e;
        }
    }

    /****** GENERIC CREATE UPDATE METADATA DOC */

    /**
	 * Insert new metadata doc
	 * @param insertModel model to insert
	 */
    public void create(MetadataDoc insertModel) throws SQLException {
        try {
            PreparedStatement sqlQuery = _connection.prepareStatement(
				"INSERT INTO MetadataDoc(componentId, docString, docType) VALUES (?,?,?);");
            sqlQuery.setString(1, insertModel.getComponentId());
            sqlQuery.setString(2, insertModel.getDocString());
            sqlQuery.setString(3, insertModel.getDocType());
            _logger.info(String.format(_logPrefix, "Executing generic CREATE query"));
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
				"UPDATE MetadataDoc SET docString=?, docType=? WHERE componentId=?;");
            sqlQuery.setString(3, updateModel.getComponentId());
            sqlQuery.setString(1, updateModel.getDocString());
            sqlQuery.setString(2, updateModel.getDocType());
            _logger.info(String.format(_logPrefix, "Executing UPDATE query for component id " + updateModel.getComponentId()));
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

    /**
     * Convert model object to swagger json object
     * @param model CAE model, assumed valid
     */
    public String modelToSwagger(Model model) {
        System.out.println("========START MODEL TO SWAGGER==========");
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

        // get user input metadata doc if exists
        String modelName = model.getAttributes().getName();
        System.out.println("===PROCESS USER INPUT METADATA DOC for " + modelName);
        String userInputMetadataDoc = getUserInputMetadataDocStringByComponentId(modelName);
        System.out.println(userInputMetadataDoc);
        
        String description = "No description";
        String version = "1.0";
        String termsOfService = "LICENSE.txt";
        
        if (!Strings.isNullOrEmpty(userInputMetadataDoc)) {
            try {
                JsonNode metadataTree = mapper.readTree(userInputMetadataDoc);
                System.out.println("===Parsed json ");
                System.out.println(metadataTree);
                // get info node
                if (metadataTree.hasNonNull("info")) {
                    JsonNode infoNode = metadataTree.get("info");
                    description = infoNode.get("description").asText();
                    version = infoNode.get("version").asText();
                    termsOfService = infoNode.get("termsOfService").asText();
                }
            } catch (IOException ex) {
                System.out.println("Exception on parsing user input metadata doc");
            }
        }

        //get basic attributes for first level
        rootObject.put("swagger", "2.0");

        // info object
        ModelAttributes attributes = model.getAttributes();
        ObjectNode infoObject = mapper.createObjectNode();
        infoObject.put("title", attributes.getName());

        // generated from widget
        infoObject.put("description", description);
        infoObject.put("version", version);
        infoObject.put("termsOfService", termsOfService);

        // Fixed value
        rootObject.put("basePath", "/");
        ArrayNode schemes = mapper.createArrayNode();
        schemes.add("http");
        rootObject.put("schemes", schemes);

        try {
            // ==================== PROCESS NODES ======================
            System.out.println("=======[Model to Swagger] Process Nodes ==========");
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
                        httpResponseNodes.put(node.getId(), nodeToHttpResponse(node));
                        break;
                    default:
                        break;
                };
            };
        } catch(Exception e) {
            System.out.println("[Model to Swagger] Exception on process nodes");
            System.out.println(e);
            return rootObject.toString();
        }

        try {
            // ==================== PROCESS EDGES ======================
            System.out.println("=======[Model to Swagger] Process Edges ==========");
            System.out.println("httpPayloadNodes " + httpResponseNodes.size());
            System.out.println("httpResponseNodes " + httpResponseNodes.size());

            ArrayList<Edge> edges = model.getEdges();
            for(Edge edge: edges) {
                String sourceId = edge.getSourceNode();
                String targetId = edge.getTargetNode();
                switch(edge.getType()) {
                    case "RESTful Resource to HTTP Method":
                        // do not process since we're going to put every http method into the root anyway
                        break;
                    case "HTTP Method to HTTP Payload":
                        System.out.println("HTTP Method to HTTP Payload source " + sourceId + " target " + targetId);
                        //get the http method & payload object node
                        if (httpMethodNodes.get(sourceId) != null) {
                            System.out.println("HTTP Node found " + sourceId);
                            // get payload object node
                            ObjectNode httpPayloadNode = httpPayloadNodes.get(targetId);
                            if (httpPayloadNode != null) {
                                // add to parameters list
                                System.out.println("Add to parameters list " + sourceId + " value " + targetId);
                                if (httpMethodParameterNodes.get(sourceId) != null) {
                                    System.out.println("Parameters list with key not null " + targetId);
                                    httpMethodParameterNodes.get(sourceId).add(httpPayloadNode);
                                } else {
                                    System.out.println("Parameters list with key null, create " + targetId);
                                    ArrayList<ObjectNode> payloadList = new ArrayList<ObjectNode>();
                                    payloadList.add(httpPayloadNode);
                                    System.out.println(payloadList.size());
                                    System.out.println(payloadList);
                                    httpMethodParameterNodes.put(sourceId, payloadList);
                                }
                                
                            }
                        }
                        break;
                    case "HTTP Method to HTTP Response":
                        System.out.println("HTTP Method to HTTP Response source " + sourceId + " target " + targetId);
                        //get the http method & payload object node
                        if (httpMethodNodes.get(sourceId) != null) {
                            // get payload object node
                            System.out.println("HTTP Node found " + sourceId);
                            SimpleEntry<String, ObjectNode> httpResponseNode = httpResponseNodes.get(targetId);
                            if (httpResponseNode != null) {
                                System.out.println("Add to responses list " + sourceId + " value " + targetId);
                                // add to payload list
                                if (httpMethodResponsesNodes.get(sourceId) != null) {
                                    System.out.println("Responses list with key not null " + targetId);
                                    httpMethodResponsesNodes.get(sourceId).add(httpResponseNode);
                                } else {
                                    System.out.println("Response list with key null, create " + targetId);
                                    ArrayList<SimpleEntry<String, ObjectNode>> responseList = new ArrayList<SimpleEntry<String, ObjectNode>>();
                                    responseList.add(httpResponseNode);
                                    System.out.println(responseList.size());
                                    System.out.println(responseList);
                                    httpMethodResponsesNodes.put(sourceId, responseList);
                                }
                            }
                        }
                        break;
                }

            }

        } catch(Exception e) {
            System.out.println("[Model to Swagger] Exception on process edges");
            System.out.println(e);
            return rootObject.toString();
        }

        // add info node
        rootObject.put("info", infoObject);

        try {
            // ==================== PROCESS JSON OBJECT HTTP NODES ======================
            System.out.println("httpMethodNodes count " + httpMethodNodes.size());
            System.out.println("httpMethodParameterNodes count " + httpMethodParameterNodes.size());
            System.out.println("httpMethodResponsesNodes count " + httpMethodResponsesNodes.size());
            
            System.out.println("=======[Model to Swagger] Process HTTP Nodes to Json ==========");
            for(Map.Entry<String, SimpleEntry<String, SimpleEntry<String, ObjectNode>>> entry: httpMethodNodes.entrySet()) {
                // get the object node
                String methodId = entry.getKey();
                SimpleEntry<String, SimpleEntry<String, ObjectNode>> methodPathToTypeNode = entry.getValue();
                String methodPath = methodPathToTypeNode.getKey();

                SimpleEntry<String, ObjectNode> methodTypeToNode = methodPathToTypeNode.getValue();
                String methodType = methodTypeToNode.getKey();
                ObjectNode methodObjectNode = methodTypeToNode.getValue();

                System.out.println("Method id " + methodId);
                System.out.println("Method path " + methodPath);
                System.out.println("Method type " + methodType);

                // get all parameters
                ArrayNode parameters = mapper.createArrayNode();
                System.out.println("Array node created");
                System.out.println(httpMethodParameterNodes.get(methodId));
                for(Map.Entry<String, ArrayList<ObjectNode>> entryLog: httpMethodParameterNodes.entrySet()) {
                    System.out.println(entryLog.getKey());
                }
                ArrayList<ObjectNode> parametersArray = httpMethodParameterNodes.get(methodId);
                System.out.println("parametersArray count " + parametersArray.size());

                for (ObjectNode parameter: parametersArray) {
                    System.out.println("Add to array node parameters " + methodId);
                    parameters.add(parameter);
                }
                methodObjectNode.put("parameters", parameters);

                // get all responses
                ObjectNode responses = mapper.createObjectNode();
                ArrayList<SimpleEntry<String, ObjectNode>> responsesArray = httpMethodResponsesNodes.get(methodId);
                System.out.println("responsesArray count " + responsesArray.size());
                
                for (SimpleEntry<String, ObjectNode> response: responsesArray) {
                    System.out.println("Put to object node responses " + methodId);
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
        } catch(Exception e) {
            System.out.println("[Model to Swagger] Exception on json object http nodes");
            System.out.println(e);
            return rootObject.toString();
        }

        try {
            // ==================== PROCESS JSON OBJECT PATH NODES ======================
            System.out.println("=======[Model to Swagger] Process Path Nodes to JSON ==========");
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
        } catch(Exception e) {
            System.out.println("[Model to Swagger] Exception on json object path nodes");
            System.out.println(e);
            return rootObject.toString();
        }

        // add path node to root
        rootObject.put("paths", pathsObject);
        
        System.out.println("MODEL TO SWAGGER STRING");
        System.out.println("================================");
        System.out.println(rootObject.toString());

        // save result to database
        try {
            createUpdateModelGeneratedMetadata(modelName, rootObject.toString(), "json");
        } catch (SQLException e) {
            System.out.println(e);
        }
        
        return rootObject.toString();
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

        if(Strings.isNullOrEmpty(path)) {
            path = "/";
        }

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