package havis.test.suite;

import havis.test.suite.api.Step;
import havis.test.suite.api.dto.TestCaseInfo;
import havis.test.suite.api.dto.VerificationReport;
import havis.test.suite.common.messaging.ExceptionSerializer;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCaseDTO;
import havis.test.suite.exceptions.ReportedException;
import havis.test.suite.exceptions.VerificationException;
import havis.test.suite.testcase.EntryType;
import havis.test.suite.testcase.ModuleReportType;
import havis.test.suite.testcase.ModuleReportsType;
import havis.test.suite.testcase.ModuleType;
import havis.test.suite.testcase.PropertiesType;
import havis.test.suite.testcase.ReportVerificationType;
import havis.test.suite.testcase.ReportVerificationsType;
import havis.test.suite.testcase.ResultType;
import havis.test.suite.testcase.VerificationType;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Module {
	private static final Logger log = LoggerFactory.getLogger(Module.class);

	private final TestAutomatDTO testAutomatDTO;
	private final TestCaseDTO testCaseDTO;
	private final String stepId;
	private final ModuleType module;

	public Module(TestAutomatDTO testAutomatDTO, TestCaseDTO testCaseDTO,
			String stepId, ModuleType module) {
		this.testAutomatDTO = testAutomatDTO;
		this.testCaseDTO = testCaseDTO;
		this.stepId = stepId;
		this.module = module;
	}

	/**
	 * Executes a step module and returns the result
	 * 
	 * @param step
	 * @param stepProperties
	 * @return
	 * @throws Exception
	 */
	private String execute(Step step, Map<String, Object> stepProperties)
			throws Exception {
		try {
			// execute the module
			return testAutomatDTO.getUi().execute(step, stepProperties);
		} catch (Throwable t) {
			// if an exception shall be verified
			if (module.getVerifications() != null
					&& module.getVerifications().isVerifyException()) {
				// use the exception as expected result
				return ExceptionSerializer.getExceptionStackXml(t);
			}
			throw t;
		}
	}

	/**
	 * Returns a exception for the first validation that failed
	 * 
	 * @param verifications
	 * @return null, if no validation error exists
	 */
	private static VerificationException createVerificationException(
			List<ReportVerificationType> verifications) {
		if (verifications != null) {
			for (ReportVerificationType verification : verifications) {
				if (verification.getDiff() != null
						&& verification.getDiff().length() != 0) {
					return new VerificationException("Verification '"
							+ verification.getName() + "' failed");
				}
			}
		}
		return null;
	}

	/**
	 * Runs the module. A runtime exception and all verification errors are
	 * added to the report
	 * 
	 * @param passNo
	 * @throws Throwable
	 *             The first verification error or a runtime exception
	 */
	public void run(int passNo) throws Throwable {
		Map<String, Object> sourceProperties = new HashMap<String, Object>();
		XMLGregorianCalendar startTime = null;
		XMLGregorianCalendar endTime = null;

		String result = "";
		List<ReportVerificationType> verificationReports = new ArrayList<ReportVerificationType>();
		List<Throwable> exceptions = new ArrayList<>();
		Throwable resultException = null;

		try {
			// load object
			String objectId = module.getObject().getObjectId();
			String fileName = null;

			if (!testAutomatDTO.getObjectIdFiles().containsKey(objectId)) {
				throw new Exception("The step module with objectId '"
						+ objectId + "' cannot be found");
			}

			fileName = testAutomatDTO.getObjectIdFiles().get(objectId);
			String moduleHome = Paths.get(fileName).getParent().toString();
			Step step = (Step) testAutomatDTO.getObjContext().getBean(objectId);

			Map<String, Object> stepProperties = new HashMap<String, Object>();
			// prepare step and get source properties
			if (module.getObject().getProperties() != null) {
				stepProperties = new XMLTypeConverter().convert(module
						.getObject().getProperties().getProperty(),
						testAutomatDTO.getGlobalContext());
			}
			TestCaseInfo tci = new TestCaseInfo();
			tci.setHome(testCaseDTO.getHome());
			tci.setId(testCaseDTO.getId());
			tci.setName(testCaseDTO.getName());
			sourceProperties = testAutomatDTO.getUi().prepareExecute(step, tci,
					moduleHome, stepId, stepProperties);

			try {
				// execute step module
				startTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar());
				result = execute(step, stepProperties);
				endTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar());;
				try {
					// verify result
					Verifier verifier = new Verifier(
							testAutomatDTO.getObjContext(),
							testCaseDTO.getHome());
					List<VerificationType> lst = new ArrayList<VerificationType>();
					if (module.getVerifications() != null) {
						lst = module.getVerifications().getVerification();
					}
					verificationReports = verifier.verify(result, lst,
							testAutomatDTO.getGlobalContext());
				} // runtime exception while verification
				catch (Throwable t) {
					exceptions.add(t);
				}
			} // result exception while execution
			catch (Throwable t) {
				endTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime().toGregorianCalendar());
				resultException = t;
			}
			// finish execution
			List<VerificationReport> reports;
			if (module.getVerifications() == null) {
				reports = new XMLTypeConverter().convert(verificationReports,
						new ArrayList<VerificationType>());
			} else {
				reports = new XMLTypeConverter().convert(verificationReports,
						module.getVerifications().getVerification());
			}

			testAutomatDTO.getUi().finishExec(step, stepProperties, reports);

		} catch (Throwable t)// preparation or finishing failed
		{
			exceptions.add(t);
		}

		// add report data to test case
		List<EntryType> reportSourceProperties = new XMLTypeConverter()
				.convert(sourceProperties);
		if (resultException != null) {
			// serialize result exception
			result = ExceptionSerializer.getExceptionStackXml(resultException);
		}

		if (module.getReports() == null) {
			module.setReports(new ModuleReportsType());
		}

		List<ModuleReportType> modLst = module.getReports().getReport();
		ModuleReportType modRep = new ModuleReportType();

		if (startTime != null) {
			modRep.setStartTime(startTime);
		} else {
			modRep.setStartTime(null);
		}
		if (endTime != null) {
			modRep.setEndTime(endTime);
		} else {
			modRep.setEndTime(null);
		}
		if (reportSourceProperties.size() != 0) {
			PropertiesType props = new PropertiesType();
			props.getProperty().clear();
			props.getProperty().addAll(reportSourceProperties);
			modRep.setSourceProperties(props);
		} else {
			modRep.setSourceProperties(null);
		}

		if (result != null) {
			ResultType resType = new ResultType();
			if (resultException != null) {
				resType.setIsException(true);
			} else {
				resType.setIsException(false);
			}
			resType.setValue(result);
			modRep.setResult(resType);
		}

		if (verificationReports.size() == 0) {
			modRep.setVerifications(null);
		} else {
			ReportVerificationsType repVerType = new ReportVerificationsType();
			repVerType.getVerification().clear();
			repVerType.getVerification().addAll(verificationReports);
			modRep.setVerifications(repVerType);
		}
		modRep.setPassNo(passNo);
		modLst.add(modRep);

		// exceptions have a higher priority than verification errors => handle
		// them first
		// if the result of the excecution is an exception
		if (resultException != null) {
			for (Throwable t : exceptions) {
				// log exceptions occurred after execution
				log.warn(
						"Finishing of execution of step module with objectId '"
								+ module.getObject().getObjectId() + "' failed",
						t);
			}
			// throw result exception as reported exception
			throw new ReportedException(
					"Execution of step module with objectId '"
							+ module.getObject().getObjectId() + "' failed",
					resultException);
		}

		if (exceptions.size() > 0) {
			// throw first exception
			throw exceptions.get(0);
		}
		// create exception from first verification error
		Exception verificationException = createVerificationException(verificationReports);
		if (verificationException != null) {
			throw verificationException;
		}

	}
}
