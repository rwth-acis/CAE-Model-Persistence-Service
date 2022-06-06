package i5.las2peer.services.modelPersistenceService.testmodel;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import i5.las2peer.services.modelPersistenceService.testmodel.report.ReportFailure;
import i5.las2peer.services.modelPersistenceService.testmodel.report.ReportTestCase;
import i5.las2peer.services.modelPersistenceService.testmodel.report.ReportTestSuite;
import org.apache.commons.io.IOUtils;
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

		if(latestCommitWorkflowRun.getStatus() == GHWorkflowRun.Status.COMPLETED) {
			if(latestCommitWorkflowRun.getConclusion() == GHWorkflowRun.Conclusion.SUCCESS) {
				// mark all test cases as successful
				markAllTestCasesAs(testModel, "success");
			} else if(latestCommitWorkflowRun.getConclusion() == GHWorkflowRun.Conclusion.FAILURE) {
				// check which test cases were successful or failed
				GHArtifact artifact = getTestResultsArtifact(latestCommitWorkflowRun);
                ReportTestSuite testReport = extractTestResultFromArtifact(artifact);

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
		} else if(latestCommitWorkflowRun.getStatus() == GHWorkflowRun.Status.IN_PROGRESS) {
			// mark all test cases as in progress
			markAllTestCasesAs(testModel, "in_progress");
		}
	}

	/**
	 * Searches for the artifact of the workflow run which contains the test results.
	 * @param latestCommitWorkflowRun Workflow run that contains the artifact.
	 * @return Artifact of the given workflow run which contains the test results.
	 * @throws IOException
	 */
	private GHArtifact getTestResultsArtifact(GHWorkflowRun latestCommitWorkflowRun) throws IOException {
		for(GHArtifact artifact : latestCommitWorkflowRun.listArtifacts().toList()) {
			if (artifact.getName().equals("test-results")) {
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
		return artifact.download(is -> {
			ZipInputStream zipInputStream = new ZipInputStream(is);
			// search for test result xml file
			ZipEntry nextEntry = zipInputStream.getNextEntry();
			while (nextEntry != null) {
				if (nextEntry.getName().startsWith("TEST-i5"))
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
