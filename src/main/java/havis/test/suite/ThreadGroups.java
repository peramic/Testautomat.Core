package havis.test.suite;

import havis.test.suite.api.Reporter;
import havis.test.suite.dto.TestAutomatDTO;
import havis.test.suite.dto.TestCaseDTO;
import havis.test.suite.testcase.LoopType;
import havis.test.suite.testcase.ModuleReportType;
import havis.test.suite.testcase.ModuleReportsType;
import havis.test.suite.testcase.ModuleType;
import havis.test.suite.testcase.StepReportType;
import havis.test.suite.testcase.StepReportsType;
import havis.test.suite.testcase.StepType;
import havis.test.suite.testcase.StepsType;
import havis.test.suite.testcase.TestCaseType;
import havis.test.suite.testcase.ThreadGroupType;
import havis.test.suite.testcase.ThreadsType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadGroups {
	private static final Logger log = LoggerFactory
			.getLogger(ThreadGroups.class);
	private final ThreadGroupsInfo threadGroupsInfo;
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
	 * @param threadGroups
	 * @param logBasePath
	 *            base path to the step list (used for logging)
	 */
	public ThreadGroups(TestAutomatDTO testAutomatDTO, TestCaseDTO testCaseDTO,
			List<ThreadGroupType> threadGroups, String logBasePath) {
		threadGroupsInfo = new ThreadGroupsInfo();
		threadGroupsInfo.setTestAutomatDTO(testAutomatDTO);
		threadGroupsInfo.setTestCaseDTO(testCaseDTO);
		threadGroupsInfo.setThreadGroups(threadGroups);
		threadGroupsInfo.setLogBasePath(logBasePath);
		reporters = new ArrayList<Reporter>();
	}

	/**
	 * Runs the thread group
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {
		List<ThreadGroupType> threadGroups = threadGroupsInfo.getThreadGroups();
		ThreadGroupRunner[] threadGroupRunners = new ThreadGroupRunner[threadGroups
				.size()];
		ExecutorService pool = Executors
				.newFixedThreadPool(threadGroups.size());
		List<Future<?>> futures = new ArrayList<Future<?>>(threadGroups.size());

		log.info("Starting " + threadGroups.size() + " thread group"
				+ (threadGroups.size() == 1 ? "" : "s"));
		// for each thread group
		for (int i = 0; i < threadGroups.size(); i++) {
			ThreadGroupRunner threadGroupRunner = new ThreadGroupRunner(
					threadGroupsInfo, i, (int) Thread.currentThread().getId(),
					reporters);
			// create runner for the thread group
			threadGroupRunners[i] = threadGroupRunner;
			// create thread and running the thread group
			futures.add(pool.submit(threadGroupRunners[i]));
		}
		// for each thread group
		for (int i = 0; i < threadGroups.size(); i++) {
			// Wait for thread to be finished
			futures.get(i).get();
			ThreadGroupType threadGroup = threadGroups.get(i);
			ThreadGroupRunner threadGroupRunner = threadGroupRunners[i];
			// if executing of thread group failed
			if (threadGroupRunner.getException() != null) {
				// throw exception from thread group
				throw threadGroupRunner.getException();
			}
			if (threadGroupRunner.getStepsRunners() != null) {
				Exception firstException = null;
				// for each thread of thread group
				for (int j = 0; j < threadGroupRunner.getStepsRunners().length; j++) {
					StepsRunner stepsRunner = threadGroupRunner
							.getStepsRunners()[j];
					// if thread content aborts with exception
					if (stepsRunner.getException() != null) {
						// first exception will be thrown
						if (firstException == null) {
							firstException = stepsRunner.getException();
						} else { // all other exceptions are only logged
							log.warn(
									"Thread "
											+ (j + 1)
											+ "/"
											+ threadGroupRunner
													.getStepsRunners().length
											+ " failed", stepsRunner
											.getException());
						}
					}
					// move reports from copy of step list to original step list
					moveReports(stepsRunner.getSteps(), threadGroup.getSteps()
							.getStep());
				}
				if (firstException != null) {
					// throw exception from the first step
					throw firstException;
				}
			}
		} 
		pool.shutdown();
	}

	/**
	 * Executes a thread group definition
	 * 
	 * @param threadGroupsInfo
	 * @param threadGroupIndex
	 * @param reporters
	 * @return informations about the executed threads
	 * @throws InterruptedException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws ExecutionException
	 */
	private static StepsRunner[] execThreadGroup(
			ThreadGroupsInfo threadGroupsInfo, int threadGroupIndex,
			List<Reporter> reporters) throws InterruptedException,
			ClassNotFoundException, IOException, ExecutionException {
		ThreadGroupType threadGroup = threadGroupsInfo.getThreadGroups().get(
				threadGroupIndex);
		if (threadGroup.getCount() == 0) {
			threadGroup.setCount((long) 1);
		}
		if (threadGroup.getRampUpPeriod() == 0) {
			threadGroup.setRampUpPeriod((long) 0);
		}

		StepsRunner stepsRunners[] = new StepsRunner[(int) threadGroup
				.getCount()];
		ExecutorService pool = Executors.newFixedThreadPool((int) threadGroup
				.getCount());
		List<Future<?>> futures = new ArrayList<Future<?>>(
				(int) threadGroup.getCount());
		// calculate the interval in which the threads shall be started
		int interval = (int) (threadGroup.getCount() <= 1 ? 0 : threadGroup
				.getRampUpPeriod() / (threadGroup.getCount() - 1));

		for (int i = 0; i < threadGroup.getCount(); i++) {
			// get the position of the current thread group in the test case by
			// determining the count of thread groups existing before the
			// current one
			TestCaseType testCase = threadGroupsInfo.testCaseDTO.getTestCase();
			Found found = new Found();
			found.setFound(false);
			int count = getThreadGroupCount(testCase.getSteps().getStep(),
					threadGroup, found);
			// clone the test case containing the current thread group and its
			// step list
			TestCaseType testCaseClone = deepClone(testCase);
			// get the current thread group list from the clone by using the
			// determined position
			List<ThreadGroupType> threadGroupsClone = getThreadGroups(
					testCaseClone.getSteps().getStep(), count);
			// remove step reports from thread group clone
			removeReports(threadGroupsClone.get(threadGroupIndex).getSteps()
					.getStep());
			// create new steps infos with the cloned test case
			ThreadGroupsInfo stepsInfo = new ThreadGroupsInfo();
			stepsInfo.setTestAutomatDTO(threadGroupsInfo.getTestAutomatDTO());
			TestCaseDTO tcDTO = new TestCaseDTO();
			tcDTO.setBase(threadGroupsInfo.getTestCaseDTO().getBase());
			tcDTO.setHome(threadGroupsInfo.getTestCaseDTO().getHome());
			tcDTO.setId(threadGroupsInfo.getTestCaseDTO().getId());
			tcDTO.setName(threadGroupsInfo.getTestCaseDTO().getName());
			tcDTO.setTestCase(testCaseClone);
			stepsInfo.setTestCaseDTO(tcDTO);
			stepsInfo.setThreadGroups(threadGroupsClone);
			stepsInfo.setLogBasePath(threadGroupsInfo.getLogBasePath());
			// create steps runner for the step list
			// (the cloned step list with the reports are safed inside the
			// runner)
			StepsRunner stepsRunner = new StepsRunner(stepsInfo,
					threadGroupIndex, i, (int) Thread.currentThread().getId(),
					reporters);
			// create task for running the thread
			stepsRunners[i] = stepsRunner;
		}
		Date start = new Date();
		// for each thread
		for (int i = 0; i < threadGroup.getCount(); i++) {
			// calculate offset, wait for starting point and execute the task
			long offset = i * interval;
			long totalDiff = (new Date()).getTime() - start.getTime();
			long diff = offset - totalDiff;
			if (diff > 0) {
				Thread.sleep(diff);
			} else if (threadGroup.getRampUpPeriod() > 0 && diff < 0) {
				log.warn("Could not keep the interval for thread " + i
						+ " (difference of " + (-diff) + " ms)");
			}
			log.info("Starting thread " + (i + 1) + "/"
					+ threadGroup.getCount() + " (rampUpPeriod="
					+ threadGroup.getRampUpPeriod() + ")");
			futures.add(pool.submit(stepsRunners[i]));
		}
		// wait for end of all threads
		for (Future<?> future : futures) {
			future.get();
		}
		pool.shutdown();
		return stepsRunners;
	}

	/**
	 * Executes a list of steps
	 * 
	 * @param threadGroupsInfo
	 * @param threadGroupIndex
	 * @param reporters
	 * @return
	 * @throws Exception
	 */
	private static List<StepType> execSteps(ThreadGroupsInfo threadGroupsInfo,
			int threadGroupIndex, List<Reporter> reporters) throws Exception {
		StepsType steps = threadGroupsInfo.getThreadGroups()
				.get(threadGroupIndex).getSteps();
		Steps s = new Steps(threadGroupsInfo.getTestAutomatDTO(),
				threadGroupsInfo.getTestCaseDTO(), steps.getStep(),
				threadGroupsInfo.getLogBasePath());
		s.setReporters(reporters);
		s.run();

		return steps.getStep();
	}

	/**
	 * Gets the sequential number of a thread group in a step list. If the
	 * thread group cannot be found then the count of all thread groups is
	 * returned
	 * 
	 * @param steps
	 * @param threadGroup
	 * @param found
	 *            Wrapper for boolean (instance instead of value)
	 * @return
	 */
	private static int getThreadGroupCount(List<StepType> steps,
			ThreadGroupType threadGroup, Found found) {

		int count = 0;
		for (StepType step : steps) {
			// if loop
			if (step.getLoop() != null) {
				LoopType l = step.getLoop();
				count += getThreadGroupCount(l.getSteps().getStep(),
						threadGroup, found);
				if (found.isFound()) {
					return count;
				}
			} else if (step.getThreads() != null) { // if threads
				ThreadsType t = step.getThreads();
				// for each thread group
				for (ThreadGroupType tg : t.getThreadGroup()) {
					count++;
					if (tg == threadGroup) {
						found.setFound(true);
						return count;
					}
					// get count of containing thread groups (recursive call)
					count += getThreadGroupCount(tg.getSteps().getStep(),
							threadGroup, found);
					if (found.isFound()) {
						return count;
					}
				}
			}
		}
		found.setFound(false);
		return count;
	}

	/**
	 * Gets the thread group list of a thread group in a step list. The thread
	 * group is determined by the sequential number. If the thread group cannot
	 * be found then null is returned
	 * 
	 * @param steps
	 * @param threadGroupCount
	 * @return
	 */
	private static List<ThreadGroupType> getThreadGroups(List<StepType> steps,
			int threadGroupCount) {

		for (StepType step : steps) {
			// if loop
			if (step.getLoop() != null) {
				LoopType l = step.getLoop();
				// recursive call
				List<ThreadGroupType> threadGroups = getThreadGroups(l
						.getSteps().getStep(), threadGroupCount);
				if (threadGroups != null) {
					return threadGroups;
				}
			} else if (step.getThreads() != null) { // if threads
				ThreadsType t = step.getThreads();
				// for each thread group
				for (int i = 0; i < t.getThreadGroup().size(); i++) {
					ThreadGroupType threadGroup = t.getThreadGroup().get(i);
					threadGroupCount--;
					if (threadGroupCount == 0) {
						return t.getThreadGroup();
					}
					// recursive call
					List<ThreadGroupType> threadGroups = getThreadGroups(
							threadGroup.getSteps().getStep(), threadGroupCount);
					if (threadGroups != null) {
						return threadGroups;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Moves the reports from a step list to another step list. Reports in
	 * embedded step lists are also moved
	 * 
	 * @param sourceSteps
	 *            source step list
	 * @param destSteps
	 *            destination step list with the same structure as the source
	 *            step list
	 */
	private static void moveReports(List<StepType> sourceSteps,
			List<StepType> destSteps) {
		int passNoDiff = Steps.getMaxPassNo(destSteps) + 1;
		// for each source step
		for (int i = 0; i < sourceSteps.size(); i++) {
			StepType sourceStep = sourceSteps.get(i);
			StepType destStep = destSteps.get(i);
			// if reports exist for the step itself
			if (sourceStep.getReports() != null) {
				// increase pass numbers for reports
				for (StepReportType report : sourceStep.getReports()
						.getReport()) {
					report.setPassNo(report.getPassNo() + passNoDiff);
				}
				// move reports to destination
				if (destStep.getReports() == null) {
					destStep.setReports(new StepReportsType());
				}
				List<StepReportType> l = new ArrayList<>(destStep.getReports()
						.getReport());
				l.addAll(sourceStep.getReports().getReport());
				destStep.getReports().getReport().clear();
				destStep.getReports().getReport().addAll(l);
				sourceStep.setReports(null);
			}
			// if module
			if (sourceStep.getModule() != null) {
				ModuleType srcModule = sourceStep.getModule();
				// if reports exist
				if (srcModule.getReports() != null) {
					// increase pass numbers for reports
					for (ModuleReportType report : srcModule.getReports()
							.getReport()) {
						report.setPassNo(report.getPassNo() + passNoDiff);
					}
					// move reports to destination
					ModuleType destModule = destStep.getModule();
					if (destModule.getReports() == null) {
						destModule.setReports(new ModuleReportsType());
					}
					List<ModuleReportType> l = new ArrayList<>(destModule
							.getReports().getReport());
					l.addAll(srcModule.getReports().getReport());
					destModule.getReports().getReport().clear();
					destModule.getReports().getReport().addAll(l);
					srcModule.setReports(null);
				}
			} else if (sourceStep.getLoop() != null) { // if loop
				LoopType sourceLoop = sourceStep.getLoop();
				LoopType destLoop = destStep.getLoop();
				// recursive call
				moveReports(sourceLoop.getSteps().getStep(), destLoop
						.getSteps().getStep());
			} else if (sourceStep.getThreads() != null) { // if threads
				ThreadsType sourceThreads = sourceStep.getThreads();
				ThreadsType destThreads = destStep.getThreads();
				// for each thread group
				for (int j = 0; j < sourceThreads.getThreadGroup().size(); j++) {
					// recursive call
					moveReports(sourceThreads.getThreadGroup().get(j)
							.getSteps().getStep(), destThreads.getThreadGroup()
							.get(j).getSteps().getStep());
				}
			}
		}
	}

	/**
	 * Removes the existing reports from a step list. Reports in embedded step
	 * lists are also removed
	 * 
	 * @param steps
	 */
	private static void removeReports(List<StepType> steps) {
		for (StepType step : steps) {
			step.setReports(null);
			if (step.getModule() != null) {
				ModuleType m = step.getModule();
				// remove reports
				m.setReports(null);
			} else if (step.getLoop() != null) { // if loop
				LoopType l = step.getLoop();
				// recursive call
				removeReports(l.getSteps().getStep());
			} else if (step.getThreads() != null) { // if threads
				ThreadsType t = step.getThreads();
				// for each thread group
				for (ThreadGroupType threadGroup : t.getThreadGroup()) {
					// recursive call
					removeReports(threadGroup.getSteps().getStep());
				}
			}
		}
	}

	/**
	 * Creates a deep clone of an object (realized with binary serializer)
	 * 
	 * @param a
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	private static <T> T deepClone(T a) throws IOException,
			ClassNotFoundException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);
		out.writeObject(a);
		out.flush();
		out.close();
		// Make an input stream from the byte array and read
		// a copy of the object back in.
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
				bos.toByteArray()));
		return (T) in.readObject();
	}

	private static class Found {
		boolean found;

		public boolean isFound() {
			return found;
		}

		public void setFound(boolean found) {
			this.found = found;
		}
	}

	private static class ThreadGroupRunner implements Runnable {
		private StepsRunner[] stepsRunners;
		private Exception exception;

		public StepsRunner[] getStepsRunners() {
			return stepsRunners;
		}

		public Exception getException() {
			return exception;
		}

		private final ThreadGroupsInfo threadGroupsInfo;
		private final int threadGroupIndex;
		private final int parentThreadId;
		private final List<Reporter> reporters;

		public ThreadGroupRunner(ThreadGroupsInfo threadGroupsInfo,
				int threadGroupIndex, int parentThreadId,
				List<Reporter> reporters) {
			this.threadGroupsInfo = threadGroupsInfo;
			this.threadGroupIndex = threadGroupIndex;
			this.parentThreadId = parentThreadId;
			this.reporters = reporters;
		}

		public void run() {
			try {
				log.info("Thread group " + (threadGroupIndex + 1) + "/"
						+ threadGroupsInfo.getThreadGroups().size()
						+ " started (parentThreadId=" + parentThreadId + ")");
				stepsRunners = execThreadGroup(threadGroupsInfo,
						threadGroupIndex, reporters);
			} catch (Exception e) {
				exception = e;
			}
		}
	}

	private static class StepsRunner implements Runnable {
		private List<StepType> steps;
		private Exception exception;
		private final ThreadGroupsInfo threadGroupsInfo;
		private final int threadGroupIndex;
		private final int threadIndex;
		private final int parentThreadId;
		private final List<Reporter> reporters;

		public List<StepType> getSteps() {
			return steps;
		}

		public Exception getException() {
			return exception;
		}

		private void setException(Exception exception) {
			this.exception = exception;
		}

		public StepsRunner(ThreadGroupsInfo threadGroupsInfo,
				int threadGroupIndex, int threadIndex, int parentThreadId,
				List<Reporter> reporters) {
			this.threadGroupsInfo = threadGroupsInfo;
			this.threadGroupIndex = threadGroupIndex;
			this.threadIndex = threadIndex;
			this.parentThreadId = parentThreadId;
			this.reporters = reporters;
		}

		public void run() {
			try {
				ThreadGroupType threadGroup = threadGroupsInfo
						.getThreadGroups().get(threadGroupIndex);
				log.info("Thread " + (threadIndex + 1) + "/"
						+ threadGroup.getCount() + "started (parentThreadId="
						+ parentThreadId + ")");
				steps = execSteps(threadGroupsInfo, threadGroupIndex, reporters);
			} catch (Exception e) {
				setException(e);
			}
		}
	}

	private static class ThreadGroupsInfo {
		private TestAutomatDTO testAutomatDTO;
		private TestCaseDTO testCaseDTO;
		private List<ThreadGroupType> threadGroups;
		private String logBasePath;

		public TestAutomatDTO getTestAutomatDTO() {
			return testAutomatDTO;
		}

		public void setTestAutomatDTO(TestAutomatDTO testAutomatDTO) {
			this.testAutomatDTO = testAutomatDTO;
		}

		public TestCaseDTO getTestCaseDTO() {
			return testCaseDTO;
		}

		public void setTestCaseDTO(TestCaseDTO testCaseDTO) {
			this.testCaseDTO = testCaseDTO;
		}

		public List<ThreadGroupType> getThreadGroups() {
			return threadGroups;
		}

		public void setThreadGroups(List<ThreadGroupType> threadGroups) {
			this.threadGroups = threadGroups;
		}

		public String getLogBasePath() {
			return logBasePath;
		}

		public void setLogBasePath(String logBasePath) {
			this.logBasePath = logBasePath;
		}
	}

}
