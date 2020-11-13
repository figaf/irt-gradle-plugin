package com.figaf.plugin;

import com.figaf.plugin.tasks.TestSuiteRunner;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author Arsenii Istlentev
 */
public class IrtPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        IrtPluginExtension irtPluginExtension = project.getExtensions().create("irtPlugin", IrtPluginExtension.class, project);

        project.getTasks().register("runTestSuite", TestSuiteRunner.class, testSuiteRunner -> {
            testSuiteRunner.setDeploymentType(irtPluginExtension.getDeploymentType().getOrElse("on-premise"));
            testSuiteRunner.setTestSuiteId(irtPluginExtension.getTestSuiteId().getOrNull());
            testSuiteRunner.setTestSuiteName(irtPluginExtension.getTestSuiteName().getOrNull());
            testSuiteRunner.setUrl(irtPluginExtension.getUrl().getOrNull());
            testSuiteRunner.setClientId(irtPluginExtension.getClientId().getOrNull());
            testSuiteRunner.setClientSecret(irtPluginExtension.getClientSecret().getOrNull());
            Long delayBeforePolling = irtPluginExtension.getDelayBeforePolling().getOrNull();
            if (delayBeforePolling == null || delayBeforePolling == 0) {
                delayBeforePolling = 15000L;
            }
            testSuiteRunner.setDelayBeforePolling(delayBeforePolling);
            testSuiteRunner.setSynchronizeBeforeRunningTestSuite(irtPluginExtension.getSynchronizeBeforeRunningTestSuite().getOrElse(true));
            testSuiteRunner.setTestSystemId(irtPluginExtension.getTestSystemId().getOrNull());
        });
    }
}
