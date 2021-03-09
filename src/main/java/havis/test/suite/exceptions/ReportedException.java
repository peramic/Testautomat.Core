package havis.test.suite.exceptions;

public class ReportedException extends Exception {
	private static final long serialVersionUID = 7504175433949158154L;

	public ReportedException(String message) {
		super(message);
	}

	public ReportedException(String message, Throwable e) {
		super(message,e);
	}

}
