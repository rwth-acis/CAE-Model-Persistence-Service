package i5.las2peer.services.modelPersistenceService;

import java.io.Serializable;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.core.JsonProcessingException;

import i5.cae.semanticCheck.SemanticCheckResponse;
import i5.cae.simpleModel.SimpleEntityAttribute;
import i5.cae.simpleModel.SimpleModel;
import i5.cae.simpleModel.node.SimpleNode;
import i5.las2peer.api.Context;
import i5.las2peer.api.ServiceException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.services.modelPersistenceService.database.DatabaseManager;
import i5.las2peer.services.modelPersistenceService.exception.CGSInvocationException;
import i5.las2peer.services.modelPersistenceService.exception.ModelNotFoundException;
import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;
import i5.las2peer.services.modelPersistenceService.model.Model;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

@Path("/")
public class RESTResources {

	private final ModelPersistenceService service = (ModelPersistenceService) Context.getCurrent().getService();
	private L2pLogger logger;
	private String semanticCheckService;
	private String codeGenerationService;
	private DatabaseManager dbm;

	public RESTResources() throws ServiceException {
		this.logger = (L2pLogger) service.getLogger();
		this.semanticCheckService = service.getSemanticCheckService();
		this.codeGenerationService = service.getCodeGenerationService();
		this.dbm = service.getDbm();
	}

