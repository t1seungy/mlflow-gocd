package com.indix.mlflow_gocd;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

@Extension
public class MLFlowPackagePlugin implements GoPlugin {
    public static String MLFLOW_URL = "MLFLOW_URL";
    public static String PROMOTION_TAG_NAME = "PROMOTION_TAG_NAME";
    public static String PROMOTION_TAG_VALUE = "PROMOTION_TAG_VALUE";
    public static String EXPERIMENT_ID = "EXPERIMENT_ID";

    public static final String REQUEST_REPOSITORY_CONFIGURATION = "repository-configuration";
    public static final String REQUEST_PACKAGE_CONFIGURATION = "package-configuration";
    public static final String REQUEST_VALIDATE_REPOSITORY_CONFIGURATION = "validate-repository-configuration";
    public static final String REQUEST_VALIDATE_PACKAGE_CONFIGURATION = "validate-package-configuration";
    public static final String REQUEST_CHECK_REPOSITORY_CONNECTION = "check-repository-connection";
    public static final String REQUEST_CHECK_PACKAGE_CONNECTION = "check-package-connection";
    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISION_SINCE = "latest-revision-since";

    private static final String MLFLOW_GET_EXPERIMENT_ENDPOINT="/api/2.0/preview/mlflow/experiments/get";

    private static Logger logger = Logger.getLoggerFor(MLFlowPackagePlugin.class);

    private final HttpRequestFactory requestFactory;

