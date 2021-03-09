package havis.test.suite;

public class ConfigurationException extends Exception {
	private static final long serialVersionUID = 7504175433949158154L;

	public ConfigurationException(String message) {
		super(message);
	}

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
