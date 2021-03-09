package havis.test.suite;

import havis.test.suite.api.NDIContext;
import havis.test.suite.common.ndi.MapNDIProvider;
import havis.test.suite.common.ndi.SynchronizedNDIContext;
import havis.test.suite.testcase.ActualType;
import havis.test.suite.testcase.EntryType;
import havis.test.suite.testcase.ExpectedType;
import havis.test.suite.testcase.ParametersType;
import havis.test.suite.testcase.ReportVerificationType;
import havis.test.suite.testcase.VerificationType;

import java.util.ArrayList;
import java.util.List;

import net.sf.saxon.s9api.SaxonApiException;

import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXParseException;

public class VerifierTest {

	private final String XMLHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private GenericApplicationContext objContext;

	@BeforeClass
	public void setUp() {
		objContext = new GenericApplicationContext();
		objContext.refresh();
	}

	@Test
	public void verify() throws Exception {

		// no verification data
		Verifier verifier = new Verifier(objContext, "notUsed");
		List<ReportVerificationType> reports = verifier
				.verify(null, null, null);
		Assert.assertEquals(reports.size(), 0);

		reports = verifier
				.verify(null, new ArrayList<VerificationType>(), null);
		Assert.assertEquals(reports.size(), 0);

		reports = verifier.verify("<a>a</a>", null, null);
		Assert.assertEquals(reports.size(), 0);

		// check expected result without any filter
		List<VerificationType> verifications = new ArrayList<VerificationType>();
		VerificationType verification1 = new VerificationType();
		ExpectedType expected = new ExpectedType();
		expected.setResult("<a>a</a>");
		verification1.setName("ver1");
		verification1.setExpected(expected);
		verifications.add(verification1);
		reports = verifier.verify(
				XMLHeader + "" + System.getProperty("line.separator")
						+ "<a>a</a>", verifications, null);
		ReportVerificationType report = reports.get(0);
		Assert.assertEquals(report.getName(), "ver1");
		Assert.assertEquals(
				report.getActual(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<a>a</a>"
						+ System.getProperty("line.separator") + "");
		Assert.assertEquals(
				report.getExpected(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<a>a</a>"
						+ System.getProperty("line.separator") + "");
		Assert.assertNull(report.getDiff());

		// use actual result filter
		verifications = new ArrayList<VerificationType>();
		verification1 = new VerificationType();
		ActualType actual = new ActualType();
		actual.setResultFilter("//b");
		expected = new ExpectedType();
		expected.setResult(XMLHeader + "<b> <c>c</c></b>");
		verification1.setActual(actual);
		verification1.setExpected(expected);
		verifications.add(verification1);
		reports = verifier.verify("<a>" + System.getProperty("line.separator")
				+ "  <b>" + System.getProperty("line.separator")
				+ "    <c>c</c>" + System.getProperty("line.separator")
				+ "  </b>" + System.getProperty("line.separator") + "</a>",
				verifications, null);
		Assert.assertEquals(reports.size(), 1);
		Assert.assertNull(reports.get(0).getDiff());

		// use actual result filter with empty result
		verifications = new ArrayList<VerificationType>();
		verification1 = new VerificationType();
		actual = new ActualType();
		actual.setResultFilter("//x");
		expected = new ExpectedType();
		expected.setResult("");
		verification1.setActual(actual);
		verification1.setExpected(expected);
		verifications.add(verification1);
		try {
			reports = verifier.verify("<a>a</a>", verifications, null);
			Assert.fail();
		} catch (SAXParseException | SaxonApiException e) {
			Assert.assertTrue(e.toString().contains("lineNumber"));
		} catch (Exception e) {
			Assert.fail();
		}

		// use expected result filter
		verifications = new ArrayList<VerificationType>();
		verification1 = new VerificationType();
		expected = new ExpectedType();
		expected.setResult("<c><a>a</a></c>");
		expected.setResultFilter("//a");
		verification1.setExpected(expected);
		verifications.add(verification1);
		reports = verifier.verify("<a>a</a>", verifications, null);
		Assert.assertEquals(reports.size(), 1);
		Assert.assertNull(reports.get(0).getDiff());

		// use expected result filter with empty result
		verifications = new ArrayList<VerificationType>();
		verification1 = new VerificationType();
		expected = new ExpectedType();
		expected.setResult("<a>a</a>");
		verification1.setExpected(expected);
		verifications.add(verification1);
		try {
			reports = verifier.verify("", verifications, null);
			Assert.fail();
		} catch (SAXParseException | SaxonApiException e) {
			Assert.assertTrue(e.toString().contains("lineNumber"));
		} catch (Exception e) {
			Assert.fail();
		}

		// use actual and expected result filters
		verifications = new ArrayList<VerificationType>();
		verification1 = new VerificationType();
		actual = new ActualType();
		actual.setResultFilter("//b");
		verification1.setActual(actual);
		expected = new ExpectedType();
		expected.setResult("<x>  <b><c>c</c> </b></x>");
		expected.setResultFilter("x/b");
		verification1.setExpected(expected);
		verifications.add(verification1);
		reports = verifier.verify("<a>" + System.getProperty("line.separator")
				+ "  <b>" + System.getProperty("line.separator")
				+ "    <c>c</c>" + System.getProperty("line.separator")
				+ "  </b>" + System.getProperty("line.separator") + "</a>",
				verifications, null);
		Assert.assertEquals(reports.size(), 1);
		Assert.assertNull(reports.get(0).getDiff());
	}

	@Test
	public void verfifyViaFiles() throws Exception {

		String actualResultFilter = "test/Havis/RfidTestSuite/Testautomat/verifierActualResultFilter.xquery";
		String expectedResult = "test/Havis/RfidTestSuite/Testautomat/verifierExpectedResult.xml";
		
		String home = "test";
		Verifier verifier = new Verifier(objContext, home.toString());

		ParametersType parameters = new ParametersType();
		EntryType a = new EntryType();
		a.setName("content");
		a.setValue("b");
		EntryType b = new EntryType();
		b.setName("rootActual");
		b.setValue("/ra");
		b.setGlobalContextCommunity("ccc");
		EntryType c = new EntryType();
		c.setName("rootExpected");
		c.setValue("/re");
		c.setGlobalContextCommunity("ddd");
		parameters.getParameter().add(a);
		parameters.getParameter().add(b);
		parameters.getParameter().add(c);

		ActualType actual = new ActualType();
		actual.setResultFilterURI(actualResultFilter.toString());
		actual.setResultFilterParameters(parameters);
		ExpectedType expected = new ExpectedType();
		expected.setResultURI(expectedResult.toString());
		ParametersType groupParas = new ParametersType();
		groupParas.getParameter().addAll(parameters.getParameter());
		expected.setResultFilterParameters(groupParas);

		List<VerificationType> verifications = new ArrayList<VerificationType>();
		VerificationType verification1 = new VerificationType();
		verification1.setActual(actual);
		verification1.setExpected(expected);
		verifications.add(verification1);

		NDIContext context = new SynchronizedNDIContext();
		context.setProvider(new MapNDIProvider());
		context.setValue("ccc", "/ra", "x");
		context.setValue("ddd", "/re", "y");

		List<ReportVerificationType> reports = verifier.verify(
				"<x>" + System.getProperty("line.separator") + "  <b>"
						+ System.getProperty("line.separator") + "    <c>c</c>"
						+ System.getProperty("line.separator") + "  </b>"
						+ System.getProperty("line.separator") + "</x>",
				verifications, context);
		Assert.assertEquals(reports.size(), 1);
		//TODO Redesign
		//Assert.assertNull(reports.get(0).getDiff());
	}

	@Test
	public void verifyErrorResult() throws Exception {
		Verifier rv = new Verifier(objContext, "notUsed");
		// a result that does not match
		List<VerificationType> verifications = new ArrayList<VerificationType>();
		VerificationType verification1 = new VerificationType();
		ExpectedType expected = new ExpectedType();
		expected.setResult("<a><b>b</b></a>");
		verification1.setExpected(expected);
		verifications.add(verification1);
		verification1 = new VerificationType();
		expected = new ExpectedType();
		expected.setResult("<a><b>XXX</b></a>");
		verification1.setExpected(expected);
		verifications.add(verification1);

		List<ReportVerificationType> reports = rv.verify(XMLHeader + ""
				+ System.getProperty("line.separator") + "<a><b>XXX</b></a>",
				verifications, null);
		Assert.assertEquals(reports.size(), 2);
		ReportVerificationType report = reports.get(0);

		Assert.assertEquals(
				report.getActual(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<a>"
						+ System.getProperty("line.separator")
						+ "    <b>XXX</b>"
						+ System.getProperty("line.separator") + "</a>"
						+ System.getProperty("line.separator") + "");
		Assert.assertEquals(
				report.getExpected(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ System.getProperty("line.separator") + "<a>"
						+ System.getProperty("line.separator") + "    <b>b</b>"
						+ System.getProperty("line.separator") + "</a>"
						+ System.getProperty("line.separator") + "");

		String lineSepOffset = "";
		if (System.getProperty("line.separator").length() == 1) {
			lineSepOffset = "UT";
		}

		Assert.assertEquals(
				report.getDiff(),
				"- ..." + lineSepOffset + "F-8\"?>"
						+ System.getProperty("line.separator") + "<a>"
						+ System.getProperty("line.separator")
						+ "    <b>XXX</b>"
						+ System.getProperty("line.separator") + "</a>"
						+ System.getProperty("line.separator") + "..."
						+ System.getProperty("line.separator") + "+ ..."
						+ lineSepOffset + "F-8\"?>"
						+ System.getProperty("line.separator") + "<a>"
						+ System.getProperty("line.separator") + "    <b>b</b>"
						+ System.getProperty("line.separator") + "</a>"
						+ System.getProperty("line.separator") + "...");
		report = reports.get(1);
		Assert.assertNull(report.getDiff());
	}
}
