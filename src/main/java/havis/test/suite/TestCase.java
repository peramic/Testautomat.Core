package havis.test.suite;

import havis.test.suite.api.Reporter;
import havis.test.suite.common.IO;
import havis.test.suite.common.XmlValidationException;
import havis.test.suite.common.messaging.XMLMessage;
import havis.test.suite.common.messaging.XSD;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCaseDTO;
import havis.test.suite.dto.TestCasesDTO;
import havis.test.suite.testcase.EntryType;
import havis.test.suite.testcase.ParametersType;
import havis.test.suite.testcase.TestCaseReportType;
import havis.test.suite.testcase.TestCaseType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.datatype.DatatypeFactory;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

public class TestCase {
	private static final Logger log = LoggerFactory.getLogger(TestCase.class);
	private String parentStepId;

	public String getParentStepId() {
		return parentStepId;
	}

	public void setParentStepId(String parentStepId) {
		this.parentStepId = parentStepId;
	}

	public List<Reporter> getReporters() {
		return reporters;
	}

	public void setReporters(List<Reporter> reporters) {
		this.reporters = reporters;
	}

	private List<Reporter> reporters;
	private final TestAutomatDTO testAutomatDTO;
	private final TestCaseDTO testCaseDTO;
	private final TestCaseType testCase;
	private final String logBasePath;

	/**
	 * 
	 * @param testAutomatDTO
	 * @param testCasesDTO
	 * @param testCaseHome
	 *            The full path to the test case directory
	 * @param testCaseParameters
	 * @param logBasePath
	 *            base path to the test case (used for logging)
	 * @throws Exception
	 */
	public TestCase(TestAutomatDTO testAutomatDTO, TestCasesDTO testCasesDTO,
			String testCaseHome, Map<String, Object> testCaseParameters,
			String logBasePath) throws Exception {
		this.testAutomatDTO = testAutomatDTO;
		this.logBasePath = logBasePath;
		reporters = new ArrayList<Reporter>();
		// generate testCaseId
		String testCaseId = UUID.randomUUID().toString().replace("-", "");
		if (testCaseParameters == null) {
			testCaseParameters = new HashMap<>();
		}
		testCaseParameters.put("testCaseId", testCaseId);
		// load test case
		String file = testCaseHome.replace(File.separator, "/") + "/" +  testCasesDTO.getDescriptorFileName();
		IO io = new IO(testAutomatDTO.getObjContext());
		String content = io.loadResource(file);
		// apply template
		content = applyTestCaseTemplate(content, testCasesDTO.getParameters(),
				testCaseParameters);
		if (testCasesDTO.getXsd() != null) {
			// validate the XML content
			XmlValidationException ve = testCasesDTO.getXsd().validate(content);
			if (ve != null) {
				throw ve;
			}
		}
		// deserialize test case
		testCase = (TestCaseType) new XMLMessage(content, TestCaseType.class)
				.getDeserializedObject();
		// create test case name from path:
		// 1. get rel. path to the directory of all test cases
		// 2. replace directory separator with '.'
		String relTestCasePath = testCaseHome.replace(File.separator, "/").substring(testCasesDTO.getHome()
				.length() + 1);;
		String testCaseName = relTestCasePath.replaceAll("/", ".");
		// create DTO
		testCaseDTO = new TestCaseDTO();
		testCaseDTO.setId(testCaseId);
		testCaseDTO.setBase(testCasesDTO);
		testCaseDTO.setName(testCaseName);
		testCaseDTO.setHome(testCaseHome);
		testCaseDTO.setTestCase(testCase);
		testCaseDTO.setParameters(testCaseParameters);
	}

