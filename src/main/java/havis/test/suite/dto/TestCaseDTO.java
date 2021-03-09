package havis.test.suite.dto;

import havis.test.suite.testcase.TestCaseType;

import java.util.Map;


public class TestCaseDTO {
	private TestCasesDTO base;
	private String id;
	private String name;
	private String home;
	private Map<String, Object> parameters;

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public TestCasesDTO getBase() {
		return base;
	}

	public void setBase(TestCasesDTO base) {
		this.base = base;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHome() {
		return home;
	}

	public void setHome(String home) {
		this.home = home;
	}

	private TestCaseType testCase;

	public TestCaseType getTestCase() {
		return testCase;
	}

	public void setTestCase(TestCaseType testCase) {
		this.testCase = testCase;
	}

}
