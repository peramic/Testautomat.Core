package havis.test.suite;

import havis.test.suite.api.App;
import havis.test.suite.api.NDIContext;
import havis.test.suite.api.Reporter;
import havis.test.suite.api.StatisticCreator;
import havis.test.suite.api.UI;
import havis.test.suite.api.dto.ModulesObjectIds;
import havis.test.suite.common.PathResolver;
import havis.test.suite.common.messaging.XSD;
import havis.test.suite.common.ndi.MapNDIProvider;
import havis.test.suite.common.ndi.SynchronizedNDIContext;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCasesDTO;
import havis.test.suite.exceptions.VerificationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;


public class Testautomat {
	private static final Logger log = LoggerFactory
			.getLogger(Testautomat.class);
	private final String modulesDir = "modules";
	private final String objectsDeploymentDescriptor = "beans.xml";
	private final String urlHead = "classpath:";
	private final String reportsDir = "reportsDir";
	private final String statisticsDir = "statisticsDir";
	private final String xsdDir = "xsd";
	private final String xsdDescriptorFileName = "TestCase.xsd";
	private final String testCasesDescriptorFileName = "testCase.xml";
	private final String testCasesDir = "testcases";
	/**
	 * ObjectId -> file name
	 */
	private Map<String, String> objectIdFiles = new HashMap<String, String>();

	/**
	 * Runs the test automat
	 * 
	 * @param args
	 * @return
	 */
	public int run(String[] args) {
		String uiObjectId = null;
		UI ui = null;
		ModulesObjectIds modulesObjectIds = null;
		Map<String, App> apps = null;
		Map<String, Reporter> reporters = null;
		Map<String, StatisticCreator> statisticCreators = null;

		try {
			// load deployment descriptors of the modules
			ApplicationContext objContext = loadModuleDeploymentDescriptors();
			// start UI
			NDIContext globalContext = new SynchronizedNDIContext();
			globalContext.setProvider(new MapNDIProvider());
			uiObjectId = getUIObjectId(args);
			List<Path> testCasesDirectories = PathResolver
					.getAbsolutePathFromResource(testCasesDir,
							testCasesDescriptorFileName);
			List<String> testCasesDirs = new ArrayList<String>();
			for (Path testCaseDir : testCasesDirectories) {
				testCasesDirs.add(testCaseDir.toString());
			}
			ui = startUI(globalContext, objContext, uiObjectId, testCasesDirs,
					args);
			List<String> testCasesPaths = ui.getTestCasesPaths();
			// if any test case shall be executed
			if (testCasesPaths != null && testCasesPaths.size() > 0) {
				// get objectIds of modules from UI
				modulesObjectIds = ui.getModulesObjectIds();
				// start app modules
				apps = startApps(globalContext, objContext,
						modulesObjectIds.getApps());
				// get test case parameters from app modules
				List<App> appList = getObjectList(modulesObjectIds.getApps(),
						apps);
				Map<String, Object> testCasesParameters = getTestCasesParameters(appList);
				// start reporter modules
				reporters = startReporters(globalContext, objContext,
						modulesObjectIds.getReporters());
				// start statistic creator modules
				statisticCreators = startStatisticCreator(globalContext,
						objContext, modulesObjectIds.getStatisticCreators());
				// create list of reporters and statistic creators
				List<Reporter> reporterList = getObjectList(
						modulesObjectIds.getReporters(), reporters);
				List<StatisticCreator> statisticCreatorList = getObjectList(
						modulesObjectIds.getStatisticCreators(),
						statisticCreators);
				ArrayList<Reporter> allReportersList = new ArrayList<Reporter>();

				allReportersList.addAll(reporterList);
				allReportersList.addAll(statisticCreatorList);
				// while the UI provides test cases
				do {
					// execute test cases
					executeTestsCases(globalContext, objContext, ui,
							testCasesDirs, testCasesPaths, testCasesParameters,
							allReportersList);
					// execute statistic creator modules
					executeStatisticCreators(
							modulesObjectIds.getStatisticCreators(),
							statisticCreators);
					// clean up generated reports
					cleanupReporters(modulesObjectIds.getReporters(), reporters);
					cleanupStatisticCreators(
							modulesObjectIds.getStatisticCreators(),
							statisticCreators);
					// get next test cases from UI
					testCasesPaths = ui.getTestCasesPaths();
				} while (testCasesPaths != null && testCasesPaths.size() > 0);
			}
		} catch (VerificationException e) {
			log.error("", e);
			return 3;
		} catch (Exception e) {
			log.error("", e);
			return 2;
		} finally {
			if (statisticCreators != null) {
				// reverse order of statistic creators
				Collections.reverse(modulesObjectIds.getStatisticCreators());
				// stop statistic creator modules
				stopStatisticCreators(modulesObjectIds.getStatisticCreators(),
						statisticCreators);
			}

			if (reporters != null) {
				// reverse order of reporters
				Collections.reverse(modulesObjectIds.getReporters());
				// stop reporter modules
				stopReporters(modulesObjectIds.getReporters(), reporters);
			}

			if (apps != null) {
				// reverse order of reporters
				Collections.reverse(modulesObjectIds.getApps());
				// stop app modules
				stopApps(modulesObjectIds.getApps(), apps);
			}

			if (ui != null) {
				// stop IU
				stopUI(uiObjectId, ui);
			}
		}
		return 0;
	}

