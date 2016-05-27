/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.artifact;

import org.mule.runtime.module.launcher.descriptor.DeployableArtifactDescriptor;

import java.util.Optional;

/**
 * A {@code ConfigurableArtifact} is an abstract representation of a deployable artifact
 * that contains mule configuration like flows, configs, etc.
 *
 * @param <ArtifactDescriptor> type of the artifact descriptor
 *
 * @since 4.0
 */
public interface ConfigurableArtifact<ArtifactDescriptor extends DeployableArtifactDescriptor> extends DeployableArtifact<ArtifactDescriptor>
{

    /**
     * Looks up for a generic component within the artifact.
     *
     * This method must not be called if the artifact was not yet initialized.
     *
     * @param globalComponentName unique identifier for the component in the configuration.
     * @return the component within an {@link java.util.Optional} that may be empty if no such component exits.
     */
    Optional<Object> lookupComponent(String globalComponentName);

}
