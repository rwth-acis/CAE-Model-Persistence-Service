package i5.las2peer.services.modelPersistenceService.testmodel;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import i5.las2peer.services.modelPersistenceService.model.EntityAttribute;
import i5.las2peer.services.modelPersistenceService.model.Model;
import i5.las2peer.services.modelPersistenceService.model.node.Node;
import i5.las2peer.services.modelPersistenceService.testmodel.report.ReportFailure;
import i5.las2peer.services.modelPersistenceService.testmodel.report.ReportTestCase;
import i5.las2peer.services.modelPersistenceService.testmodel.report.ReportTestSuite;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.kohsuke.github.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * Helper class that allows to fetch the current state of test cases and their assertions
 * from GitHub Actions (whether they succeeded, failed or the run is still in progress).
 */
public class TestGHActionsHelper {

	/**
	 * Personal access token with read-access to the GitHub organization.
	 * This is needed, because artifacts of GitHub Actions workflows can not be
	 * accessed without being logged in.
	 */
	public String PERSONAL_ACCESS_TOKEN;

	/**
	 * Name of the GitHub organization, where the repositories of CAE generated
	 * applications are hosted.
	 */
	public String ORG_NAME;

	private GitHub github;

	public TestGHActionsHelper(String orgName, String personalAccessToken) throws IOException {
		this.ORG_NAME = orgName;
		this.PERSONAL_ACCESS_TOKEN = personalAccessToken;
		this.github = new GitHubBuilder().withOAuthToken(PERSONAL_ACCESS_TOKEN, ORG_NAME).build();
	}

	public void addTestCoverage(String latestCommitSha, Model model, String repoName) throws Exception {
		GHWorkflowRun latestCommitWorkflowRun = getLatestCommitWorkflowRun(repoName, latestCommitSha);
		if(latestCommitWorkflowRun == null) throw new Exception("Could not find a GitHub Actions workflow for the given latestCommitSha.");

		if(latestCommitWorkflowRun.getStatus() == GHWorkflowRun.Status.IN_PROGRESS) {
			return;
		}

		// get coverage report artifact
		GHArtifact artifact = getArtifactByName(latestCommitWorkflowRun, "swagger-coverage-report");
		if(artifact == null) return;

		// iterate over operations from coverage report
		JSONObject operations = getCoverageReportOperations(artifact);
        for(Object operationsKey : operations.keySet().toArray()) {
        	// get HTTP method and path of current operation
        	JSONObject operation = (JSONObject) operations.get(operationsKey);
            String httpMethod = getOperationHTTPMethod(operation);
			String path = getOperationPath(operation);

			JSONArray conditions = (JSONArray) operation.get("conditions");

            // search for this method in the model
			Node methodNode = getHttpMethodNode(model, httpMethod, path);
			if(methodNode != null) {
				// check if this method/path is covered
				int coverage = getPathCoverage(operation);

				// add coverage attribute to node
				addCoverageAttributeToNode(methodNode, coverage);

				// get nodes that are connected with the method node in the model
				model.getEdges().forEach(edge -> {
					if(edge.getSourceNode().equals(methodNode.getSyncMetaId())) {
						// find target node
						Node targetNode = model.getNodes().stream().filter(node -> node.getSyncMetaId().equals(edge.getTargetNode())).findFirst().get();
						String condition = getConditionForNode(targetNode);
						if(condition != null) {
							checkConditionCoverageForNode(targetNode, conditions, condition);
						}
					}
				});

			}
		}
	}

	/**
	 * Returns the condition (from the coverage report) that needs to be fulfilled for the given node to be
	 * covered by the tests.
	 * @param node
	 * @return
	 */
	private String getConditionForNode(Node node) {
		String condition = null;
		switch(node.getType()) {
			case "HTTP Response":
				// get status code
				String statusCodeValue = getNodeAttributeValue(node, "returnStatusCode");
				int statusCode = getStatusCodeFromText(statusCodeValue);

				// check if this status code is covered by the test
				condition = "HTTP status " + statusCode;
				break;
			case "HTTP Payload":
				// get payloadType
				String payloadType = getNodeAttributeValue(node, "payloadType");
				if(payloadType.equals("JSON") || payloadType.equals("TEXT")) {
					// check if body is covered by the test
					condition = "not empty body request";
				} else if(payloadType.equals("PATH_PARAM")) {
					// check if path param is covered by the test
					String paramName = getNodeAttributeValue(node, "name");
					condition = "path «" + paramName + "» is not empty";
				}
				break;
		}
		return condition;
	}

