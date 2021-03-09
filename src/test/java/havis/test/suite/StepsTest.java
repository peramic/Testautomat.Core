package havis.test.suite;

import havis.test.suite.common.IO;
import havis.test.suite.common.helpers.PathResolverFileHelper;
import havis.test.suite.common.messaging.XMLMessage;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCaseDTO;
import havis.test.suite.dto.TestCasesDTO;
import havis.test.suite.exceptions.ReportedException;
import havis.test.suite.exceptions.VerificationException;
import havis.test.suite.testcase.EntryType;
import havis.test.suite.testcase.LoopType;
import havis.test.suite.testcase.ModuleReportType;
import havis.test.suite.testcase.ModuleReportsType;
import havis.test.suite.testcase.ModuleType;
import havis.test.suite.testcase.ParametersType;
import havis.test.suite.testcase.StepReportType;
import havis.test.suite.testcase.StepReportsType;
import havis.test.suite.testcase.StepType;
import havis.test.suite.testcase.TestCaseReportType;
import havis.test.suite.testcase.TestCaseType;
import havis.test.suite.testcase.ThreadGroupType;
import havis.test.suite.testcase.ThreadsType;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeFactory;

import org.joda.time.DateTime;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class StepsTest {

	private final String testCasesDescriptorFileName;
	private final String testCasesDir;
	private final String objectsFile;

	private TestAutomatDTO testAutomatDTO;
	private TestCasesDTO testCasesDTO;

	public StepsTest() throws IOException, URISyntaxException {
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
		map.put("o2", objectsFile);
		map.put("common.sleep", objectsFile);
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
	public void module() throws Exception {
		// load test case 1 with 1 step: valid module usage
		String testCaseName = "Havis.RfidTestSuite.Testautomat.testcases.StepsModule1";
		String testCaseHome = testCasesDir + "/StepsModule1/";
		String content = new IO(testAutomatDTO.getObjContext())
				.loadResource(testCaseHome + "/" + testCasesDescriptorFileName);
		TestCaseType testCase1 = (TestCaseType) new XMLMessage(content,
				TestCaseType.class).getDeserializedObject();

		// get step module (declared as singleton)
		Step step = (Step) testAutomatDTO.getObjContext().getBean("o1");
		step.initialize();
		TestCaseDTO tcDTO = new TestCaseDTO();
		tcDTO.setId("huhu");
		tcDTO.setBase(testCasesDTO);
		tcDTO.setName(testCaseName);
		tcDTO.setHome(testCaseHome);
		tcDTO.setTestCase(testCase1);
		Steps steps = new Steps(testAutomatDTO, tcDTO, testCase1.getSteps()
				.getStep(), testCaseName);
		steps.run();
		// all interface methods had to be called
		Assert.assertTrue(step.isPreparedCalled());
		Assert.assertTrue(step.isRunCalled());
		Assert.assertTrue(step.isFinishedCalled());
		// check parameters from prepare call
		Assert.assertEquals(step.getModuleHome(),
				Paths.get(testAutomatDTO.getObjectIdFiles().get("o1"))
						.getParent().toString());
		Assert.assertEquals(step.getTestCaseInfo().getId(), "huhu");
		Assert.assertEquals(step.getTestCaseInfo().getName(), testCaseName);
		Assert.assertEquals(step.getTestCaseInfo().getHome(), testCaseHome);
		Assert.assertFalse(step.getStepId() == null
				|| step.getStepId().length() == 0);
		Assert.assertEquals(step.getStepProperties().size(), 0);

		// add reporters for intermediate reports and execute the steps again
		// expected: no errors => no reports
		Reporter reporter1 = new Reporter();
		Reporter reporter2 = new Reporter();
		List<havis.test.suite.api.Reporter> reporters = new ArrayList<havis.test.suite.api.Reporter>();
		reporters.add(reporter1);
		reporters.add(reporter2);
		steps.setReporters(reporters);
		steps.run();
		Assert.assertEquals(reporter1.getReports().size(), 0);
		Assert.assertEquals(reporter2.getReports().size(), 0);

		// load test case 2 with 5 steps: valid module usage, verification
		// error,
		// exception while loading object, verification error throwing an
		// exception,
		// valid module usage
		// Step 4 stops the processing by throwing an exception for the
		// verification error
		// (no intermediate report expected for step 4)
		// 2 intermediate reports expected: verification error, exception while
		// loading object
		testCaseName = "Havis.RfidTestSuite.Testautomat.testcases.StepsModule2";
		testCaseHome = testCasesDir + "/StepsModule2/";
		content = new IO(testAutomatDTO.getObjContext())
				.loadResource(testCaseHome + "/" + testCasesDescriptorFileName);
		testCase1 = (TestCaseType) new XMLMessage(content, TestCaseType.class)
				.getDeserializedObject();
		TestCaseReportType tcrt = new TestCaseReportType();
		tcrt.setName(testCaseName);// required by reporter

		EntryType entryType = new EntryType();
		entryType.setName("testCaseId");
		entryType.setValue("huhu");
		tcrt.setParameters(new ParametersType());
		tcrt.getParameters().getParameter().add(entryType);
		tcrt.setStartTime(DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(new DateTime().toGregorianCalendar()));
		testCase1.setReport(tcrt);
		// get step module (declared as singleton)
		step = (Step) testAutomatDTO.getObjContext().getBean("o1");
		step.initialize();
		reporter1 = new Reporter();
		reporter1.initialize();
		reporters = new ArrayList<havis.test.suite.api.Reporter>();
		reporters.add(reporter1);
		tcDTO = new TestCaseDTO();
		tcDTO.setId("huhu");
		tcDTO.setBase(testCasesDTO);
		tcDTO.setName(testCaseName);
		tcDTO.setHome(testCaseHome);
		tcDTO.setTestCase(testCase1);
		steps = new Steps(testAutomatDTO, tcDTO,
				testCase1.getSteps().getStep(), testCaseName);
		steps.setReporters(reporters);
		try {
			steps.run();
			Assert.fail();
		} catch (VerificationException e) {
			Assert.assertEquals(e.getMessage(), "Verification 'ver2' failed");
		} catch (ReportedException e) {
			Assert.fail();
		} catch (Exception e) {
			Assert.fail();
		}
		// 2 reports for 1 test case expected
		Assert.assertEquals(reporter1.getReports().size(), 1);
		List<String> reports = reporter1.getReports().get(testCaseName);
		Assert.assertEquals(reports.size(), 2);

		// check 1. intermediate report: step 2 with verification error
		TestCaseType testCaseReport = (TestCaseType) new XMLMessage(
				reports.get(0), TestCaseType.class).getDeserializedObject();
		// end time must not exist
		Assert.assertNull(testCaseReport.getReport().getEndTime());
		// 1. step must contain passNo 0 and a stepId
		StepReportType step0Report = testCaseReport.getSteps().getStep().get(0)
				.getReports().getReport().get(0);
		Assert.assertEquals(step0Report.getPassNo(), 0);
		Assert.assertFalse(step0Report.getStepId() == null
				|| step0Report.getStepId().length() == 0);
		// 2. step must contain passNo 0 and a stepId differing from the
		// previous step
		StepReportType step1Report = testCaseReport.getSteps().getStep().get(1)
				.getReports().getReport().get(0);
		Assert.assertEquals(step1Report.getPassNo(), 0);
		Assert.assertNotEquals(step1Report.getStepId(), step0Report.getStepId());
		// the module reports a verification error
		ModuleReportType stepModuleReport = testCaseReport.getSteps().getStep()
				.get(1).getModule().getReports().getReport().get(0);
		Assert.assertFalse(stepModuleReport.getResult().isIsException());

		String lineSepOffset = "";
		if (System.getProperty("line.separator").length() == 1) {
			lineSepOffset = "e";
		}

		Assert.assertEquals(
				stepModuleReport.getVerifications().getVerification()
						.get(0).getDiff()
						.replaceAll("\n", System.getProperty("line.separator")),
				"- ..." + lineSepOffset + "ncoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<anyResult/>"
						+ System.getProperty("line.separator") + "..."
						+ System.getProperty("line.separator") + "+ ..."
						+ lineSepOffset + "ncoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<oh/>"
						+ System.getProperty("line.separator") + "...");
		// all other steps are not executed
		Assert.assertNull(testCaseReport.getSteps().getStep().get(2)
				.getModule().getReports());
		Assert.assertNull(testCaseReport.getSteps().getStep().get(3)
				.getModule().getReports());
		Assert.assertNull(testCaseReport.getSteps().getStep().get(4)
				.getModule().getReports());

		// check 2. intermediate report: step 2 with verification error
		// and step 3 with exception while loading object
		testCaseReport = (TestCaseType) new XMLMessage(reports.get(1),
				TestCaseType.class).getDeserializedObject();
		// end time must not exist
		Assert.assertNull(testCaseReport.getReport().getEndTime());
		// 1. step must contain passNo 0 and a stepId
		step0Report = testCaseReport.getSteps().getStep().get(0).getReports()
				.getReport().get(0);
		Assert.assertEquals(step0Report.getPassNo(), 0);
		Assert.assertFalse(step0Report.getStepId() == null
				|| step0Report.getStepId().length() == 0);
		// 2. step must contain passNo 0 and a stepId differing from the
		// previous step
		step1Report = testCaseReport.getSteps().getStep().get(1).getReports()
				.getReport().get(0);
		Assert.assertEquals(step1Report.getPassNo(), 0);
		Assert.assertNotEquals(step1Report.getStepId(), step0Report.getStepId());
		// the module reports a verification error
		stepModuleReport = testCaseReport.getSteps().getStep().get(1)
				.getModule().getReports().getReport().get(0);
		Assert.assertFalse(stepModuleReport.getResult().isIsException());
		Assert.assertEquals(
				stepModuleReport.getVerifications().getVerification()
						.get(0).getDiff()
						.replaceAll("\n", System.getProperty("line.separator")),
				"- ..." + lineSepOffset + "ncoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<anyResult/>"
						+ System.getProperty("line.separator") + "..."
						+ System.getProperty("line.separator") + "+ ..."
						+ lineSepOffset + "ncoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<oh/>"
						+ System.getProperty("line.separator") + "...");
		// 3. step must contain passNo 0 and a stepId differing from the
		// previous steps
		// the step report must contain the exception
		StepReportType step2Report = testCaseReport.getSteps().getStep().get(2)
				.getReports().getReport().get(0);
		Assert.assertEquals(step2Report.getPassNo(), 0);
		Assert.assertNotEquals(step2Report.getStepId(), step0Report.getStepId());
		Assert.assertNotEquals(step2Report.getStepId(), step1Report.getStepId());
		Assert.assertTrue(step2Report.getException().contains(
				"'oh' cannot be found")
				&& step2Report.getException().contains("stacktrace>"));

		// load test case 3 with 3 steps: valid module usage, exception while
		// loading object,
		// valid module usage
		// Step 2 stops the processing by throwing an exception while loading
		// the object
		// no intermediate report expected
		testCaseName = "Havis.RfidTestSuite.Testautomat.testcases.StepsModule3";
		testCaseHome = testCasesDir + "/StepsModule3/";
		;
		content = new IO(testAutomatDTO.getObjContext())
				.loadResource(testCaseHome + "/" + testCasesDescriptorFileName);
		testCase1 = (TestCaseType) new XMLMessage(content, TestCaseType.class)
				.getDeserializedObject();
		tcrt = new TestCaseReportType();
		tcrt.setName(testCaseName);// required by reporter

		entryType = new EntryType();
		entryType.setName("testCaseId");
		entryType.setValue("huhu");
		tcrt.setParameters(new ParametersType());
		tcrt.getParameters().getParameter().add(entryType);

		tcrt.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar()));
		testCase1.setReport(tcrt);
		// get step module (declared as singleton)
		step = (Step) testAutomatDTO.getObjContext().getBean("o1");
		step.initialize();
		reporter1 = new Reporter();
		reporter1.initialize();
		reporters = new ArrayList<havis.test.suite.api.Reporter>();
		reporters.add(reporter1);
		tcDTO = new TestCaseDTO();
		tcDTO.setId("huhu");
		tcDTO.setBase(testCasesDTO);
		tcDTO.setName(testCaseName);
		tcDTO.setHome(testCaseHome);
		tcDTO.setTestCase(testCase1);
		steps = new Steps(testAutomatDTO, tcDTO,
				testCase1.getSteps().getStep(), testCaseName);
		steps.setReporters(reporters);

		try {
			steps.run();
			Assert.fail();
		} catch (VerificationException e) {
			Assert.fail();
		} catch (ReportedException e) {
			Assert.assertTrue(e.getCause().getMessage()
					.contains("'oh' cannot be found"));
		} catch (Exception e) {
			Assert.fail();
		}
		Assert.assertEquals(reporter1.getReports().size(), 0);

	}

	//TODO Test import
	public void testImport() throws Exception {
		// load test case with 3 steps: valid module usage,
		// import of test case "StepsImport.Import1" throwing a verification
		// error,
		// import of test case "StepsImport.Import2" throwing an exception,
		// valid module usage
		final String testCaseName = "Havis.RfidTestSuite.Testautomat.testcases.StepsImport";
		String testCaseHomeRes = testCasesDir + "/StepsImport/";
		String testCaseHome = testCasesDir.replace("/", File.separator)
				+ File.separator + "StepsImport";
		String content = new IO(testAutomatDTO.getObjContext())
				.loadResource(testCaseHomeRes + "/"
						+ testCasesDescriptorFileName);
		TestCaseType testCase1 = (TestCaseType) new XMLMessage(content,
				TestCaseType.class).getDeserializedObject();
		TestCaseReportType tcrt = new TestCaseReportType();
		tcrt.setName(testCaseName);
		EntryType entryType = new EntryType();
		entryType.setName("testCaseId");
		entryType.setValue("huhu");
		tcrt.setParameters(new ParametersType());
		tcrt.getParameters().getParameter().add(entryType);
		tcrt.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar()));
		testCase1.setReport(tcrt);

		// create a reporter
		Reporter reporter1 = new Reporter();
		reporter1.initialize();
		// execute steps
		// 1 final report expected for imported test case "StepsImport.Import1"
		// (step 2 of imported test case throws the exception to parent test
		// case
		// => no intermediate report for imported test case
		// "StepsImport.Import1")
		// 1 final report expected for imported test case "StepsImport.Import2"
		// (step 2 of imported test case throws the exception to parent test
		// case
		// => no intermediate report for imported test case
		// "StepsImport.Import2")
		// 2 intermediate reports expected for main test case caused by step 2 +
		// 3
		TestCaseDTO tcDTO = new TestCaseDTO();
		tcDTO.setId("huhu");
		tcDTO.setBase(testCasesDTO);
		tcDTO.setName(testCaseName);
		tcDTO.setHome(testCaseHome);
		tcDTO.setTestCase(testCase1);
		List<havis.test.suite.api.Reporter> reporters = new ArrayList<havis.test.suite.api.Reporter>();
		reporters.add(reporter1);
		Steps steps = new Steps(testAutomatDTO, tcDTO, testCase1.getSteps()
				.getStep(), null);
		steps.setReporters(reporters);
		steps.run();
		// 3 report lists: StepsImport.Import1, StepsImport.Import2, StepsImport
		Assert.assertEquals(reporter1.getReports().size(), 3);
		List<String> reports1 = reporter1.getReports().get(
				"StepsImport.Import1");
		Assert.assertEquals(reports1.size(), 1);
		TestCaseType report1 = (TestCaseType) new XMLMessage(reports1.get(0),
				TestCaseType.class).getDeserializedObject();
		List<String> reports2 = reporter1.getReports().get(
				"StepsImport.Import2");
		Assert.assertEquals(reports2.size(), 1);
		TestCaseType report2 = (TestCaseType) new XMLMessage(reports2.get(0),
				TestCaseType.class).getDeserializedObject();
		List<String> reports3 = reporter1.getReports().get(
				"Havis.RfidTestSuite.Testautomat.testcases.StepsImport");
		Assert.assertEquals(reports3.size(), 2);
		TestCaseType report3A = (TestCaseType) new XMLMessage(reports3.get(0),
				TestCaseType.class).getDeserializedObject();
		TestCaseType report3B = (TestCaseType) new XMLMessage(reports3.get(1),
				TestCaseType.class).getDeserializedObject();

		// check final report of imported test case "StepsImport.Import1"
		// name
		Assert.assertEquals(report1.getReport().getName(),
				"StepsImport.Import1");
		// parentStepId must match stepId of first intermediate report of main
		// test case
		Assert.assertTrue(report3A.getSteps().getStep().get(1).getReports()
				.getReport().get(0).getStepId()
				.equals(report1.getReport().getParentStepId()));
		// final report
		Assert.assertNotNull(report1.getReport().getEndTime());
		// pass no
		for (int i = 0; i < report1.getSteps().getStep().size(); i++) {
			ModuleType module = report1.getSteps().getStep().get(i).getModule();
			if (i < 2) {
				// first to failed step
				Assert.assertEquals(module.getReports().getReport().get(0)
						.getPassNo(), 0);
			} else {
				Assert.assertNull(module.getReports());
			}
		}

		// check final report of imported test case "StepsImport.Import2"
		// name
		Assert.assertEquals(report2.getReport().getName(),
				"StepsImport.Import2");
		// parentStepId must match stepId of second intermediate report of main
		// test case
		Assert.assertTrue(report3B.getSteps().getStep().get(2).getReports()
				.getReport().get(0).getStepId()
				.equals(report2.getReport().getParentStepId()));
		// final report
		Assert.assertNotNull(report2.getReport().getEndTime());
		// pass no
		for (int i = 0; i < report2.getSteps().getStep().size(); i++) {
			ModuleType module = report2.getSteps().getStep().get(i).getModule();
			if (i < 2) {
				// first to failed step
				Assert.assertEquals(module.getReports().getReport().get(0)
						.getPassNo(), 0);
			} else {
				Assert.assertNull(module.getReports());
			}
		}

		// check first intermediate report of main test case
		// name
		Assert.assertEquals(report3A.getReport().getName(),
				"Havis.RfidTestSuite.Testautomat.testcases.StepsImport");
		// intermediate report
		Assert.assertNull(report3A.getReport().getEndTime());
		// pass no
		ModuleType module = report3A.getSteps().getStep().get(0).getModule();
		Assert.assertEquals(module.getReports().getReport().get(0)
				.getPassNo(), 0);
		Assert.assertEquals(report3A.getSteps().getStep().get(1).getReports()
				.getReport().get(0).getPassNo(), 0);
		Assert.assertNull(report3A.getSteps().getStep().get(2).getReports());
		module = report3A.getSteps().getStep().get(3).getModule();
		Assert.assertNull(module.getReports());

		// check second intermediate report of main test case
		// name
		Assert.assertEquals(report3B.getReport().getName(),
				"Havis.RfidTestSuite.Testautomat.testcases.StepsImport");
		// intermediate report
		Assert.assertNull(report3B.getReport().getEndTime());
		// pass no
		module = report3B.getSteps().getStep().get(0).getModule();
		Assert.assertEquals(module.getReports().getReport().get(0)
				.getPassNo(), 0);
		for (int i = 1; i < 3; i++) {
			Assert.assertEquals(report3B.getSteps().getStep().get(i)
					.getReports().getReport().get(0).getPassNo(), 0);
		}
		module = report3B.getSteps().getStep().get(3).getModule();
		Assert.assertNull(module.getReports());
	}

	@Test
	public void testLoop() throws Exception {
		// load test case 1
		final String testCaseName = "Havis.RfidTestSuite.Testautomat.testcases.StepsLoop";
		String testCaseHome = testCasesDir + "/StepsLoop";

		String content = new IO(testAutomatDTO.getObjContext())
				.loadResource(testCaseHome + "/" + testCasesDescriptorFileName);
		TestCaseType testCase1 = (TestCaseType) new XMLMessage(content,
				TestCaseType.class).getDeserializedObject();
		TestCaseReportType tcrt = new TestCaseReportType();
		tcrt.setName(testCaseName);
		testCase1.setReport(tcrt);
		// create a reporter
		Reporter reporter1 = new Reporter();
		reporter1.initialize();
		// execute steps
		// 2 final report expected from imported test case
		// "StepsLoop.Import1.Import2"
		// 1 final report expected from imported test case "StepsLoop.Import1"
		TestCaseDTO tcDTO = new TestCaseDTO();
		tcDTO.setId("huhu");
		tcDTO.setBase(testCasesDTO);
		tcDTO.setName(testCaseName);
		tcDTO.setHome(testCaseHome);
		tcDTO.setTestCase(testCase1);
		List<havis.test.suite.api.Reporter> reporters = new ArrayList<havis.test.suite.api.Reporter>();
		reporters.add(reporter1);
		Steps steps = new Steps(testAutomatDTO, tcDTO, testCase1.getSteps()
				.getStep(), null);
		steps.setReporters(reporters);
		steps.run();

		// 2 report lists: StepsLoop.Import1.Import2, StepsLoop.Import1
		Assert.assertEquals(reporter1.getReports().size(), 2);
		List<String> reports3 = reporter1.getReports().get(
				"StepsLoop.Import1.Import2");
		Assert.assertEquals(reports3.size(), 2);
		List<String> reports2 = reporter1.getReports().get("StepsLoop.Import1");
		Assert.assertEquals(reports2.size(), 1);

		// check reports from imported test case "StepsLoop.Import1.Import2"
		TestCaseType report3P1 = (TestCaseType) new XMLMessage(reports3.get(0),
				TestCaseType.class).getDeserializedObject();
		TestCaseType report3P2 = (TestCaseType) new XMLMessage(reports3.get(1),
				TestCaseType.class).getDeserializedObject();

		// names
		Assert.assertTrue(report3P1.getReport().getName()
				.equals("StepsLoop.Import1.Import2"));
		Assert.assertTrue(report3P2.getReport().getName()
				.equals("StepsLoop.Import1.Import2"));

		// check report from imported test case "StepsLoop.Import1"
		TestCaseType report2 = (TestCaseType) new XMLMessage(reports2.get(0),
				TestCaseType.class).getDeserializedObject();
		Assert.assertTrue(report2.getReport().getName()
				.equals("StepsLoop.Import1"));

		// check nested loop in report from imported StepsLoop.Import1
		StepType step0 = report2.getSteps().getStep().get(0);
		LoopType step0Loop = step0.getLoop();
		ModuleType step0Module = step0Loop.getSteps().getStep().get(0)
				.getModule();
		// report count
		Assert.assertEquals(step0Module.getReports()
				.getReport().size(), step0Loop.getCount());
		// pass no
		for (int i = 0; i < step0Module.getReports().getReport().size(); i++) {
			ModuleReportType report = step0Module.getReports().getReport()
					.get(i);
			Assert.assertEquals(report.getPassNo(), i);
		}

		// check normal step in report from imported test case
		// "StepsLoop.Import1"
		StepType step01 = report2.getSteps().getStep().get(1);
		ModuleType step1Module = step01.getModule();
		// report count
		Assert.assertEquals(step1Module.getReports().getReport().size(), 1);
		// pass no
		Assert.assertEquals(step1Module.getReports().getReport().get(0)
				.getPassNo(), 0);

		// check loop with content wich will never be executed
		StepType step2 = report2.getSteps().getStep().get(2);
		LoopType step2Loop = step2.getLoop();
		ModuleType step2Module = step2Loop.getSteps().getStep().get(0)
				.getModule();
		// report count
		Assert.assertNull(step2Module.getReports());

		// check loop over imports in report from imported test case
		// "StepsLoop.Import1"
		StepType step3 = report2.getSteps().getStep().get(3);
		LoopType step3Loop = step3.getLoop();
		StepReportsType step3LoopStep0 = step3Loop.getSteps().getStep().get(0)
				.getReports();
		// report count
		Assert.assertEquals(step3LoopStep0.getReport().size(), 2);
		// pass no
		for (int i = 0; i < step3LoopStep0.getReport().size(); i++) {
			Assert.assertEquals(step3LoopStep0.getReport().get(i)
					.getPassNo(), i);
		}
		// parentStepId
		Assert.assertEquals(report3P1.getReport().getParentStepId(),
				step3LoopStep0.getReport().get(0).getStepId());
		Assert.assertEquals(report3P2.getReport().getParentStepId(),
				step3LoopStep0.getReport().get(1).getStepId());
	}

	//TODO Test Threads
	public void threads() throws Exception {
		// load test case 1
		final String testCaseName = "Havis.RfidTestSuite.Testautomat.testcases.StepsThreads";
		String testCaseHome = testCasesDir + "/StepsThreads";
		String content = new IO(testAutomatDTO.getObjContext())
				.loadResource(testCaseHome + "/" + testCasesDescriptorFileName);
		TestCaseType testCase1 = (TestCaseType) new XMLMessage(content,
				TestCaseType.class).getDeserializedObject();
		TestCaseReportType tcrt = new TestCaseReportType();
		tcrt.setName(testCaseName);
		EntryType entryType = new EntryType();
		entryType.setName("testCaseId");
		entryType.setValue("huhu");
		tcrt.setParameters(new ParametersType());
		tcrt.getParameters().getParameter().add(entryType);
		tcrt.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar()));
		testCase1.setReport(tcrt);
		// create a reporter
		Reporter reporter1 = new Reporter();
		reporter1.initialize();
		// execute steps
		// 1 final report expected from imported test case "StepsThreads.Import"
		TestCaseDTO tcDTO = new TestCaseDTO();
		tcDTO.setId("huhu");
		tcDTO.setBase(testCasesDTO);
		tcDTO.setName(testCaseName);
		tcDTO.setHome(testCaseHome);
		tcDTO.setTestCase(testCase1);
		List<havis.test.suite.api.Reporter> reporters = new ArrayList<havis.test.suite.api.Reporter>();
		reporters.add(reporter1);
		Steps steps = new Steps(testAutomatDTO, tcDTO, testCase1.getSteps()
				.getStep(), null);
		steps.setReporters(reporters);
		steps.run();
		// 1 report list: StepsThreads.Import
		Assert.assertEquals(reporter1.getReports().size(), 1);
		List<String> reports2 = reporter1.getReports().get(
				"StepsThreads.Import");

		// 1 report in list StepsThreads.Import
		Assert.assertEquals(reports2.size(), 1);
		// check report
		TestCaseType report2 = (TestCaseType) new XMLMessage(reports2.get(0),
				TestCaseType.class).getDeserializedObject();
		List<ThreadGroupType> step0ThreadGroups = report2.getSteps().getStep()
				.get(0).getThreads().getThreadGroup();

		// 1. thread group has not been executed => no report
		ThreadGroupType step0ThreadGroup0 = step0ThreadGroups.get(0);
		Assert.assertNull(step0ThreadGroup0.getSteps().getStep().get(0)
				.getReports());

		// content of 2. thread group is executed once => 1 report
		ThreadGroupType step0ThreadGroup1 = step0ThreadGroups.get(1);
		ModuleReportsType step0ThreadGroup1Reports = step0ThreadGroup1
				.getSteps().getStep().get(0).getModule().getReports();
		Assert.assertEquals(step0ThreadGroup1Reports.getReport().size(), 1);
		// pass no
		for (int i = 0; i < step0ThreadGroup1Reports.getReport().size(); i++) {
			Assert.assertEquals(step0ThreadGroup1Reports.getReport().get(i)
					.getPassNo(), i);
		}

		// content of 3. thread group is executed twice => 2 reports
		ThreadGroupType step0ThreadGroup2 = step0ThreadGroups.get(2);
		ModuleReportsType step0ThreadGroup2Reports = step0ThreadGroup2
				.getSteps().getStep().get(0).getModule().getReports();
		Assert.assertEquals(step0ThreadGroup2Reports.getReport().size(), 2);
		// pass no
		for (int i = 0; i < step0ThreadGroup2Reports.getReport().size(); i++) {
			Assert.assertEquals(step0ThreadGroup2Reports.getReport().get(i)
					.getPassNo(), i);
		}

		// content of 4. thread group is executed several times:
		// 1. step: twice
		// 2. step: step in nested thread group: 6 times
		ThreadGroupType step0ThreadGroup3 = step0ThreadGroups.get(2);
		ModuleReportsType step0ThreadGroup3Step0Reports = step0ThreadGroup3
				.getSteps().getStep().get(0).getModule().getReports();
		Assert.assertEquals(step0ThreadGroup3Step0Reports.getReport()
				.size(), 2);
		// pass no
		for (int i = 0; i < step0ThreadGroup3Step0Reports.getReport()
				.size(); i++) {
			Assert.assertEquals(step0ThreadGroup3Step0Reports.getReport()
					.get(i).getPassNo(), i);
		}
		// timestamps
		long duration0 = step0ThreadGroup3Step0Reports.getReport().get(0)
				.getEndTime().toGregorianCalendar().getTimeInMillis()
				- step0ThreadGroup3Step0Reports.getReport().get(0)
						.getStartTime().toGregorianCalendar().getTimeInMillis();
		Assert.assertTrue(duration0 >= 0);

		long interval = step0ThreadGroup3Step0Reports.getReport().get(1)
				.getStartTime().toGregorianCalendar().getTimeInMillis()
				- step0ThreadGroup3Step0Reports.getReport().get(0)
						.getStartTime().toGregorianCalendar().getTimeInMillis();
		Assert.assertTrue(interval < 50);
		// step in nested thread group
		ThreadsType step0ThreadGroup3Step1Threads = step0ThreadGroup3
				.getSteps().getStep().get(1).getThreads();
		ModuleReportsType step0ThreadGroup3Step1Reports = step0ThreadGroup3Step1Threads
				.getThreadGroup().get(0).getSteps().getStep().get(0)
				.getModule().getReports();
		Assert.assertEquals(step0ThreadGroup3Step1Reports.getReport()
				.size(), 6);
		// pass no
		for (int i = 0; i < step0ThreadGroup3Step1Reports.getReport()
				.size(); i++) {
			Assert.assertEquals(step0ThreadGroup3Step1Reports.getReport()
					.get(i).getPassNo(), i);
		}

	}

}