	/**
	 * Gets the objectId of the UI
	 * 
	 * @param args
	 * @return
	 * @throws ParseException
	 */
	private static String getUIObjectId(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine line = parser.parse(new Options(), args, true);
		String[] arguments = line.getArgs();
		if (arguments.length == 0) {
			throw new ParseException("Missing Parameter: AppSettingsUIObjectId");
		} else {
			return arguments[0];
		}
	}

	/**
	 * Loads the object definitions of available modules via the spring
	 * framework. Each module has its own sub directory with the deployment
	 * descriptor "objects.xml"
	 * 
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	private ApplicationContext loadModuleDeploymentDescriptors()
			throws IOException, URISyntaxException {
		log.info("Loading deployment descriptors of the modules");
		// initialize spring
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		List<Path> basePathes = null;
		// get Base pathes
		try {
			basePathes = PathResolver.getAbsolutePathFromResource(modulesDir,
					objectsDeploymentDescriptor);
		} catch (Exception e) {
			throw e;
		}
		// For each basePath
		ArrayList<String> prevNames = new ArrayList<String>();
		for (Path basePath : basePathes) {
			String plPath = urlHead + basePath.toString() + "/"
					+ objectsDeploymentDescriptor;
			reader.loadBeanDefinitions(plPath);
			ArrayList<String> names = new ArrayList<String>(
					Arrays.asList(reader.getRegistry().getBeanDefinitionNames()));
			names.removeAll(prevNames);
			// for each new objectId
			for (String name : names) {
				// add file name for objectId to dictionary
				objectIdFiles.put(name, basePath.toString());
			}
			prevNames.addAll(names);
		}
		context.refresh();
		return context;
	}

	/**
	 * Loads an user interface module and starts it
	 * 
	 * @param globalContext
	 * @param objContext
	 * @param objectId
	 * @param testCasesDir
	 * @param args
	 * @return the started UI module
	 * @throws Exception
	 */
	private UI startUI(NDIContext globalContext, ApplicationContext objContext,
			String objectId, List<String> testCasesDir, String[] args)
			throws Exception {
		log.info("Starting user interface module with objectId " + objectId);
		// load UI module
		UI ui = (UI) objContext.getBean(objectId);
		String moduleHome = Paths.get(objectIdFiles.get(objectId)).getParent()
				.toString();
		// start UI
		ui.start(globalContext, moduleHome, testCasesDir, args);
		return ui;

	}

	/**
	 * Starts app modules
	 * 
	 * @param globalContext
	 * @param objContext
	 * @param appObjectIds
	 * @return
	 * @throws Exception 
	 */
	private Map<String, App> startApps(NDIContext globalContext,
			ApplicationContext objContext, List<String> appObjectIds) throws Exception {
		HashMap<String, App> ret = new HashMap<String, App>();
		for (String objectId : appObjectIds) {
			log.info("Starting app module with objectId " + objectId);
			App app = (App) objContext.getBean(objectId);
			String moduleHome = Paths.get(objectIdFiles.get(objectId))
					.toString();
			app.Start(globalContext, moduleHome);
			ret.put(objectId, app);
		}
		return ret;
	}

	/**
	 * Stops app modules. Any exceptions are thrown and written to log
	 * 
	 * @param objectIds
	 * @param apps
	 */
	private void stopApps(List<String> objectIds, Map<String, App> apps) {

		for (String objectId : objectIds) {
			log.info("Stopping app module with objectId '" + objectId + "'");

			try {
				apps.get(objectId).Stop();
			} catch (Exception e) {
				log.error(
						"Ignoring error while stopping app module with objectId '"
								+ objectId + "'", e);
			}
		}
	}

