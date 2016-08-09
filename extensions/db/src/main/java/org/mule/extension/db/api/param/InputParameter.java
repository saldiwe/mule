/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

<<<<<<< HEAD
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.NoRef;
import org.mule.runtime.extension.api.annotation.param.Optional;

@Alias("in-param")
public class InputParameter extends QueryParameter
{
=======
import org.mule.extension.db.internal.domain.type.JdbcType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;

@Alias("in-param")
public class InputParameter
{
    /**
     * The name of the input parameter.
     */
    @Parameter
    private String name;
>>>>>>> origin/db

    /**
     * The parameter's value
     */
    @Parameter
    @Optional
<<<<<<< HEAD
    @NoRef
    private Object value;

=======
    @XmlHints(allowReferences = false)
    private Object value;

    /**
     * Parameter type name.
     */
    @Parameter
    @Optional
    private JdbcType type;

    public String getName()
    {
        return name;
    }
>>>>>>> origin/db

    public Object getValue()
    {
        return value;
    }

<<<<<<< HEAD

=======
    public JdbcType getType()
    {
        return type;
    }
>>>>>>> origin/db
}
