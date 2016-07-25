/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.loader;

import org.mule.runtime.core.util.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class ModuleLoader<Value, Result>
{
    public static final String MULESOFT_PREFIX_MODULE = "http://www.mulesoft.org/schema/mule/modules";
    public static final String XSD_SUFFIX = ".xsd";
    public static final String XML_SUFFIX = ".xml";

    protected EntityResolver entityResolver;
    private Map<String, Optional<Value>> map;

    public ModuleLoader()
    {
        this.entityResolver = new DelegatingEntityResolver(Thread.currentThread().getContextClassLoader());
        this.map = new HashedMap();
    }

    protected abstract Optional<Value> doGetValue(String publicId, String schemaLocation);
    protected abstract Optional<Result> getResult(Optional<Value> value, String publicId, String schemaLocation);

    public Optional<Result> lookupModuleResource(String publicId, String schemaLocation)
    {
        Optional<Value> value = lookupAndCacheModule(publicId, schemaLocation);
        return getResult(value, publicId, schemaLocation);
    }


    private Optional<Value> lookupAndCacheModule(String publicId, String schemaLocation)
    {
        if (! map.containsKey(schemaLocation))
        {
            Optional<Value> valueOptional = doGetValue(publicId, schemaLocation);
            map.put(schemaLocation, valueOptional);
        }
        return map.get(schemaLocation);
    }


    protected Optional<InputSource> springXsd(String publicId, String schemaLocation)
    {
        Optional<InputSource> result = Optional.empty();
        try
        {
            result = Optional.ofNullable(entityResolver.resolveEntity(publicId, schemaLocation));
        }
        catch (SAXException e)
        {
            //do nothing
        }
        catch (IOException e)
        {
            //do nothing
        }
        return result;
    }

    /**
     * Strips the {@link #MULESOFT_PREFIX_MODULE} from the beginning, removes the {@link #XSD_SUFFIX} and appends it
     * {@link #XML_SUFFIX} to it. So, for "http://www.mulesoft.org/schema/mule/modules/module/module-simple/module-simple.xsd"
     * it will return "/module/module-simple/module-simple.xml"
     *
     * @param schemaLocation
     * @return
     */
    protected String getModuleFileName(String schemaLocation)
    {
        return StringUtils.removeEnd(
                StringUtils.removeStart(schemaLocation, MULESOFT_PREFIX_MODULE), XSD_SUFFIX)
                .concat(XML_SUFFIX);
    }

}
