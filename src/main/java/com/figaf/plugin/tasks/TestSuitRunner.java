package com.figaf.plugin.tasks;

import com.figaf.plugin.IrtClient;
import lombok.Setter;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Arsenii Istlentev
 */
@Setter
public class TestSuitRunner extends DefaultTask {

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
    private String testSuitId;

    @Input
    private String testSuitName;

    @Input
    private Long delayBeforePolling;

    @TaskAction
    public void taskAction() {
        try {
            System.out.println("deploymentType = " + deploymentType);
            System.out.println("url = " + url);
            System.out.println("clientId = " + clientId);
            System.out.println("testSuitId = " + testSuitId);
            System.out.println("testSuitName = " + testSuitName);
            System.out.println("delayBeforePolling = " + delayBeforePolling);

            if (testSuitId == null && testSuitName == null) {
                throw new RuntimeException("testSuitId or testSuitName must be provided");
            }

            IrtClient irtClient = new IrtClient(deploymentType, url, clientId, clientSecret);
            irtClient.authorize();

            if (testSuitId == null) {
                testSuitId = irtClient.getTestSuitIdByName(testSuitName);
            }

            String testingTemplateRunId = irtClient.runTestSuit(testSuitId);

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
                    "For more details visit %s/test-suit/%s/last-result", MAX_NUMBER_OF_ITERATIONS_FOR_POLLING * SLEEP_TIME_FOR_POLLING, url, testSuitId));
            }

            numberOfIterations = 1;
            boolean testRunResultCompleted = false;
            String testingTemplateRunLastResult = null;
            while (!testRunResultCompleted && numberOfIterations <= MAX_NUMBER_OF_ITERATION_FOR_TEST_RUN_RESULTS) {
                testingTemplateRunLastResult = irtClient.getTestSuitRunLastResult(testSuitId);
                if ("UNFINISHED".equals(testingTemplateRunLastResult)) {
                    numberOfIterations++;
                    Thread.sleep(SLEEP_TIME_FOR_TEST_RUN_RESULTS);
                } else {
                    testRunResultCompleted = true;
                }
            }
            if (!"SUCCESS".equals(testingTemplateRunLastResult)) {
                throw new RuntimeException(String.format("Test was not successful. For more details visit %s/test-suit/%s/last-result", url, testSuitId));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
