/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal;

import static java.util.Arrays.asList;
import org.mule.extension.db.api.param.CustomDataType;
import org.mule.extension.db.api.param.DynamicQueryDefinition;
import org.mule.extension.db.api.param.ParameterizedQueryDefinition;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.api.param.TemplateQueryDefinition;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.connection.DefaultDbConnectionProvider;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.ArrayResolvedDbType;
import org.mule.extension.db.internal.domain.type.CompositeDbTypeManager;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.JdbcType;
import org.mule.extension.db.internal.domain.type.MappedStructResolvedDbType;
import org.mule.extension.db.internal.domain.type.MetadataDbTypeManager;
import org.mule.extension.db.internal.domain.type.ResolvedDbType;
import org.mule.extension.db.internal.domain.type.StaticDbTypeManager;
import org.mule.extension.db.internal.operation.DmlOperations;
import org.mule.extension.db.internal.resolver.param.GenericParamTypeResolverFactory;
import org.mule.extension.db.internal.resolver.param.ParamTypeResolver;
import org.mule.extension.db.internal.resolver.param.ParamTypeResolverFactory;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

@Extension(name = "DB Connector", description = "Connector for connecting to relation Databases through the JDBC API")
@Operations({DmlOperations.class})
@Providers({DefaultDbConnectionProvider.class})
@SubTypeMapping(baseType = QueryDefinition.class, subTypes = {TemplateQueryDefinition.class, ParameterizedQueryDefinition.class, DynamicQueryDefinition.class})
public class DbConnector implements Initialisable
{

    /**
     * A {@link List} which specifies non-standard custom data types
     */
    @Parameter
    @Optional
    private List<CustomDataType> customDataTypes = new LinkedList<>();

    private DbTypeManager baseTypeManager;

    /**
     * Determines actual parameter types for the parameters defined in a
     * query template.
     *
     * @param queryTemplate query template that needing parameter resolution
     * @return a not null map containing the parameter type for each parameter index
     * @throws SQLException when there are error processing the query
     */
    public Map<Integer, DbType> getParamTypes(DbConnection connection, QueryTemplate queryTemplate) throws SQLException
    {
        ParamTypeResolverFactory paramTypeResolverFactory = new GenericParamTypeResolverFactory(createTypeManager(connection));
        ParamTypeResolver paramTypeResolver = paramTypeResolverFactory.create(queryTemplate);
        return paramTypeResolver.getParameterTypes(connection, queryTemplate);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        baseTypeManager = createBaseTypeManager();
    }

    private DbTypeManager createTypeManager(DbConnection connection)
    {
        List<DbType> vendorDataTypes = connection.getVendorDataTypes();
        if (vendorDataTypes.size() > 0)
        {
            return new CompositeDbTypeManager(asList(baseTypeManager, new StaticDbTypeManager(connection.getVendorDataTypes())));
        }

        return baseTypeManager;
    }

    private DbTypeManager createBaseTypeManager()
    {
        List<DbTypeManager> typeManagers = new ArrayList<>();

        typeManagers.add(new MetadataDbTypeManager());

        if (customDataTypes.size() > 0)
        {
            typeManagers.add(new StaticDbTypeManager(getCustomTypes()));
        }

        typeManagers.add(new StaticDbTypeManager(JdbcType.getAllTypes()));

        return new CompositeDbTypeManager(typeManagers);
    }

    private List<DbType> getCustomTypes()
    {
        return customDataTypes.stream()
                .map(type ->
                     {
                         final String name = type.getName();
                         final int id = type.getId();
                         if (id == Types.ARRAY)
                         {
                             return new ArrayResolvedDbType(id, name);
                         }
                         else if (id == Types.STRUCT)
                         {
                             final String className = type.getClassName();
                             if (!StringUtils.isEmpty(className))
                             {
                                 Class<?> mappedClass;
                                 try
                                 {
                                     mappedClass = Class.forName(className);
                                 }
                                 catch (ClassNotFoundException e)
                                 {
                                     throw new IllegalArgumentException("Cannot find mapped class: " + className);
                                 }
                                 return new MappedStructResolvedDbType<>(id, name, mappedClass);
                             }
                             else
                             {
                                 return new ResolvedDbType(id, name);
                             }
                         }
                         else
                         {
                             return new ResolvedDbType(id, name);
                         }
                     })
                .collect(new ImmutableListCollector<>());
    }
}
