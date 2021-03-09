package havis.test.suite;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.dto.VerificationReport;
import havis.test.suite.common.ndi.MapNDIProvider;
import havis.test.suite.common.ndi.SynchronizedNDIContext;
import havis.test.suite.testcase.DictionaryType;
import havis.test.suite.testcase.EntryType;
import havis.test.suite.testcase.ListType;
import havis.test.suite.testcase.ReportVerificationType;
import havis.test.suite.testcase.VerificationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.testng.Assert;
import org.testng.annotations.Test;

public class XMLTypeConverterTest {
	@SuppressWarnings("unchecked")
	@Test
	public void listEntryType() {
		XMLTypeConverter c = new XMLTypeConverter();

		// key -> string
		EntryType e1 = new EntryType();
		e1.setName("a");
		e1.setValue("1");
		EntryType e2 = new EntryType();
		e2.setName("b");
		e2.setValue("2");
		EntryType e3 = new EntryType();
		e3.setName("c");
		e3.setValue("/x");
		e3.setGlobalContextCommunity("ccc");
		List<EntryType> entries = new ArrayList<EntryType>();
		entries.add(e1);
		entries.add(e2);
		entries.add(e3);
		NDIContext context = new SynchronizedNDIContext();
		MapNDIProvider provider = new MapNDIProvider();
		context.setProvider(provider);
		context.setValue("ccc", "/x", "3");
		Map<String, Object> e = c.convert(entries, context);
		Assert.assertEquals(e.get("a"), "1");
		Assert.assertEquals(e.get("b"), "2");
		Assert.assertEquals(e.get("c"), "3");

		// key -> list of strings
		e1 = new EntryType();
		e1.setName("a");
		ListType l1 = new ListType();
		List<String> sl = new ArrayList<String>();
		sl.add("1");
		sl.add("2");
		l1.getValue().addAll(sl);
		e1.setList(l1);
		
		e2 = new EntryType();
		e2.setName("b");
		l1 = new ListType();
		sl = new ArrayList<String>();
		sl.add("3");
		sl.add("4");
		l1.getValue().addAll(sl);
		e2.setList(l1);
		
		e3 = new EntryType();
		e3.setName("c");
		l1 = new ListType();
		sl = new ArrayList<String>();
		sl.add("/x");
		sl.add("/y");
		
		l1.getValue().addAll(sl);
		e3.setList(l1);
		e3.setGlobalContextCommunity("ccc");
		entries = new ArrayList<EntryType>();
		entries.add(e1);
		entries.add(e2);
		entries.add(e3);
		context = new SynchronizedNDIContext();
		provider = new MapNDIProvider();
		context.setProvider(provider);
		context.setValue("ccc", "/x", "5");
		context.setValue("ccc", "/y", "6");
		e = c.convert(entries, context);
		ArrayList<Object> l = (ArrayList<Object>) e.get("a");
		Assert.assertEquals(l.get(0), "1");
		Assert.assertEquals(l.get(1), "2");
		l = (ArrayList<Object>) e.get("b");
		Assert.assertEquals(l.get(0), "3");
		Assert.assertEquals(l.get(1), "4");
		l = (ArrayList<Object>) e.get("c");
		Assert.assertEquals(l.get(0), "5");
		Assert.assertEquals(l.get(1), "6");

		// key -> (key -> string)
		e1 = new EntryType();
		e1.setName("a");
		DictionaryType d1 = new DictionaryType();
		EntryType e11 = new EntryType();
		e11.setName("a1");
		e11.setValue("1");
		EntryType e12 = new EntryType();
		e12.setName("a2");
		e12.setValue("2");
		entries = new ArrayList<EntryType>();
		entries.add(e11);
		entries.add(e12);
		
		d1.getEntry().addAll(entries);
		e1.setDictionary(d1);
		
		e2 = new EntryType();
		e2.setName("b");
		d1 = new DictionaryType();
		EntryType e21 = new EntryType();
		e21.setName("b1");
		e21.setValue("3");
		EntryType e22 = new EntryType();
		e22.setName("b2");
		e22.setValue("4");
		entries = new ArrayList<EntryType>();
		entries.add(e21);
		entries.add(e22);
		d1.getEntry().addAll(entries);
		e2.setDictionary(d1);
		e3 = new EntryType();
		e3.setName("c");
		d1 = new DictionaryType();
		EntryType e31 = new EntryType();
		e31.setName("b1");
		e31.setValue("/x");
		e31.setGlobalContextCommunity("ddd");
		EntryType e32 = new EntryType();
		e32.setName("b2");
		e32.setValue("/y");
		entries = new ArrayList<EntryType>();
		entries.add(e31);
		entries.add(e32);
		d1.getEntry().addAll(entries);
		e3.setDictionary(d1);
		e3.setGlobalContextCommunity("ccc");
		entries = new ArrayList<EntryType>();
		entries.add(e1);
		entries.add(e2);
		entries.add(e3);
		context = new SynchronizedNDIContext();
		provider = new MapNDIProvider();
		context.setProvider(provider);
		context.setValue("ccc", "/x", "5");
		context.setValue("ccc", "/y", "6");
		context.setValue("ddd", "/x", "7");
		context.setValue("ddd", "/y", "8");
		e = c.convert(entries, context);
		HashMap<String, Object> d = (HashMap<String, Object>) e.get("a");
		Assert.assertEquals(d.get("a1"), "1");
		Assert.assertEquals(d.get("a2"), "2");
		d = (HashMap<String, Object>) e.get("b");
		Assert.assertEquals(d.get("b1"), "3");
		Assert.assertEquals(d.get("b2"), "4");
		d = (HashMap<String, Object>) e.get("c");
		Assert.assertEquals(d.get("b1"), "7");
		Assert.assertEquals(d.get("b2"), "6");
	}