	/**
	 * Starts reporter modules
	 * 
	 * @param globalContext
	 * @param objContext
	 * @param reportersObjectIds
	 * @return
	 * @throws Exception
	 */
	private Map<String, Reporter> startReporters(NDIContext globalContext,
			ApplicationContext objContext, List<String> reportersObjectIds)
			throws Exception {
		Map<String, Reporter> ret = new HashMap<String, Reporter>();

		if (reportersObjectIds.size() > 0) {
			log.info("Output directory for reporter modules: "
					+ Paths.get("").toAbsolutePath().toString()
					+ File.separator + "var" + File.separator + reportsDir);
		}

		File var = new File(Paths.get("").toAbsolutePath().toString(), "target");
		if (!var.exists()) {
			var.mkdir();
		}
		File repDir = new File(var.getAbsolutePath(), reportsDir);
		if (!repDir.exists()) {
			repDir.mkdir();
		}

		for (String objectId : reportersObjectIds) {
			log.info("Starting reporter module with objectId " + objectId);
			Reporter reporter = (Reporter) objContext.getBean(objectId);
			String moduleHome = Paths.get(objectIdFiles.get(objectId))
					.getParent().toString();
			String outputDir = repDir.getAbsolutePath() + File.separator
					+ objectId;
			// load reporter module and start it
			reporter.start(globalContext, moduleHome, outputDir);
			ret.put(objectId, reporter);
		}

		return ret;
	}

	/**
	 * Stops reporter modules. Any exceptions are thrown and written to log
	 * 
	 * @param objectIds
	 * @param reporters
	 */
	private void stopReporters(List<String> objectIds,
			Map<String, Reporter> reporters) {
		// for each objectId
		for (String objectId : objectIds) {
			log.info("Stopping reporter module with objectId '" + objectId
					+ "'");
			try {
				// stop reporter module
				reporters.get(objectId).stop();
			} catch (Exception e) {
				log.error(
						"Ignoring error while stopping reporter module with objectId '"
								+ objectId + "'", e);
			}
		}
	}

	/**
	 * Cleans up the generated reports of reporter modules
	 * 
	 * @param objectIds
	 * @param reporters
	 * @throws Exception
	 */
	private static void cleanupReporters(List<String> objectIds,
			Map<String, Reporter> reporters) throws Exception {
		for (String objectId : objectIds) {
			log.info("Cleaning up reporter module with objectId '" + objectId
					+ "'");
			reporters.get(objectId).cleanup();
		}
	}

	/**
	 * Starts statistic creator modules
	 * 
	 * @param globalContext
	 * @param objContext
	 * @param statisticCreatorObjectIds
	 * @return
	 * @throws Exception
	 */
	private Map<String, StatisticCreator> startStatisticCreator(
			NDIContext globalContext, ApplicationContext objContext,
			List<String> statisticCreatorObjectIds) throws Exception {
		HashMap<String, StatisticCreator> ret = new HashMap<String, StatisticCreator>();
		if (statisticCreatorObjectIds.size() > 0) {
			log.info("Output directory for statistic creator modules: "
					+ Paths.get("").toAbsolutePath().toString()
					+ File.separator + "var" + File.separator + statisticsDir);
		}

		File var = new File(Paths.get("").toAbsolutePath().toString(), "target");
		if (!var.exists()) {
			var.mkdir();
		}
		File statDir = new File(var.getAbsolutePath(), statisticsDir);
		if (!statDir.exists()) {
			statDir.mkdir();
		}

		for (String objectId : statisticCreatorObjectIds) {
			log.info("Starting statistic creator module with objectId "
					+ objectId);
			// load statistic creator module
			StatisticCreator statisticCreator = (StatisticCreator) objContext
					.getBean(objectId);
			String moduleHome = Paths.get(objectIdFiles.get(objectId))
					.getParent().toString();
			String outputDir = statDir.getAbsolutePath() + File.separator
					+ objectId;
			;
			statisticCreator.start(globalContext, moduleHome, outputDir);
			ret.put(objectId, statisticCreator);
		}
		return ret;
	}

	/**
	 * Executes a list of statistic creators
	 * 
	 * @param objectIds
	 * @param statisticCreators
	 * @throws Exception
	 */
	private static void executeStatisticCreators(List<String> objectIds,
			Map<String, StatisticCreator> statisticCreators) throws Exception {
		// for each objectId
		for (String objectId : objectIds) {
			log.info("Executing statistic creator module with objectId '"
					+ objectId + "'");
			// execute statistic creator modules
			statisticCreators.get(objectId).create();
		}
	}

	/**
	 * Stops statistic creator modules. Any exceptions are thrown and written to
	 * log
	 * 
	 * @param objectIds
	 * @param statisticCreators
	 */
	private static void stopStatisticCreators(List<String> objectIds,
			Map<String, StatisticCreator> statisticCreators) {
		// for each objectId
		for (String objectId : objectIds) {
			log.info("Stopping statistic creator module with objectId '"
					+ objectId + "'");
			try {
				// stop statistic creator module
				statisticCreators.get(objectId).stop();
			} catch (Exception e) {
				log.error(
						"Ignoring error while stopping statistic creator module with objectId '"
								+ objectId + "'", e);
			}
		}
	}