	private String getNodeAttributeValue(Node node, String attributeName) {
		return node.getAttributes().stream().filter(attr -> attr.getName().equals(attributeName)).findFirst().get().getValue();
	}

	/**
	 * Checks whether the given condition is covered by the given node.
	 * Adds coverage value to node attributes.
	 * @param node Node, e.g. a HTTP Response node.
	 * @param conditions All conditions which are part of the coverage report.
	 * @param condition Condition that should be checked.
	 */
	private void checkConditionCoverageForNode(Node node, JSONArray conditions, String condition) {
		Object conditionObj = conditions.stream().filter(c -> ((String) ((JSONObject) c).get("name")).equals(condition)).findFirst().orElse(null);
		if(conditionObj != null) {
			JSONObject conditionJSON = (JSONObject) conditionObj;
			boolean covered = (Boolean) conditionJSON.get("covered");
			int coverage = covered ? 100 : 0;
			addCoverageAttributeToNode(node, coverage);
		}
	}

	private String getOperationHTTPMethod(JSONObject operation) {
		JSONObject operationKey = (JSONObject) operation.get("operationKey");
		return (String) operationKey.get("httpMethod");
	}

	private String getOperationPath(JSONObject operation) {
		JSONObject operationKey = (JSONObject) operation.get("operationKey");
		String path = (String) operationKey.get("path");
		// remove / at the beginning
		path = path.startsWith("/") ? path.substring(1) : path;
		return path;
	}

	private JSONObject getCoverageReportOperations(GHArtifact artifact) throws IOException {
		// parse coverage report to JSON
		String coverageResultsStr = extractCoverageResultsFromArtifact(artifact);
		JSONObject coverageResults = (JSONObject) JSONValue.parse(coverageResultsStr);
		return (JSONObject) coverageResults.get("operations");
	}

	private void addCoverageAttributeToNode(Node node, int coverage) {
		node.getAttributes().add(new EntityAttribute("coverage", "coverage", String.valueOf(coverage)));
	}

	/**
	 * Returns 100 if path was covered by tests, 0 otherwise.
	 * @param operation Operation from coverage report.
	 * @return Returns 100 if path was covered by tests, 0 otherwise.
	 */
	private int getPathCoverage(JSONObject operation) {
		long processCount = (Long) operation.get("processCount");
		int coverage;
		if(processCount > 0) {
			// method was called at least once
			coverage = 100;
		} else {
			// method was never called
			coverage = 0;
		}
		return coverage;
	}

	/**
	 * Converts the given status code value (textual representation) to int.
	 * @param statusCodeValue Textual description of status code from microservice model.
	 * @return Status code int value.
	 */
	private int getStatusCodeFromText(String statusCodeValue) {
		int statusCode = 500;
		if(statusCodeValue.equals("OK")) statusCode = 200;
		else if(statusCodeValue.equals("CREATED")) statusCode = 201;
		else if(statusCodeValue.equals("BAD_REQUEST")) statusCode = 400;
		else if(statusCodeValue.equals("UNAUTHORIZED")) statusCode = 401;
		else if(statusCodeValue.equals("NOT_FOUND")) statusCode = 404;
		else if(statusCodeValue.equals("CONFLICT")) statusCode = 409;
		else if(statusCodeValue.equals("INTERNAL_ERROR")) statusCode = 500;
		return statusCode;
	}

	/**
	 * Searches for the "HTTP Method" node in the model that matches the given httpMethod and path.
	 * @param model Microservice model
	 * @param httpMethod e.g. GET
	 * @param path Path
	 * @return "HTTP Method" node or null if no node was found in the given model.
	 */
	private Node getHttpMethodNode(Model model, String httpMethod, String path) {
		for(Node node : getHttpMethodNodes(model)) {
			try {
				String modelMethodType = node.getAttributes().stream().filter(attr -> attr.getName().equals("methodType")).findFirst().orElseThrow().getValue();
				String modelPath = node.getAttributes().stream().filter(attr -> attr.getName().equals("path")).findFirst().orElseThrow().getValue();
				if(modelMethodType.equals(httpMethod) && modelPath.equals(path)) {
					return node;
				}
			} catch(NoSuchElementException e) {

			}
		}
		return null;
	}

