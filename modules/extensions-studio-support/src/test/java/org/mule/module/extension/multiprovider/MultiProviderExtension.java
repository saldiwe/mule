/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.multiprovider;

import org.mule.extension.annotation.api.Configurations;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.capability.Studio;
import org.mule.extension.annotation.api.capability.Xml;
import org.mule.extension.annotation.api.connector.Providers;
import org.mule.module.extension.multiconfig.BaseConfig1;
import org.mule.module.extension.multiconfig.BaseConfig2;

/**
 * Created by pablocabrera on 11/26/15.
 */
@Extension(name = "multi-provider")
@Xml(schemaLocation = "http://www.mulesoft.org/schema/mule/multi-provider", namespace = "multi-provider", schemaVersion = "3.7")
@Operations(MultiProviderOperations.class)
@Configurations({BaseConfig1.class, BaseConfig2.class})
@Providers({MultiProviderConnectionProvider.class, AnotherMultiProviderConnectionProvider.class})
@Studio
public class MultiProviderExtension
{

}