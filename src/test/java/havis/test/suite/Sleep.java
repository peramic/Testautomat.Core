package havis.test.suite;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.dto.TestCaseInfo;

/**
 * The step property duration provides the duration in milliseconds to sleep.
 */
public class Sleep implements havis.test.suite.api.Step {
	private static final Logger log = LoggerFactory.getLogger(Sleep.class);
	private long duration;

	/**
	 * See
	 * {@link Havis.RfidTestSuite.Interfaces.Step#prepare(NDIContext, String, TestCaseInfo, String, Map)}
	 */
	@Override
	public Map<String, Object> prepare(NDIContext context, String moduleHome,
			TestCaseInfo testCaseInfo, String stepId,
			Map<String, Object> stepProperties) throws Exception {
		// getDuration
		if (!stepProperties.containsKey("duration")) {
			throw new ConfigurationException(
					"Step property 'duration' is missed");
		}
		try {
			duration = Long.parseLong((String) stepProperties.get("duration"));
		} catch (Exception e) {
			throw new ConfigurationException(
					"Type of step property 'duration' must be a string with content of type 'long'",
					e);
		}
		return null;
	}


	/**
	 * See
	 * {@link Havis.RfidTestSuite.Interfaces.Step#run()}
	 */
	@Override
	public String run() throws Exception {
		log.info("Sleeping " + duration + "ms");
		Thread.sleep(duration);
		return null;
	}

	/**
	 * See
	 * {@link Havis.RfidTestSuite.Interfaces.Step#finish()}
	 */
	@Override
	public void finish() throws Exception {
	}

}
