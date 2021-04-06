package i5.las2peer.services.modelPersistenceService.projectMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import i5.las2peer.services.modelPersistenceService.exception.ReqBazException;

public class ReqBazHelper {

	private static ReqBazHelper instance;
	
	// make sure that constructor cannot be accessed from outside
    private ReqBazHelper() {}
		
	public static ReqBazHelper getInstance() {
		if(ReqBazHelper.instance == null) {
			ReqBazHelper.instance = new ReqBazHelper();
		}
		return ReqBazHelper.instance;
	}
	
	/**
	 * Requirements Bazaar configuration.
	 * This can be updated in the properties file of the service.
	 */
	private String reqBazBackendUrl = null;
	private int reqBazProjectId = -1;
	
	public void setReqBazBackendUrl(String reqBazBackendUrl) {
		this.reqBazBackendUrl = reqBazBackendUrl;
	}
	
	public void setReqBazProjectId(int reqBazProjectId) {
		this.reqBazProjectId = reqBazProjectId;
	}
	
	/**
	 * Creates a new category in the Requirements Bazaar.
	 * @param categoryName Name of the category that should be created.
	 * @param accessToken The OIDC access token which should be used to create the Requirements Bazaar category.
	 * @return ReqBazCategory object.
	 * @throws ReqBazException If something with the API request went wrong.
	 */
	public ReqBazCategory createCategory(String categoryName, String accessToken) throws ReqBazException {
		if(reqBazBackendUrl == null || reqBazProjectId == -1) {
			throw new ReqBazException("One of the variables reqBazBackendUrl or reqBazProjectId are not set.");
		}
		
		String body = getCategoryBody(categoryName);
		
		// this is the access token from the user that wants to create the project
		String oidcToken = accessToken;
		
		URL url;
		try {
			url = new URL(this.reqBazBackendUrl + "/categories");

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Length", String.valueOf(body.length()));
			connection.setRequestProperty("Authorization", "Bearer " + oidcToken);
			
            writeRequestBody(connection, body);
			
			// forward (in case of) error
			if (connection.getResponseCode() != 201) {
				String message = getErrorMessage(connection);
				throw new ReqBazException(message);
			} else {
				// get response
				String response = getResponseBody(connection);
				
				// convert to JSONObject
				JSONObject json = (JSONObject) JSONValue.parseWithException(response);
				int categoryId = ((Long) json.get("id")).intValue();
				return new ReqBazCategory(categoryId, this.reqBazProjectId);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new ReqBazException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new ReqBazException(e.getMessage());
		} catch (ParseException e) {
			e.printStackTrace();
			throw new ReqBazException(e.getMessage());
		}
	}
	
	/**
	 * Deletes the category in the Requirements Bazaar.
	 * @param category ReqBazCategory which should be deleted.
	 * @param accessToken Access Token of the user, needed to use the Requirements Bazaar API.
	 * @throws ReqBazException If something with the API went wrong.
	 */
	public void deleteCategory(ReqBazCategory category, String accessToken) throws ReqBazException {
		// this is the access token from the user that wants to create the project
		String oidcToken = accessToken;
				
		URL url;
		try {
			url = new URL(this.reqBazBackendUrl + "/categories/" + category.getId());

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("DELETE");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Authorization", "Bearer " + oidcToken);
					
		    // forward (in case of) error
            // 200 is ok, since then the category got deleted
			// 404 is ok, because then the category already got deleted before we tried to delete it (maybe manually by a user)
		    if (connection.getResponseCode() != 200 && connection.getResponseCode() != 404) {
				String message = getErrorMessage(connection);
				throw new ReqBazException(message);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new ReqBazException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new ReqBazException(e.getMessage());
		}
	}
	
	/**
	 * Creates the body needed to create a new category in a Requirements Bazaar project.
	 * @param categoryName Name of the category that should be created.
	 * @return Body as string.
	 */
	private String getCategoryBody(String categoryName) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("name", categoryName);
		jsonObject.put("description", "This category was auto-generated by the CAE.");
		jsonObject.put("projectId", this.reqBazProjectId);
		String body = JSONObject.toJSONString(jsonObject);
		return body;
	}
	
	/**
	 * Extracts the error message from the response.
	 * @param connection HttpURLConnection object
	 * @return Error message as String.
	 * @throws IOException
	 */
	private String getErrorMessage(HttpURLConnection connection) throws IOException {
		String message = "Error creating Requirements Bazaar category at: ";
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
		for (String line; (line = reader.readLine()) != null;) {
			message += line;
		}
		reader.close();
		return message;
	}
	
	/**
	 * Getter for the body of the response.
	 * @param connection HttpURLConnection object
	 * @return Body of the response as string.
	 * @throws IOException
	 */
	private String getResponseBody(HttpURLConnection connection) throws IOException {
		String response = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		for (String line; (line = reader.readLine()) != null;) {
			response += line;
		}
		reader.close();
		return response;
	}
	
	/**
	 * Writes the request body.
	 * @param connection HttpURLConnection object
	 * @param body Body that should be written to the request.
	 * @throws IOException
	 */
	private void writeRequestBody(HttpURLConnection connection, String body) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		writer.write(body);
		writer.flush();
		writer.close();
	}
}