	@Test
	public void dictionaryStringObject() {
		XMLTypeConverter c = new XMLTypeConverter();

		// key -> string
		Map<String, Object> entries = new TreeMap<String, Object>();
		entries.put("a", "1");
		entries.put("b", "2");
		List<EntryType> e = c.convert(entries);
		Assert.assertEquals(e.get(0).getName(), "a");
		Assert.assertEquals(e.get(0).getValue(), "1");
		Assert.assertEquals(e.get(1).getName(), "b");
		Assert.assertEquals(e.get(1).getValue(), "2");

		// key -> list of strings
		entries = new TreeMap<String, Object>();
		List<Object> l = new ArrayList<Object>();
		l.add("1");
		l.add("2");
		entries.put("a", l);
		l = new ArrayList<Object>();
		l.add("3");
		l.add("4");
		entries.put("b", l);
		e = c.convert(entries);
		Assert.assertEquals(e.get(0).getName(), "a");
		ListType list = (ListType) e.get(0).getList();
		Assert.assertEquals(list.getValue().get(0), "1");
		Assert.assertEquals(list.getValue().get(1), "2");
		Assert.assertEquals(e.get(1).getName(), "b");
		list = (ListType) e.get(1).getList();
		Assert.assertEquals(list.getValue().get(0), "3");
		Assert.assertEquals(list.getValue().get(1), "4");

		// key -> (key -> string)
		entries = new TreeMap<String, Object>();
		Map<String, Object> m = new TreeMap<String, Object>();
		m.put("a1", "1");
		m.put("a2", "2");
		entries.put("a", m);
		m = new TreeMap<String, Object>();
		m.put("b1", "3");
		m.put("b2", "4");
		entries.put("b", m);
		e = c.convert(entries);
		Assert.assertEquals(e.get(0).getName(), "a");
		DictionaryType dict = (DictionaryType) e.get(0).getDictionary();
		Assert.assertEquals(dict.getEntry().get(0).getName(), "a1");
		Assert.assertEquals(dict.getEntry().get(0).getValue(), "1");
		Assert.assertEquals(dict.getEntry().get(1).getName(), "a2");
		Assert.assertEquals(dict.getEntry().get(1).getValue(), "2");
		Assert.assertEquals(e.get(1).getName(), "b");
		dict = (DictionaryType) e.get(1).getDictionary();
		Assert.assertEquals(dict.getEntry().get(0).getName(), "b1");
		Assert.assertEquals(dict.getEntry().get(0).getValue(), "3");
		Assert.assertEquals(dict.getEntry().get(1).getName(), "b2");
		Assert.assertEquals(dict.getEntry().get(1).getValue(), "4");

	}

	@Test
	public void VerificationReports() {
		XMLTypeConverter c = new XMLTypeConverter();

		List<VerificationReport> e = c.convert(
				(ArrayList<ReportVerificationType>) null, null);
		Assert.assertEquals(e.size(), 0);

		ArrayList<VerificationType> verifications = new ArrayList<VerificationType>();
		VerificationType veri = new VerificationType();
		veri.setName("a");
		veri.setComment("b");
		verifications.add(veri);
		veri = new VerificationType();
		veri.setName("c");
		veri.setComment("d");
		verifications.add(veri);
		veri = new VerificationType();
		veri.setName("e");
		veri.setComment("f");
		verifications.add(veri);

		List<ReportVerificationType> reports = new ArrayList<ReportVerificationType>();
		ReportVerificationType report = new ReportVerificationType();
		report.setName("e");
		report.setActual("1");
		report.setExpected("2");
		report.setDiff("3");
		reports.add(report);
		report = new ReportVerificationType();
		report.setName("x");
		report.setActual("4");
		report.setExpected("5");
		report.setDiff("6");
		reports.add(report);
		report = new ReportVerificationType();
		report.setName("a");
		report.setActual("7");
		report.setExpected("8");
		report.setDiff("9");
		reports.add(report);

		e = c.convert(reports, verifications);
		Assert.assertEquals(e.size(), 3);
		VerificationReport resReport = e.get(0);
		Assert.assertEquals(resReport.getVerificationName(), "e");
		Assert.assertEquals(resReport.getVerificationComment(), "f");
		Assert.assertEquals(resReport.getActualValue(), "1");
		Assert.assertEquals(resReport.getExpectedValue(), "2");
		Assert.assertEquals(resReport.getValueDifference(), "3");
		resReport = e.get(1);
		Assert.assertEquals(resReport.getVerificationName(), "x");
		Assert.assertNull(resReport.getVerificationComment());
		Assert.assertEquals(resReport.getActualValue(), "4");
		Assert.assertEquals(resReport.getExpectedValue(), "5");
		Assert.assertEquals(resReport.getValueDifference(), "6");
		resReport = e.get(2);
		Assert.assertEquals(resReport.getVerificationName(), "a");
		Assert.assertEquals(resReport.getVerificationComment(), "b");
		Assert.assertEquals(resReport.getActualValue(), "7");
		Assert.assertEquals(resReport.getExpectedValue(), "8");
		Assert.assertEquals(resReport.getValueDifference(), "9");

	}

}
