package havis.test.suite;

import havis.test.suite.common.messaging.XMLMessage;
import havis.test.suite.common.messaging.XQuery;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import havis.test.suite.api.NDIContext;

public class Reporter implements havis.test.suite.api.Reporter {

	private Map<String, List<String>> reports;
	private final Object lockObject = new Object();

	public class Result {
		private String testCaseName;

		public String getTestCaseName() {
			return testCaseName;
		}

		public void setTestCaseName(String testCaseName) {
			this.testCaseName = testCaseName;
		}

	}

	public Map<String, List<String>> getReports() {
		return reports;
	}

	public Reporter() {
		reports = new HashMap<String, List<String>>();
	}

	public void initialize() throws Exception {
		cleanup();
	}

	@Override
	public void start(NDIContext context, String moduleHome, String outputDir)
			throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void stop() throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void cleanup() throws Exception {
		reports.clear();
	}

	@Override
	public void report(String report) throws Exception {
		XQuery xq = new XQuery(
				"<result>"
						+ "<testCaseName>{ /*:testCase/*:report/data(@name) }</testCaseName>"
						+ "</result>");

		String filteredReport = xq.execute(report, new URI(
				"http://www.HARTING.com"));
		XMLMessage data = new XMLMessage(filteredReport);

		Document dom = data.getDomDocument();
		Result rep = new Result();
		rep.setTestCaseName(dom.getElementsByTagName("testCaseName").item(0)
				.getTextContent());
		
		synchronized (lockObject) {
			List<String> testCases = reports.get(rep.getTestCaseName());
			if (!reports.containsKey(rep.getTestCaseName())) {
				testCases = new ArrayList<String>();
				reports.put(rep.getTestCaseName(), testCases);
			}
			testCases.add(report);	
		}
	}
}
