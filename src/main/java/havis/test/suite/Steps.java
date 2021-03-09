package havis.test.suite;

import havis.test.suite.api.Reporter;
import havis.test.suite.common.messaging.ExceptionSerializer;
import havis.test.suite.common.messaging.XMLMessage;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCaseDTO;
import havis.test.suite.exceptions.ReportedException;
import havis.test.suite.exceptions.VerificationException;
import havis.test.suite.testcase.ModuleType;
import havis.test.suite.testcase.StepReportType;
import havis.test.suite.testcase.StepReportsType;
import havis.test.suite.testcase.StepType;
import havis.test.suite.testcase.ThreadsType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Steps {
	private static final Logger log = LoggerFactory.getLogger(Steps.class);
	private final TestAutomatDTO testAutomatDTO;
	private final TestCaseDTO testCaseDTO;
	private final List<StepType> steps;
	private final String logBasePath;
	private List<Reporter> reporters;

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
	 * @param steps
	 * @param logBasePath
	 *            base path to the step list (used for logging)
	 */
	public Steps(TestAutomatDTO testAutomatDTO, TestCaseDTO testCaseDTO,
			List<StepType> steps, String logBasePath) {

		this.testAutomatDTO = testAutomatDTO;
		this.testCaseDTO = testCaseDTO;
		this.steps = steps;
		this.logBasePath = logBasePath;
		reporters = new ArrayList<Reporter>();
	}

	/**
	 * Runs the step
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {
		// get new pass no
		int passNo = getMaxPassNo(steps) + 1;
		// for each step
		for (StepType step : steps) {
			String stepId = UUID.randomUUID().toString().replace("-", "");
			String logPath = logBasePath + "->" + step.getName();
			// add report data to step
			if (step.getReports() == null) {
				step.setReports(new StepReportsType());
			}
			List<StepReportType> stepReports = step.getReports().getReport();
			StepReportType stepReport = new StepReportType();
			stepReport.setPassNo(passNo);
			stepReport.setStepId(stepId);
			stepReports.add(stepReport);

			try {
				log.info("Executing step " + logPath);
				if (step.getModule() != null) {
					Module module = new Module(testAutomatDTO, testCaseDTO,
							stepId, step.getModule());
					module.run(passNo);
				} else if (step.getImport() != null) {
					Import importStep = new Import(testAutomatDTO, testCaseDTO,
							stepId, step.getImport(), logPath);
					importStep.setReporters(reporters);
					importStep.run();
				} else if (step.getLoop() != null) {
					Loop loop = new Loop(testAutomatDTO, testCaseDTO,
							step.getLoop(), logPath);
					loop.setReporters(reporters);
					loop.run();
				} else if (step.getThreads() != null) {
					ThreadsType threads = step.getThreads();
					ThreadGroups threadGroups = new ThreadGroups(
							testAutomatDTO, testCaseDTO,
							threads.getThreadGroup(), logPath);
					threadGroups.setReporters(reporters);
					threadGroups.run();
				}
			} catch (VerificationException e) {
				if (step.isThrowVerificationErrors()) {
					// forward verification exception to parent
					throw e;
				}
				log.warn("Verification error skipped", e);
				// send intermediate report
				sendIntermediateReport();
			} catch (ReportedException e) {
				if (step.isThrowExceptions()) {
					// forward reported exception to parent
					throw e;
				}
				log.warn("Exception skipped", e);
				// send intermediate report
				sendIntermediateReport();

			} catch (Throwable t) {
				// add unreported exception to step report
				int lastReport = step.getReports().getReport().size() - 1;
				step.getReports()
						.getReport()
						.get(lastReport)
						.setException(
								ExceptionSerializer.getExceptionStackXml(t));
				if (step.isThrowExceptions()) {
					// forward exception as reported exception to parent
					throw new ReportedException("Execution of step " + stepId
							+ " failed", t);
				}
				log.warn("Exception skipped", t);
				// send intermediate report
				sendIntermediateReport();
			}
		}
	}

	/**
	 * Gets the max. pass number which exists in the list of steps. Embedded
	 * lists are ignored
	 * 
	 * @param steps
	 * @return
	 */
	public static int getMaxPassNo(List<StepType> steps) {
		int ret = -1;
		// for each step
		for (StepType step : steps) {
			if (step.getReports() != null
					&& step.getReports().getReport().size() > 0) {
				int lastReportPassNo = (int) step.getReports().getReport()
						.get(step.getReports().getReport().size() - 1)
						.getPassNo();
				if (lastReportPassNo > ret) {
					ret = lastReportPassNo;
				}
			} else if (step.getModule() != null) { // if module
				ModuleType m = step.getModule();
				if (m.getReports() != null
						&& m.getReports().getReport().size() > 0) {
					int lastReportPassNo = (int) m.getReports().getReport()
							.get(m.getReports().getReport().size() - 1)
							.getPassNo();
					if (lastReportPassNo > ret) {
						ret = lastReportPassNo;
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Sends intermediate reports to all reporters.
	 * 
	 * @throws Exception
	 */
	private void sendIntermediateReport() throws Exception {
		// create report
		String report = new XMLMessage(testCaseDTO.getTestCase())
				.getSerializationString("testCase", "http://www.HARTING.com/RFID/TestAutomat");
		// for each reporter
		for (Reporter reporter : reporters) {
			// send report
			reporter.report(report);
		}
	}

}
