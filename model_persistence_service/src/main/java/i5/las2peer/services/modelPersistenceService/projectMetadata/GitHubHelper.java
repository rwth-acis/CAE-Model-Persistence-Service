package i5.las2peer.services.modelPersistenceService.projectMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import i5.las2peer.services.modelPersistenceService.exception.GitHubException;

/**
 * Helper class for working with GitHub API.
 * Currently supports creating new GitHub projects and update their
 * visibility to public.
 * @author Philipp
 *
 */
public class GitHubHelper {

	private static final String API_BASE_URL = "https://api.github.com";

	/**
	 * Returns an ArrayList containing the version tags of the given repository as strings.
	 * @param repoOwner Owner/account on GitHub where the repository is hosted.
	 * @param repoName Name of the GitHub repository.
	 * @return ArrayList containing the version tags of the repository as strings.
	 * @throws GitHubException If something with the API request went wrong.
	 */
	public static ArrayList<String> getRepoVersionTags(String repoOwner, String repoName) throws GitHubException {
		ArrayList<String> tags = new ArrayList<>();
		URL url;
		try {
			url = new URL(API_BASE_URL + "/repos/" + repoOwner + "/" + repoName + "/tags");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoInput(true);
			connection.connect();
			
			// forward (in case of) error
			if (connection.getResponseCode() != 200) {
				String message = getErrorMessage(connection);
				throw new GitHubException(message);
			} else {
				// get response
				String response = getResponseBody(connection);
							
				// convert to JSONObject
				JSONArray json = (JSONArray) JSONValue.parseWithException(response);
				for(Object o : json) {
					JSONObject tag = (JSONObject) o;
					tags.add((String) tag.get("name"));
				}
				return tags;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		} catch (ParseException e) {
			e.printStackTrace();
			throw new GitHubException(e.getMessage());
		}
	}
	
	/**
	 * Extracts the error message from the response.
	 * @param connection HttpURLConnection object
	 * @return Error message as String.
	 * @throws IOException
	 */
	private static String getErrorMessage(HttpURLConnection connection) throws IOException {
		String message = "Error: ";
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
	private static String getResponseBody(HttpURLConnection connection) throws IOException {
		String response = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		for (String line; (line = reader.readLine()) != null;) {
			response += line;
		}
		reader.close();
		return response;
	}
}