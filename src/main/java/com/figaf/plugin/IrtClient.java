package com.figaf.plugin;

import com.figaf.plugin.entities.SimpleIntegrationObject;
import com.figaf.plugin.entities.SimpleSynchronizationResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author Arsenii Istlentev
 */
public class IrtClient {

    private final String deploymentType;
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private String token;
    private HttpClient client;

    public IrtClient(String deploymentType, String baseUrl, String clientId, String clientSecret) {
        this.deploymentType = deploymentType;
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.client = HttpClients.custom().build();
    }

    public void authorize() {
        requestTokenForTheClient();
    }

    public String getTestSuiteIdByName(String testSuiteName) {
        return getTestSuiteIdByName(testSuiteName, true);
    }

    public List<String> getTestCaseIdsByTestSuiteId(String testSuiteId) {
        return getTestCaseIdsByTestSuiteId(testSuiteId, true);
    }

    public List<SimpleIntegrationObject> getTestObjectIdsByTestCaseId(String testCaseId) {
        return getTestObjectIdsByTestCaseId(testCaseId, true);
    }

    public SimpleSynchronizationResult syncIflows(String agentId, Set<String> testObjectIds) {
        return syncIFlows(agentId, testObjectIds, true);
    }

    public SimpleSynchronizationResult getSynchronizationResult(String agentId, String synchronizationResultId) {
        return getSynchronizationResult(agentId, synchronizationResultId, true);
    }

    public String runTestSuite(String testSuiteId) {
        return runTestSuite(testSuiteId, true);
    }

    public String pollMessages(String testTemplateRunId) {
        return pollMessages(testTemplateRunId, true);
    }

    public String getPollingResult(String pollingRequestId) {
        return getPollingResult(pollingRequestId, true);
    }

    public String getTestSuiteRunLastResult(String testSuiteId) {
        return getTestSuiteRunLastResult(testSuiteId, true);
    }

