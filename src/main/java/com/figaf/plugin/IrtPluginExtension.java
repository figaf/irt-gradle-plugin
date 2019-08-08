package com.figaf.plugin;

import lombok.Getter;
import lombok.ToString;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

/**
 * @author Arsenii Istlentev
 */
@Getter
@ToString
public class IrtPluginExtension {

    private final Property<String> deploymentType;
    private final Property<String> url;
    private final Property<String> clientId;
    private final Property<String> clientSecret;
    private final Property<String> testSuiteId;
    private final Property<String> testSuiteName;
    private final Property<Long> delayBeforePolling;
    private final Property<Boolean> synchronizeBeforeRunningTestSuite;

    public IrtPluginExtension(Project project) {
        this.deploymentType = project.getObjects().property(String.class);
        this.url = project.getObjects().property(String.class);
        this.clientId = project.getObjects().property(String.class);
        this.clientSecret = project.getObjects().property(String.class);
        this.testSuiteId = project.getObjects().property(String.class);
        this.testSuiteName = project.getObjects().property(String.class);
        this.delayBeforePolling = project.getObjects().property(Long.class);
        this.synchronizeBeforeRunningTestSuite = project.getObjects().property(Boolean.class);
    }
}
