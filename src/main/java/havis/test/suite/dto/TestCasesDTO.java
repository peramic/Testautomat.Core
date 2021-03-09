package havis.test.suite.dto;

import havis.test.suite.common.messaging.XSD;

import java.util.Map;


public class TestCasesDTO {
	//The home directory of all test cases.
	private String home;
	//The name of the deployment descriptor file.
	private String descriptorFileName;
	//The XSD file for the test cases.
	private XSD xsd;
	//A list of key/value pairs which can be used in a test case
	//definition/template.
	private Map<String, Object> parameters;

	public XSD getXsd() {
		return xsd;
	}

	public void setXsd(XSD xsd) {
		this.xsd = xsd;
	}

	public String getHome() {
		return home;
	}

	public void setHome(String home) {
		this.home = home;
	}

	public String getDescriptorFileName() {
		return descriptorFileName;
	}

	public void setDescriptorFileName(String descriptorFileName) {
		this.descriptorFileName = descriptorFileName;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

}
