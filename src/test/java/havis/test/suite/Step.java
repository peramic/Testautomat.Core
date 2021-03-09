package havis.test.suite;

import java.util.Map;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.dto.TestCaseInfo;

public class Step implements havis.test.suite.api.Step {

	private boolean throwPrepareException;
	private boolean throwRunException;
	private boolean throwFinishException;
	private String stepId;
	private Map<String, Object> stepProperties;

	private String moduleHome=null;
	private TestCaseInfo testCaseInfo;
	private boolean isPreparedCalled;
	private boolean isRunCalled;
	private boolean isFinishedCalled;

	public boolean isThrowPrepareException() {
		return throwPrepareException;
	}

	public void setThrowPrepareException(boolean throwPrepareException) {
		this.throwPrepareException = throwPrepareException;
	}

	public boolean isThrowRunException() {
		return throwRunException;
	}

	public void setThrowRunException(boolean throwRunException) {
		this.throwRunException = throwRunException;
	}

	public boolean isThrowFinishException() {
		return throwFinishException;
	}

	public void setThrowFinishException(boolean throwFinishException) {
		this.throwFinishException = throwFinishException;
	}

	public String getStepId() {
		return stepId;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

	public Map<String, Object> getStepProperties() {
		return stepProperties;
	}

	public void setStepProperties(Map<String, Object> stepProperties) {
		this.stepProperties = stepProperties;
	}

	public String getModuleHome() {
		return moduleHome;
	}

	public TestCaseInfo getTestCaseInfo() {
		return testCaseInfo;
	}

	public boolean isPreparedCalled() {
		return isPreparedCalled;
	}

	public boolean isRunCalled() {
		return isRunCalled;
	}

	public boolean isFinishedCalled() {
		return isFinishedCalled;
	}

	public void initialize() {
		throwRunException = false;
		throwFinishException = false;
		isPreparedCalled = false;
		isRunCalled = false;
		isFinishedCalled = false;
	}

	@Override
	public Map<String, Object> prepare(NDIContext context, String moduleHome,
			TestCaseInfo testCaseInfo, String stepId,
			Map<String, Object> stepProperties) throws Exception {

		isPreparedCalled = true;
		if (throwPrepareException) {
			throw new Exception("prepare failed");
		}
		this.moduleHome = moduleHome;
		this.testCaseInfo = testCaseInfo;
		this.stepId = stepId;
		this.stepProperties = stepProperties;

		return stepProperties;
	}

	@Override
	public String run() throws Exception {
		isRunCalled = true;
		if (throwRunException) {
			throw new Exception("run failed");
		}
		return "<anyResult />";
	}

	@Override
	public void finish() throws Exception {
		isFinishedCalled = true;
		if (throwFinishException) {
			throw new Exception("finish failed");
		}
	}

}