	/**
	 * Runs the test case
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {
		boolean areStepsExecuted = false;
		try {
			// add existing report data to test case
			TestCaseReportType report = new TestCaseReportType();
			report.setName(testCaseDTO.getName());
			report.setParameters(new ParametersType());

			HashMap<String, Object> allParameters = getAllParameters(
					testCaseDTO.getBase().getParameters(),
					testCaseDTO.getParameters());

			XMLTypeConverter converter = new XMLTypeConverter();
			List<EntryType> entries = converter.convert(allParameters);
			
			report.getParameters().getParameter().clear();
			report.getParameters().getParameter().addAll(entries);
				
//			for (Entry<String, Object> parameter : allParameters.entrySet()) {
//				EntryType entryType = new EntryType();
//				entryType.setName(parameter.getKey());
//				if (parameter.getValue() != null) {
//					if (parameter.getValue().toString().contains("\n")
//							|| parameter.getValue().toString().contains("\r")) {
//						entryType.setChoice(new Choice());
//						entryType.getChoice().setList(new ListType());
//						entryType.getChoice().getList().getValueList()
//								.add(parameter.getValue().toString());
//					} else {
//						entryType.setValue(parameter.getValue().toString());
//					}
//				}
//				report.getParameters().getParameterList().add(entryType);
//			}

			report.setParentStepId(parentStepId);
			report.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar()));
			testCase.setReport(report);
			String logPath = (logBasePath == null ? "" : logBasePath + "/")
					+ testCaseDTO.getName();
			// execute step list
			Steps steps = new Steps(testAutomatDTO, testCaseDTO, testCase
					.getSteps().getStep(), logPath);
			steps.setReporters(reporters);
			steps.run();
			areStepsExecuted = true;
		} catch (Exception e) {
			log.warn("Exception skipped", e);
		} finally {
			// add further report data to test case
			testCase.getReport().setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar()));
			try {
				// serialize and validate the test case
				String testCaseStr = new XMLMessage(testCase)
						.getSerializationString("testCase", "http://www.HARTING.com/RFID/TestAutomat");
				XSD xsd = testCaseDTO.getBase().getXsd();
				if (xsd != null) {
					XmlValidationException ve = xsd.validate(testCaseStr);

					if (ve != null) {
						throw ve;
					}

				}
				// for each reporter
				for (Reporter reporter : reporters) {
					// send the test case as report
					reporter.report(testCaseStr);
				}
			} catch (Exception e) {
				// if only the sending of reports failed
				if (areStepsExecuted) {
					// throw the "sending" exception
					throw e;
				}
				// first exception has higher priority then the "sending"
				// exception
				// => write the "sending" exception to log
				log.warn("Exception skipped", e);
			}
		}
	}

	/**
	 * Applies a template. In addition to the given parameters the parameter
	 * testCaseId is set
	 * 
	 * @param content
	 * @param testCaseId
	 * @param testCasesParameters
	 *            parameters for all test cases
	 * @param testCaseParameters
	 *            parameters for only this test case
	 * @return
	 */
	private String applyTestCaseTemplate(String content,
			Map<String, Object> testCasesParameters,
			Map<String, Object> testCaseParameters) {
		HashMap<String, Object> allParameters = getAllParameters(
				testCasesParameters, testCaseParameters);
		// add all parameters to template and return the rendered template
		ST template = new ST(content, '$', '$');
		for (Map.Entry<String, Object> parameter : allParameters.entrySet()) {
			template.add(parameter.getKey(), parameter.getValue());
		}
		return template.render();
	}

	private HashMap<String, Object> getAllParameters(
			Map<String, Object> testCasesParameters,
			Map<String, Object> testCaseParameters) {
		HashMap<String, Object> allParameters = new HashMap<String, Object>();
		allParameters.put("testCaseId", testCaseParameters.get("testCaseId"));
		if (testCasesParameters != null) {
			// add parameters for all test cases to template
			for (Map.Entry<String, Object> parameter : testCasesParameters
					.entrySet()) {
				allParameters.remove(parameter.getKey());
				allParameters.put(parameter.getKey(), parameter.getValue());
			}
		}
		if (testCaseParameters != null) {
			// add parameters to template
			for (Map.Entry<String, Object> parameter : testCaseParameters
					.entrySet()) {
				if (!parameter.getKey().equals("testCaseId")) {
					allParameters.remove(parameter.getKey());
					allParameters.put(parameter.getKey(), parameter.getValue());
				}
			}
		}
		return allParameters;
	}

}
