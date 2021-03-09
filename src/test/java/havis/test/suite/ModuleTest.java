package havis.test.suite;

import havis.test.suite.api.NDIContext;
import havis.test.suite.common.IO;
import havis.test.suite.common.helpers.PathResolverFileHelper;
import havis.test.suite.common.messaging.XMLMessage;
import havis.test.suite.common.ndi.MapNDIProvider;
import havis.test.suite.common.ndi.SynchronizedNDIContext;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCaseDTO;
import havis.test.suite.exceptions.ReportedException;
import havis.test.suite.exceptions.VerificationException;
import havis.test.suite.testcase.ModuleReportType;
import havis.test.suite.testcase.ModuleType;
import havis.test.suite.testcase.ReportVerificationType;
import havis.test.suite.testcase.StepType;
import havis.test.suite.testcase.TestCaseType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ModuleTest {

	private final String objectsFile;
	private TestAutomatDTO testAutomatDTO;

	public ModuleTest() throws IOException, URISyntaxException {
		objectsFile = PathResolverFileHelper
				.getAbsolutePathFromResource(
						"test/Havis/RfidTestSuite/Testautomat/ModuleTest/beans.xml")
				.get(0).toString();
	}

	@BeforeClass
	public void setUp() {
		// initialize spring framework
		GenericApplicationContext objContext = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(objContext);
		reader.loadBeanDefinitions("file:///" + objectsFile);
		objContext.refresh();
		NDIContext context = new SynchronizedNDIContext();
		MapNDIProvider provider = new MapNDIProvider();
		context.setProvider(provider);
		Map<String, String> map = new HashMap<String, String>();
		map.put("o1", objectsFile);
		UI ui = new UI();
		// create test automat DTO
		testAutomatDTO = new TestAutomatDTO();
		testAutomatDTO.setGlobalContext(context);
		testAutomatDTO.setObjContext(objContext);
		testAutomatDTO.setObjectIdFiles(map);
		testAutomatDTO.setUi(ui);
		testAutomatDTO.getGlobalContext().setValue("ccc", "/x", "v2");
	}

	@Test
	public void run() throws Throwable {
		// load test case
		final String testCaseName = "Module";
		String testCaseHome = "test/Havis/RfidTestSuite/Testautomat/testcases/Module/";
		String content = new IO(testAutomatDTO.getObjContext())
				.loadResource(testCaseHome + "testCase.xml");
		TestCaseType testCase = (TestCaseType) new XMLMessage(content,
				TestCaseType.class).getDeserializedObject();
		TestCaseDTO testCaseDTO = new TestCaseDTO();
		testCaseDTO.setId("testCaseID");
		testCaseDTO.setName(testCaseName);
		testCaseDTO.setHome(testCaseHome);

		int stepNo = 0;

		// 1. step: only a result expected
		StepType step = testCase.getSteps().getStep().get(stepNo++);
		ModuleType stepModule = step.getModule();
		Module module = new Module(testAutomatDTO, testCaseDTO, "stepId",
				stepModule);
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(new DateTime().toGregorianCalendar());
		module.run(0);
		XMLGregorianCalendar endTime = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(new DateTime().toGregorianCalendar());
		Assert.assertEquals(stepModule.getReports().getReport().size(), 1);
		ModuleReportType moduleReport = stepModule.getReports().getReport()
				.get(0);
		// start and end time must be valid
		// TODO StartTime
		Assert.assertTrue(startTime.compare(moduleReport.getStartTime()) <= 0);
		Assert.assertTrue(endTime.compare(moduleReport.getEndTime()) >= 0);
		Assert.assertTrue(moduleReport.getStartTime().compare(
				moduleReport.getEndTime()) <= 0);
		// source properties are not expected
		Assert.assertNull(moduleReport.getSourceProperties());
		// result must be reported
		Assert.assertFalse(moduleReport.getResult().isIsException());
		Assert.assertEquals(moduleReport.getResult().getValue(),
				"<anyResult />");
		// verifications do not exist
		Assert.assertNull(moduleReport.getVerifications());

		// 2. step: source properties, result and verification report expected
		step = testCase.getSteps().getStep().get(stepNo++);
		stepModule = step.getModule();
		module = new Module(testAutomatDTO, testCaseDTO, "stepId", stepModule);
		module.run(0);
		Assert.assertEquals(stepModule.getReports().getReport().size(), 1);
		moduleReport = stepModule.getReports().getReport().get(0);
		// source properties
		Assert.assertEquals(moduleReport.getSourceProperties().getProperty()
				.size(), 2);
		if (moduleReport.getSourceProperties().getProperty().get(0).getName()
				.equals("p1")) {
			Assert.assertEquals(moduleReport.getSourceProperties()
					.getProperty().get(0).getValue(), "v1");
			Assert.assertEquals(moduleReport.getSourceProperties()
					.getProperty().get(1).getName(), "p2");
			Assert.assertEquals(moduleReport.getSourceProperties()
					.getProperty().get(1).getValue(), "v2");
		} else {
			Assert.assertEquals(moduleReport.getSourceProperties()
					.getProperty().get(0).getName(), "p2");
			Assert.assertEquals(moduleReport.getSourceProperties()
					.getProperty().get(0).getValue(), "v2");
			Assert.assertEquals(moduleReport.getSourceProperties()
					.getProperty().get(1).getName(), "p1");
			Assert.assertEquals(moduleReport.getSourceProperties()
					.getProperty().get(1).getValue(), "v1");
		}
		// result
		Assert.assertFalse(moduleReport.getResult().isIsException());
		Assert.assertEquals(moduleReport.getResult().getValue(),
				"<anyResult />");
		// verification report
		ReportVerificationType verification = moduleReport.getVerifications()
				.getVerification().get(0);
		Assert.assertEquals(
				verification.getActual(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<anyResult/>"
						+ System.getProperty("line.separator"));
		Assert.assertEquals(
				verification.getExpected(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<anyResult/>"
						+ System.getProperty("line.separator"));
		Assert.assertNull(verification.getDiff());

		// 3. step: verification error expected
		Step obj = (Step) testAutomatDTO.getObjContext().getBean("o1");
		obj.initialize();
		step = testCase.getSteps().getStep().get(stepNo++);
		stepModule = step.getModule();
		module = new Module(testAutomatDTO, testCaseDTO, "stepId", stepModule);
		try {
			module.run(0);
			Assert.fail();
		} catch (VerificationException e) {
			// "finish" must be called
			Assert.assertTrue(obj.isFinishedCalled());
			// verification throws the exception
			Assert.assertTrue(e.getMessage().contains(
					"Verification 'ver3' failed"));
			moduleReport = stepModule.getReports().getReport().get(0);
			// start and end time must be reported
			Assert.assertNotNull(moduleReport.getStartTime());
			Assert.assertNotNull(moduleReport.getEndTime());
			// result must be reported
			Assert.assertFalse(moduleReport.getResult().isIsException());
			Assert.assertEquals(moduleReport.getResult().getValue(),
					"<anyResult />");
			// verification results are expected
			verification = moduleReport.getVerifications().getVerification()
					.get(0);
			Assert.assertEquals(
					verification.getActual(),
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
							+ System.getProperty("line.separator")
							+ "<anyResult/>"
							+ System.getProperty("line.separator"));
			Assert.assertEquals(
					verification.getExpected(),
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
							+ System.getProperty("line.separator") + "<oh/>"
							+ System.getProperty("line.separator"));
			String lineSepOffset = "";
			if (System.getProperty("line.separator").length() == 1) {
				lineSepOffset = "e";
			}
			Assert.assertEquals(
					verification.getDiff(),
					"- ..." + lineSepOffset + "ncoding=\"UTF-8\"?>"
							+ System.getProperty("line.separator")
							+ "<anyResult/>"
							+ System.getProperty("line.separator") + "..."
							+ System.getProperty("line.separator") + "+ ..."
							+ lineSepOffset + "ncoding=\"UTF-8\"?>"
							+ System.getProperty("line.separator") + "<oh/>"
							+ System.getProperty("line.separator") + "...");
		}

		// 4. step: exception while loading or preparing object
		obj = (Step) testAutomatDTO.getObjContext().getBean("o1");
		obj.initialize();
		step = testCase.getSteps().getStep().get(stepNo++);
		stepModule = step.getModule();
		module = new Module(testAutomatDTO, testCaseDTO, "stepId", stepModule);
		try {
			module.run(0);
			Assert.fail();
		} catch (VerificationException e) {
			Assert.fail();
		} catch (ReportedException e) {
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("'oh' cannot be found"));
			// "finish" must NOT be called
			Assert.assertFalse(obj.isFinishedCalled());
			moduleReport = stepModule.getReports().getReport().get(0);
			// start and end time do not exist
			Assert.assertNull(moduleReport.getStartTime());
			Assert.assertNull(moduleReport.getEndTime());
			Assert.assertFalse(moduleReport.getResult().isIsException());
			// result is empty
			Assert.assertTrue(moduleReport.getResult().getValue() == null
					|| moduleReport.getResult().getValue().length() == 0);
			// verification results do not exist
			Assert.assertNull(moduleReport.getVerifications());
		}

		// 5. step: exception while execution
		obj = (Step) testAutomatDTO.getObjContext().getBean("o1");
		obj.initialize();
		obj.setThrowRunException(true);
		step = testCase.getSteps().getStep().get(stepNo++);
		stepModule = step.getModule();
		module = new Module(testAutomatDTO, testCaseDTO, "stepId", stepModule);

		try {
			module.run(0);
			Assert.fail();
		} catch (VerificationException e) {
			Assert.fail();
		} catch (ReportedException e) {
			Assert.assertTrue(e.getCause().getMessage().contains("run failed"));
			// "finish" must be called
			Assert.assertTrue(obj.isFinishedCalled());
			moduleReport = stepModule.getReports().getReport().get(0);
			// start and end time must be reported
			Assert.assertNotNull(moduleReport.getStartTime());
			Assert.assertNotNull(moduleReport.getEndTime());
			// result exception must be reported
			Assert.assertTrue(moduleReport.getResult().isIsException());
			Assert.assertTrue(moduleReport.getResult().getValue()
					.contains("run failed")
					&& moduleReport.getResult().getValue()
							.contains("Exception"));
			// verification results do not exist
			Assert.assertNull(moduleReport.getVerifications());
		} catch (Exception e) {
			Assert.fail();
		}

		// 6. step: exception while execution but exception accepted for
		// verification
		obj = (Step) testAutomatDTO.getObjContext().getBean("o1");
		obj.initialize();
		obj.setThrowRunException(true);
		step = testCase.getSteps().getStep().get(stepNo++);
		stepModule = step.getModule();
		module = new Module(testAutomatDTO, testCaseDTO, "stepId", stepModule);
		try {
			module.run(0);
			Assert.fail();
		} catch (VerificationException e) {
			Assert.assertTrue(e.getMessage().contains(
					"Verification 'ver6' failed"));
			// "finish" must be called
			Assert.assertTrue(obj.isFinishedCalled());
			moduleReport = stepModule.getReports().getReport().get(0);
			// start and end time must be reported
			Assert.assertNotNull(moduleReport.getStartTime());
			Assert.assertNotNull(moduleReport.getEndTime());
			// result exception must be reported but not marked as exception
			Assert.assertFalse(moduleReport.getResult().isIsException());
			Assert.assertTrue(moduleReport.getResult().getValue()
					.contains("run failed")
					&& moduleReport.getResult().getValue()
							.contains("Exception"));
			// exception had to be verified
			verification = moduleReport.getVerifications().getVerification()
					.get(0);
			Assert.assertTrue(verification.getActual().contains("run failed")
					&& verification.getActual().contains("stacktrace"));
		} catch (ReportedException e) {
			Assert.fail();
		} catch (Exception e) {
			Assert.fail();
		}

		// 7. step: exception after verification error (finish call)
		// expected: exception and verification report
		obj = (Step) testAutomatDTO.getObjContext().getBean("o1");
		obj.initialize();
		obj.setThrowFinishException(true);
		step = testCase.getSteps().getStep().get(stepNo++);
		stepModule = step.getModule();
		module = new Module(testAutomatDTO, testCaseDTO, "stepId", stepModule);
		try {
			module.run(0);
			Assert.fail();
		} catch (VerificationException e) {
			Assert.fail();
		} catch (ReportedException e) {
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("finish failed"));
			// "finish" must be called and it must fail
			Assert.assertTrue(obj.isFinishedCalled());
			// result must be reported
			moduleReport = stepModule.getReports().getReport().get(0);
			Assert.assertFalse(moduleReport.getResult().isIsException());
			// verification results are expected
			verification = moduleReport.getVerifications().getVerification()
					.get(0);
			Assert.assertEquals(
					verification.getActual(),
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
							+ System.getProperty("line.separator")
							+ "<anyResult/>"
							+ System.getProperty("line.separator") + "");
			Assert.assertEquals(
					verification.getExpected(),
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
							+ System.getProperty("line.separator") + "<oh/>"
							+ System.getProperty("line.separator") + "");

			String lineSepOffset = "";
			if (System.getProperty("line.separator").length() == 1) {
				lineSepOffset = "e";
			}

			Assert.assertEquals(
					verification.getDiff(),
					"- ..." + lineSepOffset + "ncoding=\"UTF-8\"?>"
							+ System.getProperty("line.separator")
							+ "<anyResult/>"
							+ System.getProperty("line.separator") + "..."
							+ System.getProperty("line.separator") + "+ ..."
							+ lineSepOffset + "ncoding=\"UTF-8\"?>"
							+ System.getProperty("line.separator") + "<oh/>"
							+ System.getProperty("line.separator") + "...");
		}

		// 8. step: exception while excecution and "finish" call
		// expected: exception from excecution
		obj = (Step) testAutomatDTO.getObjContext().getBean("o1");
		obj.initialize();
		obj.setThrowRunException(true);
		obj.setThrowFinishException(true);
		step = testCase.getSteps().getStep().get(stepNo++);
		stepModule = step.getModule();
		module = new Module(testAutomatDTO, testCaseDTO, "stepId", stepModule);
		try {
			module.run(0);
			Assert.fail();
		} catch (VerificationException e) {
			Assert.fail();
		} catch (ReportedException e) {
			Assert.assertTrue(e.getCause().getMessage().contains("run failed"));
			// "finish" must be called but the cause for the exception must be
			// the execution
			Assert.assertTrue(obj.isFinishedCalled());
			// result exception must be reported
			moduleReport = stepModule.getReports().getReport().get(0);
			Assert.assertTrue(moduleReport.getResult().isIsException());
			Assert.assertTrue(moduleReport.getResult().getValue()
					.contains("run failed")
					&& moduleReport.getResult().getValue()
							.contains("Exception"));
			// verifications do not exist
			Assert.assertNull(moduleReport.getVerifications());
		} catch (Exception e) {
			Assert.fail();
		}

	}
}
