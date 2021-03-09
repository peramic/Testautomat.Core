package havis.test.suite;

import java.util.List;
import java.util.Map;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.Step;
import havis.test.suite.api.dto.ModulesObjectIds;
import havis.test.suite.api.dto.TestCaseInfo;
import havis.test.suite.api.dto.VerificationReport;

public class UI implements havis.test.suite.api.UI {

	private NDIContext context;

	@Override
	public void start(NDIContext context, String moduleHome,
			List<String> testCasesHome, String[] cliArgs) throws Exception {

		this.context = context;

	}

	@Override
	public void stop() {
	}

	@Override
	public ModulesObjectIds getModulesObjectIds() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> getTestCasesPaths() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> prepareExecute(Step step,
			TestCaseInfo testCaseInfo, String moduleHome, String stepId,
			Map<String, Object> stepProperties) throws Exception {
		return step.prepare(context, moduleHome, testCaseInfo, stepId,
				stepProperties);
	}

	@Override
	public String execute(Step step, Map<String, Object> stepProperties)
			throws Exception {
		return step.run();
	}

	@Override
	public void finishExec(Step step, Map<String, Object> stepProperties,
			List<VerificationReport> verificationReports) throws Exception {
		step.finish();
	}

}
