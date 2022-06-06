package i5.las2peer.services.modelPersistenceService.testmodel.report;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Java class for "testcase" element in JUnit XML report.
 */
public class ReportTestCase {

    @XmlAttribute
    private String name;

    @XmlElement(name = "failure")
    private ReportFailure failure;

    public String getName() {
        return name;
    }

    public ReportFailure getFailure() {
        return failure;
    }

    public boolean failed() {
        return this.getFailure() != null;
    }
}
