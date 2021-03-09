package havis.test.suite;

import havis.test.suite.common.helpers.PathResolverFileHelper;
import havis.test.suite.common.messaging.XMLMessage;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCasesDTO;
import havis.test.suite.testcase.EntryType;
import havis.test.suite.testcase.TestCaseType;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestCaseTest {

	private final String testCasesDescriptorFileName;
	private final String testCasesDir;
	private final String objectsFile;

	private TestAutomatDTO testAutomatDTO;
	private TestCasesDTO testCasesDTO;

	public TestCaseTest() throws IOException, URISyntaxException {
		testCasesDir = "test/Havis/RfidTestSuite/Testautomat/testcases";
		objectsFile = PathResolverFileHelper
				.getAbsolutePathFromResource(
						"test/Havis/RfidTestSuite/Testautomat/ModuleTest")
				.get(0).toString();
		testCasesDescriptorFileName = "testCase.xml";
	}

	@BeforeClass
	public void setUp() {
		// initialize spring framework
		GenericApplicationContext objContext = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(objContext);
		reader.loadBeanDefinitions("file:///" + objectsFile + File.separator
				+ "beans.xml");
		objContext.refresh();
		Map<String, String> map = new HashMap<String, String>();
		map.put("o1", objectsFile);
		UI ui = new UI();
		// create test automat DTO
		testAutomatDTO = new TestAutomatDTO();
		testAutomatDTO.setObjContext(objContext);
		testAutomatDTO.setObjectIdFiles(map);
		testAutomatDTO.setUi(ui);
		testCasesDTO = new TestCasesDTO();
		testCasesDTO.setHome(testCasesDir);
		testCasesDTO.setDescriptorFileName(testCasesDescriptorFileName);
	}

	@Test
	public void run() throws Exception {
		final String testCaseName = "TestCase";
		String testCaseHome = testCasesDir + "/TestCase";
		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> innerMap = new HashMap<String, Object>();
		innerMap.put("value", "v1");
		map.put("prop", innerMap);
		map.put("a", "1");
		testCasesDTO.setParameters(map);
		innerMap = new HashMap<String, Object>();
		map.put("a", "2");
		map.put("b", "3");
		TestCase testCase = new TestCase(testAutomatDTO, testCasesDTO,
				testCaseHome, innerMap, null);
		testCase.setParentStepId("huhu");
		Step step = (Step) testAutomatDTO.getObjContext().getBean("o1");
		step.initialize();

		testCase.run();

		Assert.assertTrue(step.isRunCalled());

		Assert.assertEquals(step.getModuleHome(),
				Paths.get(testAutomatDTO.getObjectIdFiles().get("o1"))
						.getParent().toString());
		Assert.assertEquals(step.getTestCaseInfo().getName(), testCaseName);
		Assert.assertEquals(step.getTestCaseInfo().getHome(), testCaseHome);
		Assert.assertFalse(step.getTestCaseInfo().getId() == null
				|| step.getTestCaseInfo().getId().length() == 0);
		Assert.assertFalse(step.getStepId() == null
				|| step.getStepId().length() == 0);
		Assert.assertEquals(step.getStepProperties().size(), 4);
		Assert.assertEquals(step.getStepProperties().get("testCaseId"), step
				.getTestCaseInfo().getId());
		Assert.assertEquals(step.getStepProperties().get("propValue"), "v1");
		Assert.assertEquals(step.getStepProperties().get("a"), "2");
		Assert.assertEquals(step.getStepProperties().get("b"), "3");

		// use reporters
		// 2 reports expected: intermediate report and final report for the test
		// case
		Reporter reporter1 = new Reporter();
		Reporter reporter2 = new Reporter();

		List<havis.test.suite.api.Reporter> reporters = new ArrayList<havis.test.suite.api.Reporter>();
		reporters.add(reporter1);
		reporters.add(reporter2);

		testCase.setReporters(reporters);
		testCase.run();

		Assert.assertEquals(reporter1.getReports().size(), 1);
		Assert.assertEquals(reporter2.getReports().size(), 1);
		List<String> reports = reporter1.getReports().get(testCaseName);
		Assert.assertEquals(reports.size(), 2);

		// check intermediate report for data which are set in the test case
		// runner
		TestCaseType testCaseReport = (TestCaseType) new XMLMessage(
				reports.get(0), TestCaseType.class).getDeserializedObject();
		Assert.assertNull(testCaseReport.getReport().getEndTime());

		for (EntryType testCaseId : testCaseReport.getReport().getParameters()
				.getParameter()) {
			if (testCaseId.getName().equals("testCaseID")) {
				Assert.assertEquals(testCaseId.getValue(), step
						.getTestCaseInfo().getId());
			}
		}
		Assert.assertEquals(testCaseReport.getReport().getParentStepId(),
				"huhu");

		testCaseReport = (TestCaseType) new XMLMessage(reports.get(1),
				TestCaseType.class).getDeserializedObject();
		Assert.assertNotNull(testCaseReport.getReport().getEndTime());
		for (EntryType testCaseId : testCaseReport.getReport().getParameters()
				.getParameter()) {
			if (testCaseId.getName().equals("testCaseID")) {
				Assert.assertEquals(testCaseId.getValue(), step
						.getTestCaseInfo().getId());
			}
		}
		Assert.assertEquals(testCaseReport.getReport().getParentStepId(),
				"huhu");

	}
}
