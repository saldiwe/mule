/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.param;

import org.mule.extension.db.api.param.QueryParameter;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.DynamicDbType;
import org.mule.extension.db.internal.domain.type.UnknownDbType;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves unknown and dynamic types using metadata if possible
 */
public class DefaultParamTypeResolver implements ParamTypeResolver
{

    protected final DbTypeManager dbTypeManager;
    private ParamTypeResolver metadataParamTypeResolver;

    protected DefaultParamTypeResolver(DbTypeManager dbTypeManager, ParamTypeResolver metadataParamTypeResolver)
    {
        this.dbTypeManager = dbTypeManager;
        this.metadataParamTypeResolver = metadataParamTypeResolver;
    }

    public Map<Integer, DbType> getParameterTypes(DbConnection connection, Query query) throws SQLException
    {
        Map<Integer, DbType> resolvedParamTypes = new HashMap<>();
        Map<Integer, DbType> metadataParamTypes = null;

        int index = 1;
        for (QueryParameter queryParam : query.getDefinition().getParameters())
        {
            final DbType dbType = queryParam.getType().getDbType();
            if (dbType instanceof UnknownDbType)
            {
                if (metadataParamTypes == null)
                {
                    metadataParamTypes = getParamTypesUsingMetadata(connection, query);
                }

                resolvedParamTypes.put(index, metadataParamTypes.get(index));
            }
            else if (dbType instanceof DynamicDbType)
            {
                DbType resolvedType = dbTypeManager.lookup(connection, queryParam.getType().getDbType().getName());

                resolvedParamTypes.put(index, resolvedType);
            }
            else
            {
                resolvedParamTypes.put(index, queryParam.getType().getDbType());
            }

            index++;
        }

        return resolvedParamTypes;
    }

    protected Map<Integer, DbType> getParamTypesUsingMetadata(DbConnection connection, Query query)
    {
        Map<Integer, DbType> metadataParamTypes;
        try
        {
            metadataParamTypes = metadataParamTypeResolver.getParameterTypes(connection, query);
        }
        catch (SQLException e)
        {
            metadataParamTypes = getParamTypesFromQueryTemplate(query);
        }
        return metadataParamTypes;
    }

    private Map<Integer, DbType> getParamTypesFromQueryTemplate(Query query)
    {
        Map<Integer, DbType> paramTypes = new HashMap<>();
        int index = 1;
        for (QueryParameter queryParam : query.getDefinition().getParameters())
        {
            paramTypes.put(index++, queryParam.getType().getDbType());
        }

        return paramTypes;
    }
}
