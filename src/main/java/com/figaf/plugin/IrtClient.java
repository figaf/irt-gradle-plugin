package com.figaf.plugin;

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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public String getTestSuitIdByName(String testSuitName) {
        return getTestSuitIdByName(testSuitName, true);
    }

    public String runTestSuit(String testSuitId) {
        return runTestSuit(testSuitId, true);
    }

    public String pollMessages(String testTemplateRunId) {
        return pollMessages(testTemplateRunId, true);
    }

    public String getPollingResult(String pollingRequestId) {
        return getPollingResult(pollingRequestId, true);
    }

    public String getTestSuitRunLastResult(String testSuitId) {
        return getTestSuitRunLastResult(testSuitId, true);
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
            urlParameters.add(new BasicNameValuePair("scope", "public_api"));

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
                throw new RuntimeException(String.format("Cannot run test suit.\n Code %d, message: %s", statusCode, responseString));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while trying to get token: " + ex.getMessage(), ex);
        }
    }

    private String getTestSuitIdByName(String testSuitName, boolean firstAttempt) {
        try {
            String url = baseUrl + "/api/v1/testing-template/search";
            JSONObject requestBody = new JSONObject();
            requestBody.put("title", testSuitName);
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", String.format("Bearer %s", token));
            post.setEntity(new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON));
            HttpResponse httpResponse = client.execute(post);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseString = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
            System.out.println("test suit search response: " + responseString);

            switch (statusCode) {
                case 200: {
                    JSONArray jsonArray = new JSONArray(responseString);
                    if (jsonArray.length() == 0) {
                        throw new RuntimeException(String.format("Test suit with name %s is not found", testSuitName));
                    }
                    return jsonArray.getJSONObject(0).getString("id");
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return getTestSuitIdByName(testSuitName, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot search test suit.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while searching test suit: " + ex.getMessage(), ex);
        }
    }

    private String runTestSuit(String testSuitId, boolean firstAttempt) {
        try {
            String url = baseUrl + "/api/v1/testing-template/run";
            JSONObject requestBody = new JSONObject();
            requestBody.put("testingTemplateIds", Arrays.asList(testSuitId));
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", String.format("Bearer %s", token));
            post.setEntity(new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON));
            HttpResponse httpResponse = client.execute(post);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseString = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
            System.out.println("run test suit response: " + responseString);

            switch (statusCode) {
                case 200: {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    return (String) jsonResponse.getJSONArray("testingTemplateRunIds").get(0);
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return runTestSuit(testSuitId, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot run test suit.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while running test suit: " + ex.getMessage(), ex);
        }
    }

    private String pollMessages(String testTemplateRunId, boolean firstAttempt) {
        try {
            String url = baseUrl + "/api/v1/polling/poll-messages";
            JSONObject requestBody = new JSONObject();
            requestBody.put("testingTemplateRunIds", Arrays.asList(testTemplateRunId));
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", String.format("Bearer %s", token));
            post.setEntity(new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON));
            HttpResponse httpResponse = client.execute(post);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseString = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
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
            String url = baseUrl + "/api/v1/polling/result/" + pollingRequestId;
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", String.format("Bearer %s", token));
            HttpResponse httpResponse = client.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseString = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
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

    private String getTestSuitRunLastResult(String testSuitId, boolean firstAttempt) {
        try {
            String url = baseUrl + "/api/v1/testing-template-run/" + testSuitId + "/last-result";
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", String.format("Bearer %s", token));
            HttpResponse httpResponse = client.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseString = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
            System.out.println("get test suit run last result: " + responseString);

            switch (statusCode) {
                case 200: {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    return jsonResponse.getString("resultStatus");
                }
                case 401: {
                    if (firstAttempt) {
                        requestTokenForTheClient();
                        return getTestSuitRunLastResult(testSuitId, false);
                    }
                }
                default: {
                    throw new RuntimeException(String.format("Cannot get test suit run last result.\n Code %d, message: %s", statusCode, responseString));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error occurred while getting test suit run last result: " + ex.getMessage(), ex);
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
}
