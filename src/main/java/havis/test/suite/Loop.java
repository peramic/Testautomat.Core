package havis.test.suite;

import havis.test.suite.api.Reporter;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCaseDTO;
import havis.test.suite.testcase.LoopType;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loop {

	private static final Logger log = LoggerFactory.getLogger(Loop.class);
	private final TestAutomatDTO testAutomatDTO;
	private final TestCaseDTO testCaseDTO;
	private final LoopType loop;
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
	 * @param loop
	 * @param logBasePath base path to the step list (used for logging)
	 */
	public Loop(TestAutomatDTO testAutomatDTO, TestCaseDTO testCaseDTO, LoopType loop, String logBasePath)
	{
		this.testAutomatDTO=testAutomatDTO;
		this.testCaseDTO=testCaseDTO;
		this.loop = loop;
		this.logBasePath = logBasePath;
		reporters = new ArrayList<Reporter>();
	}
	
	/**
	 * Runs the loop
	 * @throws Exception
	 */
	public void run() throws Exception
	{
		log.info("Executing loop (count="+ loop.getCount() +")");
		Steps loopSteps = new Steps(testAutomatDTO, testCaseDTO, loop.getSteps().getStep(), logBasePath);
		loopSteps.setReporters(reporters);
		for (int i=0; i<loop.getCount(); i++)
		{
			loopSteps.run();
		}	
	}
}
