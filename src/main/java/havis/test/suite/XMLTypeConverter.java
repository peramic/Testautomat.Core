package havis.test.suite;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.NDIProvider;
import havis.test.suite.api.dto.VerificationReport;
import havis.test.suite.testcase.DictionaryType;
import havis.test.suite.testcase.EntryType;
import havis.test.suite.testcase.ListType;
import havis.test.suite.testcase.ReportVerificationType;
import havis.test.suite.testcase.VerificationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLTypeConverter {

	public Map<String, Object> convert(List<EntryType> entries,
			NDIContext globalContext) {
		return convert(entries, globalContext, null);
	}

	@SuppressWarnings("unchecked")
	public List<EntryType> convert(Map<String, Object> entries) {

		if (entries == null) {
			return new ArrayList<EntryType>();
		}

		ArrayList<EntryType> ret = new ArrayList<EntryType>();

		for (Map.Entry<String, Object> entry : entries.entrySet()) {
			String value = null;
			EntryType type = new EntryType();
			type.setName(entry.getKey());
			if (entry.getValue() != null) {
				if (entry.getValue() instanceof String) {
					value = (String) entry.getValue();
					type.setValue(value);
				} else {
					if (entry.getValue() instanceof Map) {
						DictionaryType dicType = new DictionaryType();
						dicType.getEntry().clear();
						dicType.getEntry()
								.addAll(convert((Map<String, Object>) entry
										.getValue()));

						type.setDictionary(dicType);
					} else if (entry.getValue() instanceof List) {
						type.setList(convert((List<Object>) entry.getValue()));
					} else {
						value = entry.getValue().toString();
						type.setValue(value);
					}
				}
			}
			ret.add(type);
		}
		return ret;
	}

	public List<VerificationReport> convert(
			List<ReportVerificationType> reports,
			List<VerificationType> verifications) {
		ArrayList<VerificationReport> ret = new ArrayList<VerificationReport>();

		if (reports == null) {
			return ret;
		}
		for (ReportVerificationType report : reports) {
			VerificationReport r = new VerificationReport();
			r.setVerificationName(report.getName());
			r.setActualValue(report.getActual());
			r.setExpectedValue(report.getExpected());
			r.setValueDifference(report.getDiff());
			if (verifications != null) {
				// for each verification
				for (VerificationType verification : verifications) {
					// if name matches
					if (verification.getName().equals(report.getName())) {
						// set verification comment to report
						r.setVerificationComment(verification.getComment());
						break;
					}
				}
			}
			ret.add(r);
		}
		return ret;
	}

	private Map<String, Object> convert(List<EntryType> entries,
			NDIContext globalContext, String community) {
		HashMap<String, Object> ret = new HashMap<String, Object>();

		if (entries == null) {
			return ret;
		}

		for (EntryType entry : entries) {
			String entryCommunity = community;

			if (entry.getGlobalContextCommunity() != null) {
				entryCommunity = entry.getGlobalContextCommunity();
			}

			Object value;

			if (entry.getValue() != null) {
				value = getValue(entry.getValue(), globalContext,
						entryCommunity);
			} else if (entry.getDictionary() != null) {
				value = convert(entry.getDictionary().getEntry(),
						globalContext, entryCommunity);
			} else {
				value = convert(entry.getList(), globalContext, entryCommunity);
			}

			ret.put(entry.getName(), value);
		}
		return ret;
	}

	private List<Object> convert(ListType entries, NDIContext globalContext,
			String community) {
		List<Object> ret = new ArrayList<Object>();

		if (entries == null) {
			return ret;
		}

		Object value = null;
		if (entries.getValue() != null) {
			for (String entry : entries.getValue()) {
				value = getValue(entry, globalContext, community);
				ret.add(value);
			}
		} else if (entries.getDictionary() != null) {
			for (DictionaryType entry : entries.getDictionary()) {
				value = convert(entry.getEntry(), globalContext, community);
				ret.add(value);
			}
		} else {
			for (ListType entry : entries.getList()) {
				value = convert(entry, globalContext, community);
				ret.add(value);
			}
		}
		return ret;
	}

	private static Object getValue(String value, NDIProvider globalContext,
			String community) {
		return community == null ? value : globalContext.getValue(community,
				value);
	}

	@SuppressWarnings("unchecked")
	private ListType convert(List<Object> entries) {
		final int VALUE_LIST_CHOICE = 0;
		final int LIST_LIST_CHOICE = 1;
		final int DICTIONARY_LIST_CHOICE = 2;
		int type = 0;

		if (entries == null) {
			return new ListType();
		}

		List<String> stringItems = new ArrayList<String>();
		List<DictionaryType> dictItems = new ArrayList<DictionaryType>();
		List<ListType> listItems = new ArrayList<ListType>();

		for (Object entry : entries) {
			if (entry instanceof String) {
				type = VALUE_LIST_CHOICE;
				stringItems.add((String) entry);
			} else if (entry instanceof Map) {
				type = DICTIONARY_LIST_CHOICE;
				DictionaryType dict = new DictionaryType();
				dict.getEntry().clear();
				dict.getEntry().addAll(convert((Map<String, Object>) entry));
				dictItems.add(dict);
			} else {
				type = LIST_LIST_CHOICE;
				listItems.add((ListType) entry);
			}
		}

		ListType ret = new ListType();
		switch (type) {
		case VALUE_LIST_CHOICE:
			ret.getValue().clear();
			ret.getValue().addAll(stringItems);
			break;
		case LIST_LIST_CHOICE:
			ret.getList().clear();
			ret.getList().addAll(listItems);
			break;
		case DICTIONARY_LIST_CHOICE:
			ret.getDictionary().clear();
			ret.getDictionary().addAll(dictItems);
			break;
		default:
			return ret;
		}
		return ret;
	}
}