	/**
	 * Cleans up the generated reports of statistic creators
	 * 
	 * @param objectIds
	 * @param reporters
	 * @throws Exception
	 */
	private static void cleanupStatisticCreators(List<String> objectIds,
			Map<String, StatisticCreator> reporters) throws Exception {
		for (String objectId : objectIds) {
			log.info("Cleaning up statistic creator with objectId '" + objectId
					+ "'");
			reporters.get(objectId).cleanup();
		}
	}

	/**
	 * Stops the UI modules. Any exceptions are thrown and written to log
	 * 
	 * @param objectId
	 * @param ui
	 */
	private void stopUI(String objectId, UI ui) {
		log.info("Stopping user interface module with objectId '" + objectId
				+ "'");
		try {
			// stop reporter module
			ui.stop();
		} catch (Exception e) {
			log.error(
					"Ignoring error while stopping user interface module with objectId '"
							+ objectId + "'", e);
		}
	}

	/**
	 * Executes a list of test cases
	 * 
	 * @param globalContext
	 * @param objContext
	 * @param ui
	 *            user interface
	 * @param testCasesDirs
	 * @param testCasesPaths
	 *            Path to the directory with the test case. A relative path
	 *            starts at the base directory of all test cases
	 * @param testCasesParameters
	 * @param reporters
	 * @throws Exception
	 */
	private void executeTestsCases(NDIContext globalContext,
			ApplicationContext objContext, UI ui, List<String> testCasesDirs,
			List<String> testCasesPaths,
			Map<String, Object> testCasesParameters, List<Reporter> reporters)
			throws Exception {

		List<Path> pathes = PathResolver.getAbsolutePathFromResource(xsdDir,
				xsdDescriptorFileName);
		InputStream xsdFile = null;

		if (pathes.size() > 0) {
			Path basePath = PathResolver.getAbsolutePathFromResource(xsdDir,
					xsdDescriptorFileName).get(0);
			xsdFile = PathResolver.getResourceInputStream(basePath.toString(),
					xsdDescriptorFileName);
		} else {
			throw new Exception("XSD-File not found");
		}

		// for each test case
		for (String testCasePath : testCasesPaths) {
			TestAutomatDTO testDTO = new TestAutomatDTO();
			testDTO.setGlobalContext(globalContext);
			testDTO.setObjContext(objContext);
			testDTO.setObjectIdFiles(objectIdFiles);
			testDTO.setUi(ui);

			if (testCasesDir == null) {
				throw new Exception("Testcase not found");
			}
			// get full test case path:
			// resolve test case path relative to the directory of all test
			// cases
			String fullTestCasePath = this.testCasesDir + File.separator
					+ testCasePath;

			TestCasesDTO casesDTO = new TestCasesDTO();
			casesDTO.setHome(testCasesDir);
			casesDTO.setDescriptorFileName(testCasesDescriptorFileName);
			XSD xsd = new XSD(xsdFile, xsdDescriptorFileName);
			casesDTO.setXsd(xsd);
			casesDTO.setParameters(testCasesParameters);
			TestCase testCase = new TestCase(testDTO, casesDTO,
					fullTestCasePath, null, null);
			testCase.setReporters(reporters);
			// execute test case
			testCase.run();
		}

	}

	/**
	 * Gets the parameters for the test cases from the app modules
	 * 
	 * @param apps
	 * @return
	 */
	private static Map<String, Object> getTestCasesParameters(List<App> apps) {
		Map<String, Object> ret = new HashMap<String, Object>();
		Collections.reverse(apps);
		// for each app in reverse order (parameters can be overwritten)
		for (App app : apps) {
			if (app.getTestCaseParameters() != null) {
				// for each parameter
				for (Map.Entry<String, Object> parameter : app
						.getTestCaseParameters().entrySet()) {
					// if key does not exist yet
					if (!ret.containsKey(parameter.getKey())) {
						// add parameter
						ret.put(parameter.getKey(), parameter.getValue());
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Creates a list of objects for a set of objectIds
	 * 
	 * @param objectIds
	 *            the objectIds. The resulting object list contains the relating
	 *            objects in the same order
	 * @param objects
	 *            objectId => object
	 * @return
	 */
	private static <T> List<T> getObjectList(List<String> objectIds,
			Map<String, T> objects) {
		ArrayList<T> ret = new ArrayList<T>();

		for (String objectId : objectIds) {
			ret.add(objects.get(objectId));
		}
		return ret;
	}

}