	/**
	 * Returns nodes of type "HTTP Method" from the given model.
	 * @param model Microservice model
	 * @return Nodes of type "HTTP Method" from the given model.
	 */
	private List<Node> getHttpMethodNodes(Model model) {
		ArrayList<Node> nodes = model.getNodes();
		return nodes.stream().filter(node -> node.getType().equals("HTTP Method")).toList();
	}

	/**
	 * Updates the given test model: Sets status of test cases and assertions to their status in the
	 * GitHub Actions workflow that corresponds to the commit with the given sha.
	 * @param latestCommitSha Sha of the latest commit to the microservice.
	 * @param testModel Current state of the test model.
	 * @param repoName Name of the GitHub repository, where the code is hosted.
	 * @throws Exception
	 */
	public void addTestResults(String latestCommitSha, TestModel testModel, String repoName) throws Exception {
		GHWorkflowRun latestCommitWorkflowRun = getLatestCommitWorkflowRun(repoName, latestCommitSha);
		if(latestCommitWorkflowRun == null) throw new Exception("Could not find a GitHub Actions workflow for the given latestCommitSha.");

		if(latestCommitWorkflowRun.getStatus() == GHWorkflowRun.Status.IN_PROGRESS) {
			// mark all test cases as in progress
			markAllTestCasesAs(testModel, "in_progress");
			return;
		}

		GHArtifact artifact = getArtifactByName(latestCommitWorkflowRun, "test-results");

		// if there is no artifact with test results => build may have failed and tests were not executed
		if(artifact == null) return;

		ReportTestSuite testReport = extractTestResultFromArtifact(artifact);
		addRequestResponses(testModel, testReport);

		if(latestCommitWorkflowRun.getStatus() == GHWorkflowRun.Status.COMPLETED) {
			if(latestCommitWorkflowRun.getConclusion() == GHWorkflowRun.Conclusion.SUCCESS) {
				// mark all test cases as successful
				markAllTestCasesAs(testModel, "success");
			} else if(latestCommitWorkflowRun.getConclusion() == GHWorkflowRun.Conclusion.FAILURE) {
				// check which test cases were successful or failed

                // iterate over test cases from commit
				for(TestCase commitTestCase : testModel.getTestCases()) {
					// find this commit in the test report
					ReportTestCase reportTestCase = testReport.getTestCases().stream().filter(r -> r.getName().contains("ID" + commitTestCase.getId())).findAny().orElse(null);
					if(reportTestCase != null) {
						// found test case in test report
						// check if test case failed
						if(reportTestCase.failed()) {
							commitTestCase.setStatus("failed");
							// check which assertion failed
							ReportFailure failure = reportTestCase.getFailure();
                            for(TestRequest commitRequest : commitTestCase.getRequests()) {
                            	for(RequestAssertion commitAssertion : commitRequest.getAssertions()) {
                            		if(failure.getMessage().contains("[" + commitAssertion.getId() + "]")) {
                            			// this assertion failed
										commitAssertion.setStatus("failed");
										commitAssertion.setErrorMessage(failure.getMessage());
									}
								}
							}
						} else {
							commitTestCase.setStatus("success");
							markAllAssertionsAs(commitTestCase, "success");
						}
					}
				}
			}
		}
	}

