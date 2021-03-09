package havis.test.suite;

import havis.test.suite.api.Reporter;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCaseDTO;
import havis.test.suite.testcase.ImportType;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Import {
	private static final Logger log = LoggerFactory.getLogger(Import.class);
	private final TestAutomatDTO testAutomatDTO;
	private final TestCaseDTO testCaseDTO;
	private final String stepId;
	private final ImportType importType;
	private final String logBasePath;
	public List<Reporter> reporters;

	public List<Reporter> getReporters() {
		return reporters;
	}

	public void setReporters(List<Reporter> reporters) {
		this.reporters = reporters;
	}

	/**
	 * 
	 * @param testAutomatDTO
	 * @param testCaseDTO
	 * @param stepId
	 * @param importType
	 * @param logBasePath
	 *            base path to the import step (used for logging)
	 */
	public Import(TestAutomatDTO testAutomatDTO, TestCaseDTO testCaseDTO,
			String stepId, ImportType importType, String logBasePath) {
		this.testAutomatDTO = testAutomatDTO;
		this.testCaseDTO = testCaseDTO;
		this.stepId = stepId;
		this.importType = importType;
		this.logBasePath = logBasePath;
	}

	/**
	 * Runs the imported test case
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {
		String testCasePath = importType.getTestCaseURI();

		String fullTestCasePath = testCaseDTO.getHome() + File.separator
				+ testCasePath;
		log.info("Importing test case " + fullTestCasePath);

		TestCase importTestCase = null;

		if (importType.getTestCaseParameters() != null) {
			importTestCase = new TestCase(testAutomatDTO,
					testCaseDTO.getBase(), fullTestCasePath,
					new XMLTypeConverter().convert(importType
							.getTestCaseParameters().getParameter(),
							testAutomatDTO.getGlobalContext()), logBasePath);
		} else {
			importTestCase = new TestCase(testAutomatDTO,
					testCaseDTO.getBase(), fullTestCasePath, null, logBasePath);

		}
		importTestCase.setParentStepId(stepId);
		importTestCase.setReporters(reporters);
		importTestCase.run();
	}

}