    public MLFlowPackagePlugin() {
        NetHttpTransport.Builder builder = new NetHttpTransport.Builder();
        this.requestFactory = builder.build().createRequestFactory();
    }

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {

    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) throws UnhandledRequestTypeException {
        if (goPluginApiRequest.requestName().equals(REQUEST_REPOSITORY_CONFIGURATION)) {
            return handleRepositoryConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_PACKAGE_CONFIGURATION)) {
            return handlePackageConfiguration();
        } else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_REPOSITORY_CONFIGURATION)) {
            return handleRepositoryValidation(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_VALIDATE_PACKAGE_CONFIGURATION)) {
            return handlePackageValidation(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECK_REPOSITORY_CONNECTION)) {
            return handleRepositoryCheckConnection(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_CHECK_PACKAGE_CONNECTION)) {
            return handlePackageCheckConnection(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISION)) {
            return handleGetLatestRevision(goPluginApiRequest);
        } else if (goPluginApiRequest.requestName().equals(REQUEST_LATEST_REVISION_SINCE)) {
            return handleLatestRevisionSince(goPluginApiRequest);
        }
        return null;
    }

    private GoPluginApiResponse handleLatestRevisionSince(GoPluginApiRequest goPluginApiRequest) {
        return null;
    }

    private GoPluginApiResponse handleGetLatestRevision(GoPluginApiRequest goPluginApiRequest) {
        return null;
    }

    private GoPluginApiResponse handlePackageCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryConfig = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);
        final Map<String, String> packageConfig = keyValuePairs(goPluginApiRequest, REQUEST_PACKAGE_CONFIGURATION);

        String mlflowUrl = repositoryConfig.get(MLFLOW_URL);
        String experimentId = packageConfig.get(EXPERIMENT_ID);

        MaterialResult result;
        if(StringUtils.isBlank(mlflowUrl)) {
            result = new MaterialResult(false, "Experiment id must be specified");
        } else {
            try {
                GenericUrl getExperimentUrl = new GenericUrl(String.format("%s%s?experiment_id=%s", mlflowUrl, MLFLOW_GET_EXPERIMENT_ENDPOINT, experimentId));
                HttpResponse response = requestFactory.buildGetRequest(getExperimentUrl).execute();
                if (response.getStatusCode() != 200) {
                    result = new MaterialResult(false, String.format("Experiment %s not found", experimentId));
                } else {
                    result = new MaterialResult(true, "Success");
                }
            } catch(IOException ex) {
                result = new MaterialResult(false, String.format("Unable to reach MLFlow at %s - %s", mlflowUrl, ex.getMessage()));
                logger.error("Unable to reach mlflow", ex);
            }
        }

        return createResponse(result.responseCode(), result.toMap());
    }

    private GoPluginApiResponse handleRepositoryCheckConnection(GoPluginApiRequest goPluginApiRequest) {
        final Map<String, String> repositoryConfig = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);

        MaterialResult result;
        String mlflowUrl = repositoryConfig.get(MLFLOW_URL);

        if(StringUtils.isBlank(mlflowUrl)) {
            result = new MaterialResult(false, "MLFlow url must be specified");
        } else {
            try {
                HttpResponse response = requestFactory.buildGetRequest(new GenericUrl(mlflowUrl)).execute();
                if (response.getStatusCode() != 200) {
                    result = new MaterialResult(false, String.format("Unable to reach MLFlow at %s", mlflowUrl));
                } else {
                    result = new MaterialResult(true, "Success");
                }
            } catch(IOException ex) {
                result = new MaterialResult(false, String.format("Unable to reach MLFlow at %s - %s", mlflowUrl, ex.getMessage()));
                logger.error("Unable to reach mlflow", ex);
            }
        }

        return createResponse(result.responseCode(), result.toMap());
    }

    private GoPluginApiResponse handlePackageValidation(GoPluginApiRequest goPluginApiRequest) {
        List<Map<String, Object>> validationResult = new ArrayList<>();
        final Map<String, String> packageConfig = keyValuePairs(goPluginApiRequest, REQUEST_PACKAGE_CONFIGURATION);

        String experimentId = packageConfig.get(EXPERIMENT_ID);
        if(StringUtils.isBlank(experimentId)) {
            addError(EXPERIMENT_ID, "Experiment ID must be specified", validationResult);
        }

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, validationResult);
    }

    private GoPluginApiResponse handleRepositoryValidation(GoPluginApiRequest goPluginApiRequest) {
        List<Map<String, Object>> validationResult = new ArrayList<>();
        final Map<String, String> repositoryConfig = keyValuePairs(goPluginApiRequest, REQUEST_REPOSITORY_CONFIGURATION);

        String mlflowUrl = repositoryConfig.get(MLFLOW_URL);
        if(StringUtils.isBlank(mlflowUrl)) {
            addError(MLFLOW_URL, "MLFlow URL must be specified", validationResult);
        }

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, validationResult);
    }

    private GoPluginApiResponse handlePackageConfiguration() {
        Map<String, Object> response = new HashMap<>();
        response.put(EXPERIMENT_ID,
                createField("Experiment ID", null, true, true, false, "1")
        );
        response.put(PROMOTION_TAG_NAME,
                createField("Promotion Tag Name", "promote", true, false, false, "2")
        );
        response.put(PROMOTION_TAG_VALUE,
                createField("Promotion Tag Value", "true", true, false, false, "3")
        );
        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response);
    }

    private GoPluginApiResponse handleRepositoryConfiguration() {
        Map<String, Object> response = new HashMap<>();
        response.put(MLFLOW_URL,
                createField("MLFlow URL", null, true, true, false, "1")
        );
        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, response);
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("package-repository", Arrays.asList("1.0"));
    }

    private List<Map<String, Object>> addError(String key, String message, List<Map<String, Object>> validationResult) {
        HashMap<String, Object> errorMap = new HashMap<>();
        errorMap.put("key", key);
        errorMap.put("message", message);
        validationResult.add(errorMap);
        return validationResult;
    }

    private GoPluginApiResponse createResponse(int responseCode, Object body) {
        final DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(responseCode);
        response.setResponseBody(new GsonBuilder().serializeNulls().create().toJson(body));
        return response;
    }

    private Map<String, Object> createField(String displayName, String defaultValue, boolean isPartOfIdentity, boolean isRequired, boolean isSecure, String displayOrder) {
        Map<String, Object> fieldProperties = new HashMap<>();
        fieldProperties.put("display-name", displayName);
        fieldProperties.put("default-value", defaultValue);
        fieldProperties.put("part-of-identity", isPartOfIdentity);
        fieldProperties.put("required", isRequired);
        fieldProperties.put("secure", isSecure);
        fieldProperties.put("display-order", displayOrder);
        return fieldProperties;
    }

    private Map<String, Object> getMapFor(GoPluginApiRequest goPluginApiRequest, String field) {
        Map<String, Object> map = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);
        Map<String, Object> fieldProperties = (Map<String, Object>) map.get(field);
        return fieldProperties;
    }

    private Map<String, String> keyValuePairs(GoPluginApiRequest goPluginApiRequest, String mainKey) {
        Map<String, String> keyValuePairs = new HashMap<>();
        Map<String, Object> map = (Map<String, Object>) new GsonBuilder().create().fromJson(goPluginApiRequest.requestBody(), Object.class);
        Map<String, Object> fieldsMap = (Map<String, Object>) map.get(mainKey);
        for (String field : fieldsMap.keySet()) {
            Map<String, Object> fieldProperties = (Map<String, Object>) fieldsMap.get(field);
            String value = (String) fieldProperties.get("value");
            keyValuePairs.put(field, value);
        }
        return keyValuePairs;
    }
}