    private void requestTokenForTheClient() {
        try {
            System.out.println("requestTokenForTheClient");
            String uri;
            if ("on-premise".equals(deploymentType)) {
                uri = String.format("%s/oauth/token", baseUrl);
            } else if ("cloud".equals(deploymentType)) {
                uri = String.format("%s/auth/oauth/token", baseUrl);
            } else {
                throw new RuntimeException(String.format("Deployment type %s is not supported", deploymentType));
            }

            HttpPost post = new HttpPost(uri);

            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
            urlParameters.add(new BasicNameValuePair("scope", "irt-gradle-plugin"));

            post.setHeader("Content-type", "application/x-www-form-urlencoded");
            post.setHeader(createBasicAuthHeader());
            post.setEntity(new UrlEncodedFormEntity(urlParameters));

            HttpResponse httpResponse = client.execute(post);

            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseString = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
            System.out.println("get token response: " + responseString);
            if (statusCode == 200) {
                JSONObject jsonResponse = new JSONObject(responseString);
                token = jsonResponse.getString("access_token");
            } else {
                throw new RuntimeException(String.format("Cannot run test suitee.\n Code %d, message: %s", statusCode, responseString));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while trying to get token: " + ex.getMessage(), ex);
        }
    }

    private String getTestSuiteIdByName(String testSuiteName, boolean firstAttempt) {
        try {
            String url = String.format("%s/api/v1/testing-template/search", baseUrl);
            JSONObject requestBody = new JSONObject();
            requestBody.put("title", testSuiteName);
            HttpPostRequestExecutor httpPostRequestExecutor = new HttpPostRequestExecutor(url, requestBody).invoke();
            int statusCode = httpPostRequestExecutor.getStatusCode();
            String responseString = httpPostRequestExecutor.getResponseString();
            System.out.println("test suite search response: " + responseString);

            switch (statusCode) {
                case 200: {
                    JSONArray jsonArray = new JSONArray(responseString);
                    if (jsonArray.length() == 0) {
                        throw new RuntimeException(String.format("Test Suite with name %s is not found", testSuiteName));
                    }
                    return jsonArray.getJSONObject(0).getString("id");
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return getTestSuiteIdByName(testSuiteName, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot search test suite.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while searching test suite: " + ex.getMessage(), ex);
        }
    }

    private List<String> getTestCaseIdsByTestSuiteId(String testSuiteId, boolean firstAttempt) {
        try {
            String url = String.format("%s/api/v1/testing-template/%s/test-cases", baseUrl, testSuiteId);
            HttpGetRequestExecutor httpGetRequestExecutor = new HttpGetRequestExecutor(url).invoke();
            int statusCode = httpGetRequestExecutor.getStatusCode();
            String responseString = httpGetRequestExecutor.getResponseString();

            switch (statusCode) {
                case 200: {
                    JSONArray jsonArray = new JSONArray(responseString);
                    if (jsonArray.length() == 0) {
                        throw new RuntimeException(String.format("Test Cases attached to Test Suite %s are not found", testSuiteId));
                    }
                    List<String> testCaseIds = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        testCaseIds.add(jsonObject.getString("id"));
                    }
                    return testCaseIds;
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return getTestCaseIdsByTestSuiteId(testSuiteId, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot find Test Cases attached to the Test Suite.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while searching Test Cases attached to the Test Suite: " + ex.getMessage(), ex);
        }
    }

    private List<SimpleIntegrationObject> getTestObjectIdsByTestCaseId(String testCaseId, boolean firstAttempt) {
        try {
            String url = String.format("%s/api/v1/test-case/%s/linked-integration-objects", baseUrl, testCaseId);
            HttpGetRequestExecutor httpGetRequestExecutor = new HttpGetRequestExecutor(url).invoke();
            int statusCode = httpGetRequestExecutor.getStatusCode();
            String responseString = httpGetRequestExecutor.getResponseString();

            switch (statusCode) {
                case 200: {
                    JSONArray jsonArray = new JSONArray(responseString);
                    if (jsonArray.length() == 0) {
                        throw new RuntimeException(String.format("Test Objects attached to Test Case %s are not found", testCaseId));
                    }
                    List<SimpleIntegrationObject> testObjects = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        SimpleIntegrationObject testObject = new SimpleIntegrationObject();
                        testObject.setId(jsonObject.getString("id"));
                        testObject.setAgentId(jsonObject.getString("agentId"));
                        testObjects.add(testObject);
                    }
                    return testObjects;
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return getTestObjectIdsByTestCaseId(testCaseId, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot find Test Objects attached to the Test Case.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while searching Test Objects attached to the Test Case: " + ex.getMessage(), ex);
        }
    }

    private SimpleSynchronizationResult syncIFlows(String agentId, Set<String> testObjectIds, boolean firstAttempt) {
        try {
            String url = String.format("%s/api/v1/ctt/agent/%s/sync-iflows", baseUrl, agentId);
            JSONObject requestBody = new JSONObject();
            requestBody.put("iflowIds", Arrays.asList(testObjectIds));
            HttpPostRequestExecutor httpPostRequestExecutor = new HttpPostRequestExecutor(url, requestBody).invoke();
            int statusCode = httpPostRequestExecutor.getStatusCode();
            String responseString = httpPostRequestExecutor.getResponseString();

            switch (statusCode) {
                case 200: {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    SimpleSynchronizationResult synchronizationResult = new SimpleSynchronizationResult();
                    synchronizationResult.setSynchronizationResultId(jsonResponse.getString("synchronizationResultId"));
                    synchronizationResult.setStageTitle(jsonResponse.getJSONObject("stage").getString("title"));
                    return synchronizationResult;
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return syncIFlows(agentId, testObjectIds, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot synchronize iflows\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while synchronizing iflows: " + ex.getMessage(), ex);
        }
    }

    private SimpleSynchronizationResult getSynchronizationResult(String agentId, String synchronizationResultId, boolean firstAttempt) {
        try {
            String url = String.format("%s/api/v1/ctt/agent/%s/sync/%s", baseUrl, agentId, synchronizationResultId);
            HttpGetRequestExecutor httpGetRequestExecutor = new HttpGetRequestExecutor(url).invoke();
            int statusCode = httpGetRequestExecutor.getStatusCode();
            String responseString = httpGetRequestExecutor.getResponseString();

            switch (statusCode) {
                case 200: {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    SimpleSynchronizationResult synchronizationResult = new SimpleSynchronizationResult();
                    synchronizationResult.setSynchronizationResultId(jsonResponse.getString("synchronizationResultId"));
                    synchronizationResult.setStageTitle(jsonResponse.getJSONObject("stage").getString("title"));
                    return synchronizationResult;
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return getSynchronizationResult(agentId, synchronizationResultId, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot get synchronization result.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while getting synchronization resutls: " + ex.getMessage(), ex);
        }
    }

    private String runTestSuite(String testSuiteId, boolean firstAttempt) {
        try {
            String url = String.format("%s/api/v1/testing-template/run", baseUrl);
            JSONObject requestBody = new JSONObject();
            requestBody.put("testingTemplateIds", Arrays.asList(testSuiteId));
            HttpPostRequestExecutor httpPostRequestExecutor = new HttpPostRequestExecutor(url, requestBody).invoke();
            int statusCode = httpPostRequestExecutor.getStatusCode();
            String responseString = httpPostRequestExecutor.getResponseString();
            System.out.println("run test suite response: " + responseString);

            switch (statusCode) {
                case 200: {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    return (String) jsonResponse.getJSONArray("testingTemplateRunIds").get(0);
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return runTestSuite(testSuiteId, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot run test suite.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while running test suite: " + ex.getMessage(), ex);
        }
    }

    private String pollMessages(String testTemplateRunId, boolean firstAttempt) {
        try {
            String url = String.format("%s/api/v1/polling/poll-messages", baseUrl);
            JSONObject requestBody = new JSONObject();
            requestBody.put("testingTemplateRunIds", Arrays.asList(testTemplateRunId));
            HttpPostRequestExecutor httpPostRequestExecutor = new HttpPostRequestExecutor(url, requestBody).invoke();
            int statusCode = httpPostRequestExecutor.getStatusCode();
            String responseString = httpPostRequestExecutor.getResponseString();
            System.out.println("poll messages response: " + responseString);

            switch (statusCode) {
                case 200: {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    return jsonResponse.getString("pollingRequestId");
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return pollMessages(testTemplateRunId, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot poll messages.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while polling messages: " + ex.getMessage(), ex);
        }
    }

    private String getPollingResult(String pollingRequestId, boolean firstAttempt) {
        try {
            String url = String.format("%s/api/v1/polling/result/%s", baseUrl, pollingRequestId);
            HttpGetRequestExecutor httpGetRequestExecutor = new HttpGetRequestExecutor(url).invoke();
            int statusCode = httpGetRequestExecutor.getStatusCode();
            String responseString = httpGetRequestExecutor.getResponseString();
            System.out.println("get polling result response: " + responseString);

            switch (statusCode) {
                case 200: {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    return jsonResponse.getString("status");
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return getPollingResult(pollingRequestId, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot get polling results.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while getting polling results: " + ex.getMessage(), ex);
        }
    }

    private String getTestSuiteRunLastResult(String testSuiteId, boolean firstAttempt) {
        try {
            String url = String.format("%s/api/v1/testing-template-run/%s/last-result", baseUrl, testSuiteId);
            HttpGetRequestExecutor httpGetRequestExecutor = new HttpGetRequestExecutor(url).invoke();
            int statusCode = httpGetRequestExecutor.getStatusCode();
            String responseString = httpGetRequestExecutor.getResponseString();
            System.out.println("get test suite run last result: " + responseString);

            switch (statusCode) {
                case 200: {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    return jsonResponse.getString("resultStatus");
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return getTestSuiteRunLastResult(testSuiteId, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot get test suite run last result.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while getting test suite run last result: " + ex.getMessage(), ex);
        }
    }

    private Header createBasicAuthHeader() {
        return new BasicHeader(
            "Authorization",
            String.format(
                "Basic %s",
                Base64.encodeBase64String(
                    (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }

    private class HttpPostRequestExecutor {
        private String url;
        private JSONObject requestBody;
        private int statusCode;
        private String responseString;

        public HttpPostRequestExecutor(String url, JSONObject requestBody) {
            this.url = url;
            this.requestBody = requestBody;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseString() {
            return responseString;
        }

        public HttpPostRequestExecutor invoke() throws IOException {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", String.format("Bearer %s", token));
            post.setEntity(new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON));
            HttpResponse httpResponse = client.execute(post);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            responseString = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
            return this;
        }
    }

    private class HttpGetRequestExecutor {
        private String url;
        private int statusCode;
        private String responseString;

        public HttpGetRequestExecutor(String url) {
            this.url = url;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseString() {
            return responseString;
        }

        public HttpGetRequestExecutor invoke() throws IOException {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", String.format("Bearer %s", token));
            HttpResponse httpResponse = client.execute(httpGet);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            responseString = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
            return this;
        }
    }
}
