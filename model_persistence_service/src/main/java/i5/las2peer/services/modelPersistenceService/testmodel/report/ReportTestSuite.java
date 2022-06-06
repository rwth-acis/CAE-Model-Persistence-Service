package i5.las2peer.services.modelPersistenceService.testmodel.report;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Java class for "testsuite" element in JUnit XML report.
 */
@XmlRootElement(name = "testsuite")
public class ReportTestSuite {

    @XmlAttribute
    private int tests;

    @XmlAttribute
    private int failures;

    @XmlElement(name = "testcase")
    private List<ReportTestCase> testCases;

    public int getTests() {
        return tests;
    }

    public int getFailures() {
        return failures;
    }

    public List<ReportTestCase> getTestCases() {
        return testCases;
    }
}
