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
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.modelPersistenceService.database.DatabaseManager;
import i5.las2peer.services.modelPersistenceService.exception.CGSInvocationException;
import i5.las2peer.services.modelPersistenceService.exception.ModelNotFoundException;
import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;
import i5.las2peer.services.modelPersistenceService.model.Model;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

/**
 * 
 * CAE Model Persistence Service
 * 
 * A LAS2peer service used for persisting (and validating) application models.
 * Part of the CAE.
 * 
 */
@Api
@SwaggerDefinition(info = @Info(title = "CAE Model Persistence Service", version = "0.1", description = "A LAS2peer service used for persisting (and validating) application models. Part of the CAE.", termsOfService = "none", contact = @Contact(name = "Peter de Lange", url = "https://github.com/PedeLa/", email = "lange@dbis.rwth-aachen.de"), license = @License(name = "BSD", url = "https://github.com/PedeLa/CAE-Model-Persistence-Service//blob/master/LICENSE.txt")))
@ServicePath("CAE")
public class ModelPersistenceService extends RESTService {

	/*
	 * Database configuration
	 */
	private String jdbcDriverClassName;
	private String jdbcLogin;
	private String jdbcPass;
	private String jdbcUrl;
	private String jdbcSchema;
	private String semanticCheckService = "";
	private String codeGenerationService = "";
	private DatabaseManager dbm;
	
	public enum MobSOSEvent {
		APPLICATION_CREATED,
		MICROSERVICE_CREATED,
		FRONTEND_CREATED
	}

	/*
	 * Global variables
	 */
	private final static L2pLogger logger = L2pLogger.getInstance(ModelPersistenceService.class.getName());

	public ModelPersistenceService() {
		// read and set properties values
		setFieldValues();
		// instantiate a database manager to handle database connection pooling
		// and credentials
		dbm = new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass, jdbcUrl, jdbcSchema);
	}

	@Override
	protected void initResources() {
		getResourceConfig().register(RESTResources.class);
	}
	
	public L2pLogger getLogger() {
		return logger;
	}
	
	public String getSemanticCheckService() {
		return semanticCheckService;
	}
	
	public String getCodeGenerationService() {
		return codeGenerationService;
	}
	
	public DatabaseManager getDbm(){
		return dbm;
	}
	
	public void logMobSOSEvent(MobSOSEvent event) {
		switch(event) {
			case APPLICATION_CREATED:
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_1, Context.getCurrent().getMainAgent(), "Application created");
				break;
			case MICROSERVICE_CREATED:
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_2, Context.getCurrent().getMainAgent(), "Microservice created");
				break;
			case FRONTEND_CREATED:
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_3, Context.getCurrent().getMainAgent(), "Frontend created");
				break;
		}
	}
	
}
