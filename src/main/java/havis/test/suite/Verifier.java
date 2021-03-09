package havis.test.suite;

import havis.test.suite.api.NDIContext;
import havis.test.suite.common.Diff;
import havis.test.suite.common.IO;
import havis.test.suite.common.messaging.XMLMessage;
import havis.test.suite.common.messaging.XQuery;
import havis.test.suite.testcase.EntryType;
import havis.test.suite.testcase.ReportVerificationType;
import havis.test.suite.testcase.VerificationType;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.context.ApplicationContext;
import org.stringtemplate.v4.ST;

public class Verifier {

	private String baseDir;
	private static final URI uri = createURI();

	private static URI createURI() {
		try {
			return new URI("http://www.HARTING.com");
		} catch (Exception e) {
			return null;
		}
	}

	private final ApplicationContext context;

	/**
	 * Initializes a verifier
	 * 
	 * @param context
	 * @param baseDir
	 */
	public Verifier(ApplicationContext context, String baseDir) {
		this.context = context;
		this.baseDir = baseDir;
	}

	/**
	 * Loads a file
	 * 
	 * @param fileName
	 *            relative path including filename
	 * @return
	 * @throws IOException
	 */
	private String loadFile(String path) throws IOException {
		IO io = new IO(context);
		return io.loadResource(baseDir + "/" + path);
	}

	/**
	 * Returns content with replaced templates
	 * 
	 * @param content
	 * @param parameters
	 * @param globalContext
	 * @return
	 */
	private static String applyTemplate(String content,
			List<EntryType> parameters, NDIContext globalContext) {
		ST template = new ST(content, '$', '$');
		for (Entry<String, Object> parameter : new XMLTypeConverter().convert(
				parameters, globalContext).entrySet()) {
			template.add(parameter.getKey(), parameter.getValue());
		}
		return template.render();
	}

	/**
	 * Verifies results and returns the verification reports
	 * 
	 * @param actualResult
	 * @param verifications
	 * @param globalContext
	 * @return
	 * @throws Exception
	 */
	public List<ReportVerificationType> verify(String actualResult,
			List<VerificationType> verifications, NDIContext globalContext)
			throws Exception {

		ArrayList<ReportVerificationType> ret = new ArrayList<ReportVerificationType>();

		if (verifications == null) {
			return ret;
		}

		// for each verification
		for (VerificationType verification : verifications) {

			String actualFilter = "root()";
			if (verification.getActual() != null) {
				if (verification.getActual().getResultFilter() != null) {
					actualFilter = verification.getActual().getResultFilter();
				} else {
					actualFilter = loadFile(verification.getActual()
							.getResultFilterURI());

					if (verification.getActual().getResultFilterParameters() != null
							&& verification.getActual()
									.getResultFilterParameters().getParameter()
									.size() != 0) {

						actualFilter = applyTemplate(actualFilter, verification
								.getActual().getResultFilterParameters()
								.getParameter(), globalContext);
					}

				}
			}

			String expectedResult = null;
			if (verification.getExpected() != null) {
				if (verification.getExpected().getResult() != null) {
					expectedResult = verification.getExpected().getResult();
				} else {
					expectedResult = loadFile(verification.getExpected()
							.getResultURI());

					if (verification.getExpected().getResultParameters() != null
							&& verification.getExpected().getResultParameters()
									.getParameter().size() != 0) {

						expectedResult = applyTemplate(expectedResult,
								verification.getExpected()
										.getResultParameters().getParameter(),
								globalContext);

					}
				}
			}

			String expectedFilter = "root()";
			if (verification.getExpected().getResultFilter() != null) {
				expectedFilter = verification.getExpected().getResultFilter();
			} else if (verification.getExpected().getResultFilterURI() != null) {
				expectedFilter = loadFile(verification.getExpected()
						.getResultFilterURI());
				if (verification.getExpected().getResultFilterParameters() != null
						&& verification.getExpected()
								.getResultFilterParameters().getParameter()
								.size() != 0) {
					expectedFilter = applyTemplate(expectedFilter, verification
							.getExpected().getResultFilterParameters()
							.getParameter(), globalContext);
				}
			}

			// filter the actual result
			XQuery xq = new XQuery(actualFilter);
			String filteredActualResult = xq.execute(actualResult, uri);
			// filter the expected result
			xq = new XQuery(expectedFilter);
			String filteredExpectedResult = xq.execute(expectedResult, uri);
			// normalize filtered results
			String normalizedActualResult = new XMLMessage(filteredActualResult)
					.normalize();
			String normalizedExpectedResult = new XMLMessage(
					filteredExpectedResult).normalize();
			// get difference
			String diff = new Diff(normalizedActualResult,
					normalizedExpectedResult).getDiff(20);
			// add report entry for the verification
			ReportVerificationType rep = new ReportVerificationType();
			rep.setName(verification.getName());
			rep.setActual(normalizedActualResult);
			rep.setExpected(normalizedExpectedResult);
			rep.setDiff(diff);
			ret.add(rep);

		}

		return ret;

	}
}
