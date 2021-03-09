package havis.test.suite.dto;

import java.util.Map;

import org.springframework.context.ApplicationContext;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.UI;

public class TestAutomatDTO {

	private NDIContext globalContext;
	private ApplicationContext objContext;
	// Mapping of the objectId to its declaration file (full path).
	private Map<String, String> objectIdFiles;
	private UI ui;

	public NDIContext getGlobalContext() {
		return globalContext;
	}

	public void setGlobalContext(NDIContext globalContext) {
		this.globalContext = globalContext;
	}

	public ApplicationContext getObjContext() {
		return objContext;
	}

	public void setObjContext(ApplicationContext objContext) {
		this.objContext = objContext;
	}

	public Map<String, String> getObjectIdFiles() {
		return objectIdFiles;
	}

	public void setObjectIdFiles(Map<String, String> objectIdFiles) {
		this.objectIdFiles = objectIdFiles;
	}

	public UI getUi() {
		return ui;
	}

	public void setUi(UI ui) {
		this.ui = ui;
	}

}
