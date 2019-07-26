package com.figaf.plugin;

import com.figaf.plugin.tasks.TestSuitRunner;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author Arsenii Istlentev
 */
public class IrtPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        IrtPluginExtension irtPluginExtension = project.getExtensions().create("irtPlugin", IrtPluginExtension.class, project);

        project.getTasks().register("runTestSuit", TestSuitRunner.class, testSuitRunner -> {
            testSuitRunner.setDeploymentType(irtPluginExtension.getDeploymentType().getOrElse("on-premise"));
            testSuitRunner.setTestSuitId(irtPluginExtension.getTestSuitId().getOrNull());
            testSuitRunner.setTestSuitName(irtPluginExtension.getTestSuitName().getOrNull());
            testSuitRunner.setUrl(irtPluginExtension.getUrl().getOrNull());
            testSuitRunner.setClientId(irtPluginExtension.getClientId().getOrNull());
            testSuitRunner.setClientSecret(irtPluginExtension.getClientSecret().getOrNull());
            Long delayBeforePolling = irtPluginExtension.getDelayBeforePolling().getOrNull();
            if (delayBeforePolling == null || delayBeforePolling == 0) {
                delayBeforePolling = 15000L;
            }
            testSuitRunner.setDelayBeforePolling(delayBeforePolling);
            testSuitRunner.setSynchronizeBeforeRunningTestSuit(irtPluginExtension.getSynchronizeBeforeRunningTestSuit().getOrElse(true));
        });
    }
}
