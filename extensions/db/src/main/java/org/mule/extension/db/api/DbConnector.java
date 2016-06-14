/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api;

import org.mule.extension.db.api.config.GenericDbConfig;
import org.mule.extension.db.api.param.DynamicQueryDefinition;
import org.mule.extension.db.api.param.ParameterizedQueryDefinition;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.TemplateQueryDefinition;
import org.mule.extension.db.internal.operation.DmlOperations;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;

@Extension(name = "DB Connector", description = "Connector for connecting to relation Databases through the JDBC API")
@Configurations({GenericDbConfig.class})
@Operations({DmlOperations.class})
@SubTypeMapping(baseType = QueryDefinition.class, subTypes = {TemplateQueryDefinition.class, ParameterizedQueryDefinition.class, DynamicQueryDefinition.class})
public class DbConnector
{

}
