/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.Status.FAILURE;
import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.Status.SUCCESS;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.api.config.ComponentConfiguration;
import org.mule.runtime.module.launcher.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.launcher.TemporaryToolingArtifactBuilderFactory;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectionResult;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.tooling.api.connectivity.MultipleConnectivityTestingObjectsFoundException;
import org.mule.runtime.module.tooling.api.connectivity.NoConnectivityTestingObjectFoundException;
import org.mule.runtime.module.tooling.internal.DefaultToolingService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ToolingServiceTestCase extends AbstractMuleTestCase
{

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = none();

    @Test
    public void successfulConnectivityTesting()
    {
        ConnectionResult testConnectionResult = doFileConnectivityTesting(temporaryFolder.getRoot().getAbsolutePath());
        assertThat(testConnectionResult.getStatus(), is(SUCCESS));
    }

    @Test
    public void failedConnectivityTesting()
    {
        String baseDirPath = new File("no-existent-path").getAbsolutePath();
        ConnectionResult testConnectionResult = doFileConnectivityTesting(baseDirPath);
        assertThat(testConnectionResult.getStatus(), is(FAILURE));
        assertThat(testConnectionResult.getFailureMessage(), containsString(format("Provided baseDir '%s' does not exists", baseDirPath)));
    }

    @Test
    public void noConnectivityTestingObject()
    {
        ComponentConfiguration componentConfiguration = new ComponentConfiguration.Builder()
                .setNamespace("mule")
                .setIdentifier("configuration")
                .build();
        ConnectivityTestingService connectivityTestingService = getConnectionTestingService(componentConfiguration);

        expectedException.expect(NoConnectivityTestingObjectFoundException.class);
        connectivityTestingService.testConnection();
    }

    @Test
    public void multipleConnectivityTestingObjects() throws IOException
    {
        ConnectivityTestingService connectivityTestingService = getConnectionTestingService(
                createFileComponentConfiguration("config1", temporaryFolder.getRoot().getAbsolutePath()),
                createFileComponentConfiguration("config2", temporaryFolder.getRoot().getAbsolutePath()));

        expectedException.expect(MultipleConnectivityTestingObjectsFoundException.class);
        connectivityTestingService.testConnection();
    }

    private ConnectionResult doFileConnectivityTesting(String baseDirPath)
    {
        String configName = "fileConfig";
        ComponentConfiguration componentConfiguration = createFileComponentConfiguration(configName, baseDirPath);

        ConnectivityTestingService connectivityTestingService = getConnectionTestingService(componentConfiguration);
        return connectivityTestingService.testConnection();
    }

    private ComponentConfiguration createFileComponentConfiguration(String configName, String baseDirPath)
    {
        return new ComponentConfiguration.Builder()
                .setNamespace("file")
                .setIdentifier("config")
                .addParameter("baseDir", baseDirPath)
                .addParameter("name", configName)
                .build();
    }

    private ConnectivityTestingService getConnectionTestingService(ComponentConfiguration... componentConfigurations)
    {
        ArtifactConfiguration artifactConfiguration = new ArtifactConfiguration(asList(componentConfigurations));
        MuleArtifactResourcesRegistry muleArtifactResourcesRegistry = new MuleArtifactResourcesRegistry();
        ToolingService toolingService = new DefaultToolingService(createFakeRepositorySystem(), new TemporaryToolingArtifactBuilderFactory(muleArtifactResourcesRegistry));

        return toolingService.newConnectivityTestingServiceBuilder()
                .setArtifactConfiguration(artifactConfiguration)
                .addExtension("org.mule.extensions", "mule-module-file", "4.0-SNAPSHOT")
                .build();
    }

    private RepositoryService createFakeRepositorySystem()
    {
        return bundleDescriptor -> new File("/Users/pablolagreca/Dev/Projects2/mule/4.x/4.x-ce/extensions/file/target", "mule-module-file-4.0-SNAPSHOT.zip");
    }

}
