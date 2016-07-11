/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.type;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.sql.Types;
import java.util.List;

/**
 * Defines {@link DbType} for JDBC types defined in {@link Types}
 */
public enum JdbcType
{

    BIT_DB_TYPE(new ResolvedDbType(Types.BIT, "BIT")),
    TINYINT_DB_TYPE(new ResolvedDbType(Types.TINYINT, "TINYINT")),
    SMALLINT_DB_TYPE(new ResolvedDbType(Types.SMALLINT, "SMALLINT")),
    INTEGER_DB_TYPE(new ResolvedDbType(Types.INTEGER, "INTEGER")),
    BIGINT_DB_TYPE(new ResolvedDbType(Types.BIGINT, "BIGINT")),
    FLOAT_DB_TYPE(new ResolvedDbType(Types.FLOAT, "FLOAT")),
    REAL_DB_TYPE(new ResolvedDbType(Types.REAL, "REAL")),
    DOUBLE_DB_TYPE(new ResolvedDbType(Types.DOUBLE, "DOUBLE")),
    NUMERIC_DB_TYPE(new ResolvedDbType(Types.NUMERIC, "NUMERIC")),
    DECIMAL_DB_TYPE(new ResolvedDbType(Types.DECIMAL, "DECIMAL")),
    CHAR_DB_TYPE(new ResolvedDbType(Types.CHAR, "CHAR")),
    VARCHAR_DB_TYPE(new ResolvedDbType(Types.VARCHAR, "VARCHAR")),
    LONGVARCHAR_DB_TYPE(new ResolvedDbType(Types.LONGVARCHAR, "LONGVARCHAR")),
    DATE_DB_TYPE(new ResolvedDbType(Types.DATE, "DATE")),
    TIME_DB_TYPE(new ResolvedDbType(Types.TIME, "TIME")),
    TIMESTAMP_DB_TYPE(new ResolvedDbType(Types.TIMESTAMP, "TIMESTAMP")),
    BINARY_DB_TYPE(new ResolvedDbType(Types.BINARY, "BINARY")),
    VARBINARY_DB_TYPE(new ResolvedDbType(Types.VARBINARY, "VARBINARY")),
    LONGVARBINARY_DB_TYPE(new ResolvedDbType(Types.LONGVARBINARY, "LONGVARBINARY")),
    NULL_DB_TYPE(new ResolvedDbType(Types.NULL, "NULL")),
    OTHER_DB_TYPE(new ResolvedDbType(Types.OTHER, "OTHER")),
    JAVA_OBJECT_DB_TYPE(new ResolvedDbType(Types.JAVA_OBJECT, "JAVA_OBJECT")),
    DISTINCT_DB_TYPE(new ResolvedDbType(Types.DISTINCT, "DISTINCT")),
    STRUCT_DB_TYPE(new ResolvedDbType(Types.STRUCT, "STRUCT")),
    ARRAY_DB_TYPE(new ResolvedDbType(Types.ARRAY, "ARRAY")),
    BLOB_DB_TYPE(new ResolvedDbType(Types.BLOB, "BLOB")),
    CLOB_DB_TYPE(new ResolvedDbType(Types.CLOB, "CLOB")),
    REF_DB_TYPE(new ResolvedDbType(Types.REF, "REF")),
    DATALINK_DB_TYPE(new ResolvedDbType(Types.DATALINK, "DATALINK")),
    BOOLEAN_DB_TYPE(new ResolvedDbType(Types.BOOLEAN, "BOOLEAN")),
    ROWID_DB_TYPE(new ResolvedDbType(Types.ROWID, "ROWID")),
    NCHAR_DB_TYPE(new ResolvedDbType(Types.NCHAR, "NCHAR")),
    NVARCHAR_DB_TYPE(new ResolvedDbType(Types.NVARCHAR, "NVARCHAR")),
    LONGNVARCHAR_DB_TYPE(new ResolvedDbType(Types.LONGNVARCHAR, "LONGNVARCHAR")),
    NCLOB_DB_TYPE(new ResolvedDbType(Types.NCLOB, "NCLOB")),
    SQLXML_DB_TYPE(new ResolvedDbType(Types.SQLXML, "SQLXML"));

    private final ResolvedDbType dbType;

    public static List<DbType> getAllTypes()
    {
        return stream(JdbcType.values()).map(JdbcType::getDbType).collect(toList());
    }

    JdbcType(ResolvedDbType dbType)
    {
        this.dbType = dbType;
    }

    public ResolvedDbType getDbType()
    {
        return dbType;
    }
}
