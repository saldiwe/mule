/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.query;

import static java.util.Arrays.asList;
import org.mule.extension.db.api.param.InputParameter;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.type.CompositeDbTypeManager;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.StaticDbTypeManager;
import org.mule.extension.db.internal.resolver.param.GenericParamTypeResolverFactory;
import org.mule.extension.db.internal.resolver.param.ParamTypeResolverFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Represents an instantiation of a {@link QueryTemplate} with parameter values
 */
public class Query {

  private final QueryDefinition queryDefinition;
  private final StatementType statementType;
  private final DbConnection connection;
  private final ParamTypeResolverFactory paramTypeResolverFactory;


  /**
   * Creates a query from a template and a set of parameter values
   *
   * @param queryTemplate template describing the query
   * @param paramValues   parameter values for the query
   */
  public Query(QueryDefinition queryDefinition, StatementType statementType, DbConnector connector, DbConnection connection) {
    this.queryDefinition = queryDefinition;
    this.statementType = statementType;
    this.connection = connection;
    paramTypeResolverFactory = new GenericParamTypeResolverFactory(createTypeManager(connection, connector));
  }

  public QueryDefinition getDefinition() {
    return queryDefinition;
  }

  public StatementType getStatementType() {
    return statementType;
  }

  public boolean hasInputParameters() {
    return queryDefinition.getParameters().stream().anyMatch(p -> p instanceof InputParameter);
  }

  /**
   * Determines actual parameter types for the parameters defined in {@code this}
   * query
   *
   * @return a not null map containing the parameter type for each parameter index
   * @throws SQLException when there are error processing the query
   */
  public Map<Integer, DbType> getParamTypes() throws SQLException {
    return paramTypeResolverFactory.create(this).getParameterTypes(connection, this);
  }

  private DbTypeManager createTypeManager(DbConnection connection, DbConnector connector) {
    final DbTypeManager baseTypeManager = connector.getTypeManager();
    List<DbType> vendorDataTypes = connection.getVendorDataTypes();
    if (vendorDataTypes.size() > 0) {
      return new CompositeDbTypeManager(asList(baseTypeManager, new StaticDbTypeManager(connection.getVendorDataTypes())));
    }

    return baseTypeManager;
  }
}