	/**
	 * 
	 * Entry point for all new models. Stores it to the database.
	 * 
	 * @param inputModel
	 *            the model as a JSON string
	 * 
	 * @return HttpResponse containing the status code of the request and a
	 *         small return message
	 * 
	 */
	@POST
	@Path("/models/")
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Entry point for storing a model to the database.", notes = "Entry point for storing a model to the database.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "OK, model stored"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Input model was not valid"),
			@ApiResponse(code = HttpURLConnection.HTTP_CONFLICT, message = "Tried to save a model that already had a name and thus was not new"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response postModel(String inputModel) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "postModel: trying to store new model");
		Model model;
		try {
			// create the model
			model = new Model(inputModel);
		} catch (ParseException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "postModel: exception parsing JSON input: " + e);
			throw new BadRequestException(e.getMessage());
			// return Response.serverError().build();
		} catch (Exception e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "postModel: something went seriously wrong: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Internal server error!").build();
		}

		// check if model name is already taken
		if (this.getModel(model.getAttributes().getName()).getStatus() != HttpURLConnection.HTTP_NOT_FOUND) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
					"postModel: model name " + model.getAttributes().getName() + " is already taken");
			return Response.serverError()
					.entity("Model with name " + model.getAttributes().getName() + " already exists!").build();
		}

		// do the semantic check
		if (!semanticCheckService.isEmpty()) {
			this.checkModel(model);
		}

		// call code generation service
		if (!codeGenerationService.isEmpty()) {
			try {
				Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "postModel: invoking code generation service..");
				model = callCodeGenerationService("createFromModel", model);
			} catch (CGSInvocationException e) {
				return Response.serverError().entity("Model not valid: " + e.getMessage()).build();
			}
		}

		// save the model to the database
		Connection connection = null;
		try {
			connection = dbm.getConnection();
			model.persist(connection);
			int modelId = model.getId();
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "postModel: model with id " + modelId + " and name "
					+ model.getAttributes().getName() + " stored!");
			return Response.status(201).entity("Model stored!").build();
		} catch (SQLException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "postModel: exception persisting model: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Could not persist, database rejected model!").build();
		} catch (Exception e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "postModel: something went seriously wrong: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Internal server error...").build();
		}
		// always close connections
		finally {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * 
	 * Searches for a model in the database by name.
	 * 
	 * @param modelName
	 *            the model as a JSON string
	 * 
	 * @return HttpResponse containing the status code of the request and (if
	 *         successful) the model as a JSON string
	 * 
	 */
	@GET
	@Path("/models/{modelName}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Searches for a model in the database. Takes the modelName as search parameter.", notes = "Searches for a model in the database.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model found"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Model could not be found."),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response getModel(@PathParam("modelName") String modelName) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "getModel: searching for model with name " + modelName);
		Model model = null;
		Connection connection = null;
		try {
			connection = dbm.getConnection();
			model = new Model(modelName, connection);
		} catch (ModelNotFoundException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "getModel: did not find model with name " + modelName);
			return Response.status(404).entity("Model not found!").build();
		} catch (SQLException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "getModel: exception fetching model: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Database error!").build();
		} catch (Exception e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "getModel: something went seriously wrong: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Server error!").build();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
				"getModel: found model " + modelName + ", now converting to JSONObject and returning");
		JSONObject jsonModel = model.toJSONObject();

		return Response.ok(jsonModel.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	/**
	 * 
	 * Retrieves all model names from the database.
	 * 
	 * 
	 * @return HttpResponse containing the status code of the request and (if
	 *         the database is not empty) the model-list as a JSON array
	 * 
	 */

	@SuppressWarnings("unchecked")
	@GET
	@Path("/models/")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Retrieves a list of models from the database.", notes = "Retrieves a list of all models stored in the database. Returns a list of model names.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model list is returned"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "No models in the database"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response getModels() {

		ArrayList<String> modelNames = new ArrayList<String>();
		Connection connection = null;
		try {
			connection = dbm.getConnection();
			// search for all models
			PreparedStatement statement = connection.prepareStatement("SELECT modelName FROM ModelAttributes;");
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "getModels: retrieving all models..");
			ResultSet queryResult = statement.executeQuery();
			while (queryResult.next()) {
				modelNames.add(queryResult.getString(1));
			}
			if (modelNames.isEmpty()) {
				Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "getModels: database is empty!");
				return Response.status(404).entity("Database is empty!").build();
			}
			connection.close();
		} catch (SQLException e) {
			L2pLogger.logEvent(Event.SERVICE_ERROR, "getModels: exception fetching model: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Database error!").build();
		} catch (Exception e) {
			L2pLogger.logEvent(Event.SERVICE_ERROR, "getModels: something went seriously wrong: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Server error!").build();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
		L2pLogger.logEvent(Event.SERVICE_MESSAGE,
				"getModels: created list of models, now converting to JSONObject and returning");
		JSONArray jsonModelList = new JSONArray();
		jsonModelList.addAll(modelNames);

		return Response.ok(jsonModelList.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@SuppressWarnings("unchecked")
	@GET
	@Path("/models/type/{modelType}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Retrieves a list of models from the database.", notes = "Retrieves a list of all models stored in the database. Returns a list of model names.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model list is returned"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "No models in the database"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response getModelsByType(@PathParam("modelType") String modelType) {

		ArrayList<String> modelNames = new ArrayList<String>();
		Connection connection = null;
		try {
			connection = dbm.getConnection();
			String sql = "select `ModelAttributes`.`modelName` from `AttributeToModelAttributes`, `Attribute`, `ModelAttributes`\n" +
					"where `AttributeToModelAttributes`.`attributeId` = `Attribute`.`attributeId`\n" +
					"and `AttributeToModelAttributes`.`modelAttributesName` = `ModelAttributes`.`modelName`\n" +
					"and `Attribute`.`name` = 'type'\n" +
					"and `Attribute`.`value` = '" + modelType + "';";
			// search for all models
			PreparedStatement statement = connection.prepareStatement(sql);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "getModels: retrieving all models..!");

			ResultSet queryResult = statement.executeQuery();
			while (queryResult.next()) {
				modelNames.add(queryResult.getString(1));
			}
			if (modelNames.isEmpty()) {
				Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "getModels: retrieving all models..");
				return Response.ok(new JSONArray().toJSONString(), MediaType.APPLICATION_JSON).build();
			}
			connection.close();
		} catch (SQLException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "getModels: exception fetching model: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Database error!").build();
		} catch (Exception e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "getModels: something went seriously wrong: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Server error!").build();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
				"getModels: created list of models, now converting to JSONObject and returning");
		JSONArray jsonModelList = new JSONArray();
		jsonModelList.addAll(modelNames);

		return Response.ok(jsonModelList.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	/**
	 * 
	 * Deletes a model.
	 *
	 * @param modelName
	 *            a string containing the model name
	 * 
	 * @return HttpResponse containing the status code of the request
	 * 
	 */
	@DELETE
	@Path("/models/{modelName}")
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Deletes a model given by its name.", notes = "Deletes a model given by its name.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model is deleted"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Model does not exist"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response deleteModel(@PathParam("modelName") String modelName) {
		Connection connection = null;
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deleteModel: trying to delete model with name: " + modelName);
		try {
			connection = dbm.getConnection();
			Model model = new Model(modelName, connection);

			// call code generation service
			if (!codeGenerationService.isEmpty()) {
				try {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deleteModel: invoking code generation service..");
					model = callCodeGenerationService("deleteRepositoryOfModel", model);
				} catch (CGSInvocationException e) {
					return Response.serverError().entity("Model not valid: " + e.getMessage()).build();
				}
			}

			model.deleteFromDatabase(connection);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deleteModel: deleted model " + modelName);
			return Response.ok("Model deleted!").build();
		} catch (ModelNotFoundException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deleteModel: did not find model with name " + modelName);
			return Response.status(404).entity("Model not found!").build();
		} catch (SQLException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "deleteModel: exception deleting model: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Internal server error...").build();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * 
	 * Updates a model. Basically only deletes the old one and creates a new
	 * one, but if the CAE Code Generation Service is used, it validates the
	 * model and prevents updating if it would break the semantics.
	 * 
	 * @param modelName
	 *            a string containing the model name
	 * @param inputModel
	 *            the model as a JSON string
	 * 
	 * @return HttpResponse containing the status code of the request
	 * 
	 */
	@PUT
	@Path("/models/{modelName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Updates a model.", notes = "Updates a model.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model is updated"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Model does not exist"),
			@ApiResponse(code = HttpURLConnection.HTTP_CONFLICT, message = "Model name may not be changed"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response updateModel(@PathParam("modelName") String modelName, String inputModel) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "updateModel: trying to update model with name: " + modelName);
		Model model;
		// first parse the updated model and check for correctness of format
		try {
			model = new Model(inputModel);
			// the model name is its "id", it may not be changed
			if (!model.getAttributes().getName().equals(modelName)) {
				Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "updateModel: posted model name " + modelName
						+ " is different from posted model name attribute " + model.getAttributes().getName());
				return Response.status(409).entity("Model name is different!").build();
			}
		} catch (ParseException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "updateModel: exception parsing JSON input: " + e);
			return Response.serverError().entity("JSON parsing exception, file not valid!").build();
		} catch (Exception e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "updateModel: something went seriously wrong: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Internal server error!").build();
		}

		// do the semantic story check
		if (!semanticCheckService.isEmpty()) {
			this.checkModel(model);
		}

		// call code generation service
		if (!codeGenerationService.isEmpty()) {
			try {
				Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "updateModel: invoking code generation service..");
				model = callCodeGenerationService("updateRepositoryOfModel", model);
			} catch (CGSInvocationException e) {
				return Response.serverError().entity("Model not valid: " + e.getMessage()).build();
			}
		} else {
			return Response.serverError().entity("CodeGeneration Service not specified").build();
		}

		// if this has thrown no exception, we can delete the "old" model
		// and persist the new one
		Connection connection = null;
		try {
			connection = dbm.getConnection();
			// load and delete the old model from the database
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
					"updateModel: loading and deleting old model with name " + modelName);
			new Model(modelName, connection).deleteFromDatabase(connection);
			// check if the "old" model did exist
		} catch (ModelNotFoundException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "updateModel: there exists no model with name: " + modelName);
			return Response.status(404).entity("Model not found!").build();
		} catch (SQLException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "updateModel: error deleting old model: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Internal server error...").build();
		}
		// always close connections
		finally {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
		try {
			connection = dbm.getConnection();
			// save the model to the database
			model.persist(connection);
			int modelId = model.getId();
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "updateModel: model with new id " + modelId + " and name "
					+ model.getAttributes().getName() + " stored!");
			return Response.ok("Model updated!").build();
		} catch (SQLException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "updateModel: exception persisting model: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Could not persist, database rejected model!").build();
		} catch (Exception e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "updateModel: something went seriously wrong: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Internal server error...").build();
		}
		// always close connections
		finally {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	/**
	 * Get the status / console text of a build. The build is identified by
	 * using the queue item that is returned when a job is created.
	 * 
	 * @param queueItem
	 *            The queue item of the job
	 * @return The console text of the job
	 */

	@GET
	@Path("/deployStatus/")
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Get the console text of the build from Jenkins", notes = "Get the console text of the build.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model will be deployed"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Model does not exist"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response deployStatus(@QueryParam("queueItem") String queueItem) {

		// delegate the request to the code generation service as it is the
		// service responsible for
		// Jenkins

		try {
			String answer = (String) Context.getCurrent().invoke(
					"i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1", "deployStatus", queueItem);
			return Response.ok(answer).build();
		} catch (Exception e) {
			logger.printStackTrace(e);
			return Response.serverError().entity(e.getMessage()).build();
		}

	}

	/**
	 * 
	 * Requests the code generation service to start a Jenkins job for an
	 * application model.
	 * 
	 * @param modelName
	 *            a string containing the model name
	 * @param jobAlias
	 *            the name/alias of the job to run, i.e. either "Build" or
	 *            "Docker"
	 * 
	 * @return HttpResponse containing the status code of the request
	 * 
	 */
	@GET
	@Path("/deploy/{modelName}/{jobAlias}")
	@Consumes(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Deploys an application model.", notes = "Deploys an application model.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model will be deployed"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Model does not exist"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response deployModel(@PathParam("modelName") String modelName, @PathParam("jobAlias") String jobAlias) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deployModel: trying to deploy model with name: " + modelName);
		Model model;
		Connection connection = null;

		// first parse the updated model and check for correctness of format
		try {
			connection = dbm.getConnection();
			model = new Model(modelName, connection);

			// the model name is its "id", it may not be changed
			if (!model.getAttributes().getName().equals(modelName)) {
				Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deployModel: posted model name " + modelName
						+ " is different from posted model name attribute " + model.getAttributes().getName());
				return Response.status(409).entity("Model name is different!").build();
			}
			try {

				// only create temp repository once, i.e. before the "Build"
				// job is started in Jenkins
				if (jobAlias.equals("Build")) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "deployModel: invoking code generation service..");
					callCodeGenerationService("prepareDeploymentApplicationModel", model);
				}

				// start the jenkins job by the code generation service
				String answer = (String) Context.getCurrent().invoke(
						"i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1", "startJenkinsJob",
						jobAlias);

				return Response.ok(answer).build();
			} catch (CGSInvocationException e) {
				return Response.serverError().entity("Model not valid: " + e.getMessage()).build();
			}
		} catch (Exception e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "updateModel: something went seriously wrong: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Internal server error!").build();
		} // always close connections
		finally {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// Methods special to the CAE. Feel free to ignore them:-)
	///////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * Loads a model from the database (by calling the respective resource) and
	 * sends it to the code generation service, requesting a Communication Model
	 * view to be displayed in SyncMeta's application editor view.
	 * 
	 * TODO: Not tested..
	 * 
	 * @param modelName
	 *            the name of the model to be loaded.
	 * 
	 * @return HttpResponse containing the status code of the request and the
	 *         communication view model as a JSON string
	 */
	@GET
	@Path("/models/commView/{modelName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Gets a CAE communication view model.", notes = "Gets a CAE communication view model.")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "OK, model found"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Model does not exist"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response getCAECommunicationModel(@PathParam("modelName") String modelName) {
		// load the application model from the database
		SimpleModel appModel;
		Connection connection = null;
		try {
			connection = dbm.getConnection();
			Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
					"getCAECommunicationModel: Loading model " + modelName + " from the database");
			appModel = (SimpleModel) new Model(modelName, connection).getMinifiedRepresentation();
		} catch (SQLException e) {
			// model might not exist
			logger.printStackTrace(e);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "getCAECommunicationModel: model " + modelName + " not found");
			return Response.status(404).entity("Model " + modelName + " does not exist!").build();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				logger.printStackTrace(e);
			}
		}
		// load submodules of application model from the database
		Serializable[] modelsToSend = null;
		for (SimpleEntityAttribute attribute : appModel.getAttributes()) {
			if (attribute.getName().equals("type") && attribute.getValue().equals("application")) {
				modelsToSend = new SimpleModel[appModel.getNodes().size() + 1];
				modelsToSend[0] = appModel; // first is always "application"
											// model itself
				int modelsToSendIndex = 1;
				// iterate through the nodes and add corresponding models to
				// array
				for (SimpleNode node : appModel.getNodes()) {
					// send application models only have one attribute with
					// its label
					String subModelName = node.getAttributes().get(0).getValue();
					try {
						connection = dbm.getConnection();
						modelsToSend[modelsToSendIndex] = new Model(subModelName, connection)
								.getMinifiedRepresentation();
					} catch (SQLException e) {
						// model might not exist
						logger.printStackTrace(e);
						Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR,
								"getCAECommunicationModel: Error loading application component: " + subModelName);
						return Response.serverError().entity("Internal server error...").build();
					} finally {
						try {
							connection.close();
						} catch (SQLException e) {
							logger.printStackTrace(e);
						}
					}
					modelsToSendIndex++;
				}
				// invoke code generation service
				try {
					Serializable[] payload = { modelsToSend };

					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"getCAECommunicationModel: Invoking code generation service now..");
					SimpleModel communicationModel = (SimpleModel) Context.getCurrent().invoke(codeGenerationService,
							"getCommunicationViewOfApplicationModel", payload);

					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE,
							"getCAECommunicationModel: Got communication model from code generation service..");

					Model returnModel = new Model(communicationModel);
					Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "getCAECommunicationModel: Created model " + modelName
							+ "from simple model, now converting to JSONObject and returning");

					JSONObject jsonModel = returnModel.toJSONObject();
					return Response.ok(jsonModel.toJSONString()).build();
				} catch (Exception e) {
					Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR,
							"getCAECommunicationModel: Internal error " + e.getMessage());
					logger.printStackTrace(e);
					return Response.serverError().entity("Internal server error...").build();
				}
			}
		}
		Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR,
				"getCAECommunicationModel: model " + modelName + " is not an application");
		return Response.serverError().entity("Internal server error...").build();
	}

	/**
	 * 
	 * Calls the code generation service to see if the model is a valid CAE
	 * model. Also implements a bit of CAE logic by checking if the code
	 * generation service needs additional models (in case of an application
	 * model) and serves them automatically, such that the rest of this service
	 * does not have to deal with this "special case".
	 * 
	 * @param methodName
	 *            the method name of the code generation service
	 * @param model
	 *            {@link Model}
	 * @return the model
	 * 
	 * @throws CGSInvocationException
	 *             if something went wrong invoking the service
	 * 
	 */
	private Model callCodeGenerationService(String methodName, Model model) throws CGSInvocationException {
		Connection connection = null;
		Serializable[] modelsToSend = null;
		SimpleModel simpleModel = (SimpleModel) model.getMinifiedRepresentation();
		boolean isApplication = false;

		for (SimpleEntityAttribute attribute : simpleModel.getAttributes()) {
			if (attribute.getName().equals("type")) {
				// handle special case of application model
				if (attribute.getValue().equals("application")) {
					isApplication = true;
					break;
				}
			}
		}

		if (isApplication) {
			modelsToSend = new SimpleModel[simpleModel.getNodes().size() + 1];
			modelsToSend[0] = simpleModel; // first is always "application"
											// model itself
			int modelsToSendIndex = 1;
			// iterate through the nodes and add corresponding models to
			// array
			for (SimpleNode node : simpleModel.getNodes()) {
				// since application models only have one attribute with its
				// label
				String modelName = node.getAttributes().get(0).getValue();
				logger.info("Attributes: " + node.getAttributes().toString());

				try {
					connection = dbm.getConnection();
					logger.info("Modelname: " + modelName);
					modelsToSend[modelsToSendIndex] = new Model(modelName, connection).getMinifiedRepresentation();
				} catch (SQLException e) {
					// model might not exist
					logger.printStackTrace(e);
					throw new CGSInvocationException("Error loading application component: " + modelName);
				} finally {
					try {
						connection.close();
					} catch (SQLException e) {
						logger.printStackTrace(e);
					}
				}
				modelsToSendIndex++;
			}
		} else {
			SimpleModel oldModel = null;
			String modelName = model.getAttributes().getName();
			try {
				connection = dbm.getConnection();
				oldModel = (SimpleModel) new Model(modelName, connection).getMinifiedRepresentation();
			} catch (SQLException e) {
				// we can ignore sql exception for the loading of the old
				// model. If such an exception is
				// thrown, we assume that
				// there is no old model
			} catch (Exception e) {
				// catch all other exceptions to ensure that the loading of
				// the old model does not influence
				// the call of the code generation service
				logger.printStackTrace(e);
			} finally {
				try {
					connection.close();
				} catch (SQLException e) {
					logger.printStackTrace(e);
				}
			}
			if (oldModel != null) {
				modelsToSend = new SimpleModel[2];
				modelsToSend[0] = simpleModel;
				modelsToSend[1] = oldModel;
			} else {
				modelsToSend = new SimpleModel[1];
				modelsToSend[0] = simpleModel;
			}
		}


		// actual invocation
		try {
			Serializable[] payload = { modelsToSend};
			String answer = (String) Context.getCurrent().invoke(codeGenerationService, methodName, payload);
			if (!answer.equals("done")) {
				throw new CGSInvocationException(answer);
			}
			return model;
		} catch (Exception e) {
			logger.printStackTrace(e);
			throw new CGSInvocationException(e.getMessage());
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////
	// Methods for Semantic Check
	////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * Performs the semantic check (if specified) on the model, without storing
	 * it
	 * 
	 * @param inputModel
	 *            the model as a JSON string
	 * 
	 * @return HttpResponse status of the check
	 * 
	 */
	@PUT
	@Path("/semantics")
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Performs the semantic check", notes = "Performs the semantic check")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_MODIFIED, message = "Semantic Check successful"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal server error") })
	public Response checkModel(String inputModel) {
		Context.get().monitorEvent(MonitoringEvent.SERVICE_MESSAGE, "checkModel: performing semantic check");
		Model model;
		// first parse the updated model and check for correctness of format
		try {
			model = new Model(inputModel);
		} catch (ParseException e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "semantic check: exception parsing JSON input: " + e);
			return Response.serverError().entity("JSON parsing exception, file not valid!").build();
		} catch (Exception e) {
			Context.get().monitorEvent(MonitoringEvent.SERVICE_ERROR, "semantic check: something went seriously wrong: " + e);
			logger.printStackTrace(e);
			return Response.serverError().entity("Internal server error!").build();
		}

		// do the semantic story check
		if (!semanticCheckService.isEmpty()) {
			this.doSemanticCheck(model);
		} else {
			return Response.status(400).entity("No semantic check service available").build();
		}
		return Response.ok(SemanticCheckResponse.success().toJSONResultString()).build();
	}

	private void checkModel(Model model) {
		SemanticCheckResponse result;
		EntityAttribute semcheckAttr = findSemcheckAttribute(model);
		try {
			result = (SemanticCheckResponse) Context.getCurrent().invoke(semanticCheckService, "doSemanticCheck",
					model.getMinifiedRepresentation());
		} catch (Exception e) {
			System.out.println(e);
			throw new InternalServerErrorException("could not execute semantic check service");
		}
		if (result == null) {
			throw new InternalServerErrorException("an error orrcured within the semantic check");
		} else if (result.getError() != 0) {
			if (semcheckAttr == null) {
				throw new BadRequestException(result.toJSONResultString());
			} else if (!semcheckAttr.getValue().equals("false")) {
				throw new BadRequestException("This model was supposed to be incorrect");
			}
		} else if (result.getError() == 0) {
			if (semcheckAttr != null && !semcheckAttr.getValue().equals("true")) {
				throw new BadRequestException("This model was supposed to be correct");
			}
		}
	}

	private void doSemanticCheck(Model model) {
		SemanticCheckResponse result;
		try {
			result = (SemanticCheckResponse) Context.getCurrent().invoke(semanticCheckService, "doSemanticCheck",
					model.getMinifiedRepresentation());
		} catch (Exception e) {
			System.out.println(e);
			throw new InternalServerErrorException("could not execute semantic check service");
		}
		if (result == null) {
			throw new InternalServerErrorException("an error orrcured within the semantic check");
		} else if (result.getError() != 0) {
			throw new InternalServerErrorException(Response.ok(result.toJSONResultString()).build());
		}
	}

	private EntityAttribute findSemcheckAttribute(Model model) {
		EntityAttribute res = null;
		for (EntityAttribute a : model.getAttributes().getAttributes()) {
			if (a.getName().equals("_semcheck")) {
				return a;
			}
		}
		return res;
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// Methods providing a Swagger documentation of the service API.
	////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * Returns the API documentation for a specific annotated top level resource
	 * for purposes of the Swagger documentation.
	 * 
	 * Note: If you do not intend to use Swagger for the documentation of your
	 * Service API, this method may be removed.
	 * 
	 * Trouble shooting: Please make sure that the endpoint URL below is correct
	 * with respect to your service.
	 * 
	 * @return the resource's documentation
	 * 
	 */
	@GET
	@Path("/models/swagger.json")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSwaggerJSON() {
		Swagger swagger = new Reader(new Swagger()).read(this.getClass());
		if (swagger == null) {
			return Response.status(404).entity("Swagger API declaration not available!").build();
		}
		try {
			return Response.ok(Json.mapper().writeValueAsString(swagger), MediaType.APPLICATION_JSON).build();
		} catch (JsonProcessingException e) {
			logger.printStackTrace(e);
			return Response.serverError().entity(e.getMessage()).build();
		}
	}
}
