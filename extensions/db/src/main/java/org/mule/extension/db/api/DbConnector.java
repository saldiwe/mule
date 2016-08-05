/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api;

import org.mule.extension.db.api.param.DynamicQueryDefinition;
import org.mule.extension.db.api.param.ParameterizedQueryDefinition;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.TemplateQueryDefinition;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.operation.DmlOperations;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.Map;

@Extension(name = "DB Connector", description = "Connector for connecting to relation Databases through the JDBC API")
@Operations({DmlOperations.class})
@SubTypeMapping(baseType = QueryDefinition.class, subTypes = {TemplateQueryDefinition.class, ParameterizedQueryDefinition.class, DynamicQueryDefinition.class})
public class DbConnector implements Initialisable
{
    /**
     * A {@link Map} which specifies non-standard custom data types, in which
     * the key is the same of the data type used by the JDBC driver and the value
     * is type identifier used by the JDBC driver.
     */
    @Parameter
    @Optional
    private Map<String, String> customDataTypes;

    private DbTypeManager dbTypeManager;

    @Override
    public void initialise() throws InitialisationException
    {
        //dbTypeManager = createTypeManager();
    }

//    private DbTypeManager createTypeManager()
//    {
//        List<DbTypeManager> typeManagers = new ArrayList<>();
//
//        typeManagers.add(new MetadataDbTypeManager());
//
//        if (customDataTypes.size() > 0)
//        {
//            typeManagers.add(new StaticDbTypeManager(customDataTypes));
//        }
//
//        List<DbType> vendorDataTypes = getVendorDataTypes();
//        if (vendorDataTypes.size() > 0)
//        {
//            typeManagers.add(new StaticDbTypeManager(vendorDataTypes));
//        }
//
//        typeManagers.add(new StaticDbTypeManager(JdbcType.getAllTypes()));
//
//        return new CompositeDbTypeManager(typeManagers);
//    }
}
