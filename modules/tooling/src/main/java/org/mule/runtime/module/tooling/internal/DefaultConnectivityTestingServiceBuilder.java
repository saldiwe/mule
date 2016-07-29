/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.createFailureResult;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.module.repository.api.BundleDescriptor;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.module.tooling.api.connectivity.NoConnectivityTestingObjectFoundException;
import org.mule.runtime.module.tooling.api.artifact.ToolingArtifact;
import org.mule.runtime.module.tooling.api.artifact.ToolingArtifactBuilderFactory;
import org.mule.runtime.module.tooling.api.artifact.ToolingArtifactBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Default implementation for {@code ConnectivityTestingServiceBuilder}.
 *
 * @since 4.0
 */
public class DefaultConnectivityTestingServiceBuilder implements ConnectivityTestingServiceBuilder
{

    private static final String EXTENSION_BUNDLE_TYPE = "zip";
    private final RepositoryService repositoryService;
    private final ToolingArtifactBuilderFactory artifactBuilderFactory;
    private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();
    private List<BundleDescriptor> bundleDescriptors = new ArrayList<>();
    private ArtifactConfiguration artifactConfiguration;
    private ToolingArtifact toolingArtifact;

    DefaultConnectivityTestingServiceBuilder(RepositoryService repositoryService, ToolingArtifactBuilderFactory artifactBuilderFactory)
    {
        this.artifactBuilderFactory = artifactBuilderFactory;
        this.repositoryService = repositoryService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectivityTestingServiceBuilder addExtension(String groupId, String artifactId, String artifactVersion)
    {
        this.bundleDescriptors.add(new BundleDescriptor.Builder()
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setType(EXTENSION_BUNDLE_TYPE)
            .setVersion(artifactVersion).build());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ConnectivityTestingServiceBuilder setArtifactConfiguration(ArtifactConfiguration artifactConfiguration)
    {
        this.artifactConfiguration = artifactConfiguration;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectivityTestingService build()
    {
        buildArtifact();
        return () -> {
            if (!toolingArtifact.isStarted())
            {
                try
                {
                    toolingArtifact.start();
                }
                catch (InitialisationException e)
                {
                    return createFailureResult(e.getMessage(), e);
                }
                catch (ConfigurationException e)
                {
                    return createFailureResult(e.getMessage(), e);
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(e);
                }
            }
            Collection<ConnectivityTestingStrategy> connectivityTestingStrategies = toolingArtifact.getMuleContext().getRegistry().lookupObjects(ConnectivityTestingStrategy.class);
            for (ConnectivityTestingStrategy connectivityTestingStrategy : connectivityTestingStrategies)
            {
                if (connectivityTestingStrategy.connectionTestingObjectIsPresent())
                {
                    try
                    {
                        return connectivityTestingStrategy.testConnectivity();
                    }
                    catch (Exception e)
                    {
                        return createFailureResult(e.getMessage(), e);
                    }
                }
            }
            throw new NoConnectivityTestingObjectFoundException(createStaticMessage("It was not possible to find an object to do connectivity testing"));
        };
    }

    private void buildArtifact()
    {
        if (toolingArtifact != null)
        {
            return;
        }

        Collection<ConnectivityTestingStrategy> connectivityTestingStrategies = serviceRegistry.lookupProviders(ConnectivityTestingStrategy.class);

        if (connectivityTestingStrategies.isEmpty())
        {
            throw new MuleRuntimeException(createStaticMessage("No %s instances where found", ConnectivityTestingService.class));
        }

        ToolingArtifactBuilder temporaryArtifactBuilder = artifactBuilderFactory.newBuilder()
                .setArtifactConfiguration(artifactConfiguration);

        connectivityTestingStrategies.stream()
                .forEach(connectivityTestingStrategy -> temporaryArtifactBuilder.addConnectivityTestingStrategyType(connectivityTestingStrategy.getClass()));

        bundleDescriptors.stream()
                .forEach(bundleDescriptor -> temporaryArtifactBuilder.addArtifactPluginFile(repositoryService.lookupBundle(bundleDescriptor)));
        toolingArtifact = temporaryArtifactBuilder.build();
    }

}