	private void addRequestResponses(TestModel testModel, ReportTestSuite testReport) {
		String testSystemOut = testReport.getSystemOut();
		for(TestCase testCase : testModel.getTestCases()) {
			for(TestRequest request : testCase.getRequests()) {
				int requestId = request.getId();
				if(testSystemOut.contains("" + requestId)) {
					try {
						List<String> lines = IOUtils.readLines(new StringReader(testSystemOut));
						for(String line : lines) {
							if(line.contains(requestId + ": ")) {
								String response = line.split(requestId + ": ")[1];
								request.setLastResponse(response);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Searches for the artifact of the workflow run with the given name.
	 * @param latestCommitWorkflowRun Workflow run that contains the artifact.
	 * @param artifactName Name of the artifact that should be searched for.
	 * @return Artifact of the given workflow run which has the given name.
	 * @throws IOException
	 */
	private GHArtifact getArtifactByName(GHWorkflowRun latestCommitWorkflowRun, String artifactName) throws IOException {
		for(GHArtifact artifact : latestCommitWorkflowRun.listArtifacts().toList()) {
			if (artifact.getName().equals(artifactName)) {
				return artifact;
			}
		}
		return null;
	}

	/**
	 * Downloads the artifact and returns the test result.
	 * @param artifact Artifact to download.
	 * @return ReportTestSuite object containing the test result.
	 * @throws IOException
	 */
	private ReportTestSuite extractTestResultFromArtifact(GHArtifact artifact) throws IOException {
		String xml = extractTestResultXMLFromArtifact(artifact);
		JAXBContext jaxbContext = null;
		try {
			jaxbContext = JAXBContext.newInstance(ReportTestSuite.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			ReportTestSuite testSuite = (ReportTestSuite) jaxbUnmarshaller.unmarshal(new StringReader(xml));
			return testSuite;
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Downloads the artifact and searches for the test result XML file.
	 * @param artifact Artifact to download.
	 * @return Test result XML file content as String.
	 * @throws IOException
	 */
	private String extractTestResultXMLFromArtifact(GHArtifact artifact) throws IOException {
		return extractFileFromArtifact(artifact, "TEST-i5");
	}

	/**
	 * Downloads the artifact and searches for the swagger coverage results JSON file.
	 * @param artifact Artifact to download.
	 * @return Coverage results JSON file content as String.
	 * @throws IOException
	 */
	private String extractCoverageResultsFromArtifact(GHArtifact artifact) throws IOException {
		return extractFileFromArtifact(artifact, "swagger-coverage-results.json");
	}

	private String extractFileFromArtifact(GHArtifact artifact, String fileStartsWith) throws IOException {
		return artifact.download(is -> {
			ZipInputStream zipInputStream = new ZipInputStream(is);
			// search for file
			ZipEntry nextEntry = zipInputStream.getNextEntry();
			while (nextEntry != null) {
				if (nextEntry.getName().startsWith(fileStartsWith))
					break;
				nextEntry = zipInputStream.getNextEntry();
			}
			// return file content as String
			return IOUtils.toString(zipInputStream, StandardCharsets.UTF_8);
		});
	}

	/**
	 * Fetches a GitHub workflow run for a given commit.
	 * @param repoName Name of the repository, that the workflow is part of.
	 * @param latestCommitSha Sha of the latest commit, for which a workflow should be fetched.
	 * @return GHWorkflowRun
	 * @throws IOException
	 */
	private GHWorkflowRun getLatestCommitWorkflowRun(String repoName, String latestCommitSha) throws IOException {
		List<GHWorkflowRun> workflowRuns = getRepository(repoName).queryWorkflowRuns().list().toList();
		GHWorkflowRun latestCommitWorkflowRun = null;
		for(GHWorkflowRun run : workflowRuns) {
			if(run.getHeadSha().equals(latestCommitSha)) {
				latestCommitWorkflowRun = run;
				break;
			}
		}
		return latestCommitWorkflowRun;
	}

	/**
	 * Marks all test cases in the given testModel with the given status.
	 * Also marks all the assertions.
	 * @param testModel
	 * @param status
	 */
	private void markAllTestCasesAs(TestModel testModel, String status) {
		for(TestCase testCase : testModel.getTestCases()) {
			testCase.setStatus(status);
			markAllAssertionsAs(testCase, status);
		}
	}

	/**
	 * Marks all assertions in the given test case with the given status.
	 * @param testCase
	 * @param status
	 */
	private void markAllAssertionsAs(TestCase testCase, String status) {
		// iterate over all requests to mark all their assertions with the given status
		for(TestRequest request : testCase.getRequests()) {
			for(RequestAssertion assertion : request.getAssertions()) {
				assertion.setStatus(status);
			}
		}
	}

	private GHRepository getRepository(String repoName) throws IOException {
		return github.getOrganization(ORG_NAME).getRepository(repoName);
	}
}
