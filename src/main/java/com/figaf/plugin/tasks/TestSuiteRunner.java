package com.figaf.plugin.tasks;

import com.figaf.plugin.IrtClient;
import com.figaf.plugin.entities.SimpleIntegrationObject;
import com.figaf.plugin.entities.SimpleSynchronizationResult;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.*;

/**
 * @author Arsenii Istlentev
 */
@Setter
public class TestSuiteRunner extends DefaultTask {

    private static final int MAX_NUMBER_OF_ITERATIONS_FOR_POLLING = 150;
    private static final long SLEEP_TIME_FOR_POLLING = 2000L;

    private static final int MAX_NUMBER_OF_ITERATION_FOR_TEST_RUN_RESULTS = 12;
    private static final long SLEEP_TIME_FOR_TEST_RUN_RESULTS = 5000L;

    @Input
    private String deploymentType;

    @Input
    private String url;

    @Input
    private String clientId;

    @Input
    private String clientSecret;

    @Input
    private String testSuiteId;

    @Input
    private String testSuiteName;

    @Input
    private Long delayBeforePolling;

    @Input
    private Boolean synchronizeBeforeRunningTestSuite;

    @Input
    private String testSystemId;

    @TaskAction
    public void taskAction() {
        try {
            System.out.println("deploymentType = " + deploymentType);
            System.out.println("url = " + url);
            System.out.println("clientId = " + clientId);
            System.out.println("testSuiteId = " + testSuiteId);
            System.out.println("testSuiteName = " + testSuiteName);
            System.out.println("delayBeforePolling = " + delayBeforePolling);
            System.out.println("synchronizeBeforeRunningTestSuite = " + synchronizeBeforeRunningTestSuite);
            System.out.println("testSystemId = " + testSystemId);

            if (StringUtils.isEmpty(testSuiteId) && StringUtils.isEmpty(testSuiteName)) {
                System.out.println("testSuiteId and testSuiteName are not provided!");
                return;
            }

            IrtClient irtClient = new IrtClient(deploymentType, url, clientId, clientSecret);
            irtClient.authorize();

            if (StringUtils.isEmpty(testSuiteId)) {
                testSuiteId = irtClient.getTestSuiteIdByName(testSuiteName);
            }

            if (synchronizeBeforeRunningTestSuite) {
                List<String> testCaseIds = irtClient.getTestCaseIdsByTestSuiteId(testSuiteId);
                Map<String, Set<String>> agentIdToTestObjects = new HashMap<>();
                for (String testCaseId : testCaseIds) {
                    List<SimpleIntegrationObject> testObjects = irtClient.getTestObjectIdsByTestCaseId(testCaseId);
                    for (SimpleIntegrationObject testObject : testObjects) {
                        if (!agentIdToTestObjects.containsKey(testObject.getAgentId())) {
                            agentIdToTestObjects.put(testObject.getAgentId(), new HashSet<>());
                        }
                        agentIdToTestObjects.get(testObject.getAgentId()).add(testObject.getId());
                    }
                }
                System.out.println("agentIdToTestObjects = " + agentIdToTestObjects);
                for (Map.Entry<String, Set<String>> entry : agentIdToTestObjects.entrySet()) {
                    String agentId = entry.getKey();
                    Set<String> testObjectIds = entry.getValue();
                    SimpleSynchronizationResult synchronizationResult = irtClient.syncIflows(agentId, testObjectIds);
                    System.out.println("synchronizationResult = " + synchronizationResult);
                    while (!"Synchronization finished".equals(synchronizationResult.getStageTitle())) {
                        Thread.sleep(5000L);
                        synchronizationResult = irtClient.getSynchronizationResult(agentId, synchronizationResult.getSynchronizationResultId());
                        System.out.println("synchronizationResult = " + synchronizationResult);
                    }
                }
            }

            String testingTemplateRunId = irtClient.runTestSuite(testSuiteId, testSystemId);

            Thread.sleep(delayBeforePolling);
            String pollingRequestId = irtClient.pollMessages(testingTemplateRunId);

            int numberOfIterations = 1;
            boolean pollingCompleted = false;
            while (!pollingCompleted && numberOfIterations <= MAX_NUMBER_OF_ITERATIONS_FOR_POLLING) {
                String pollingResultStatus = irtClient.getPollingResult(pollingRequestId);
                if (!"COMPLETED".equals(pollingResultStatus)) {
                    numberOfIterations++;
                    Thread.sleep(SLEEP_TIME_FOR_POLLING);
                } else {
                    pollingCompleted = true;
                }
            }
            if (!pollingCompleted) {
                throw new RuntimeException(String.format("Polling hasn't been completed within %d ms\n" +
                    "For more details visit %s/test-suite/%s/last-result", MAX_NUMBER_OF_ITERATIONS_FOR_POLLING * SLEEP_TIME_FOR_POLLING, url, testSuiteId));
            }

            numberOfIterations = 1;
            boolean testRunResultCompleted = false;
            String testingTemplateRunLastResult = null;
            while (!testRunResultCompleted && numberOfIterations <= MAX_NUMBER_OF_ITERATION_FOR_TEST_RUN_RESULTS) {
                testingTemplateRunLastResult = irtClient.getTestSuiteRunLastResult(testSuiteId);
                if ("UNFINISHED".equals(testingTemplateRunLastResult)) {
                    numberOfIterations++;
                    Thread.sleep(SLEEP_TIME_FOR_TEST_RUN_RESULTS);
                } else {
                    testRunResultCompleted = true;
                }
            }
            if (!"SUCCESS".equals(testingTemplateRunLastResult)) {
                throw new RuntimeException(String.format("Test was not successful. For more details visit %s/#/test-suite/%s/last-result", url, testSuiteId));
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
