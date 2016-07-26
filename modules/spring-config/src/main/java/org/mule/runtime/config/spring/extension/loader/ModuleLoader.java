/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.loader;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import org.mule.runtime.config.spring.extension.xml.ModuleXmlParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class ModuleLoader<Value, Result>
{
    protected EntityResolver entityResolver;
    private Map<String, Optional<Value>> map;

    public ModuleLoader()
    {
        this.entityResolver = new DelegatingEntityResolver(Thread.currentThread().getContextClassLoader());
        this.map = new HashedMap();
    }

    protected abstract Optional<Value> doGetValue(String publicId, String schemaLocation);
    protected abstract Optional<Result> doProcessValue(Optional<Value> value, String publicId, String schemaLocation);

    protected Optional<Result> lookupModuleResource(String publicId, String schemaLocation)
    {
        Optional<Value> value = getValue(publicId, schemaLocation);
        return doProcessValue(value, publicId, schemaLocation);
    }

    private Optional<Value> getValue(String publicId, String schemaLocation)
    {
        if (!map.containsKey(schemaLocation))
        {
            Optional<Value> valueOptional = Optional.empty();
            if (isModule(publicId, schemaLocation))
            {
                valueOptional = doGetValue(publicId, schemaLocation);
            }
            map.put(schemaLocation, valueOptional);
        }
        return map.get(schemaLocation);
    }

    private boolean isModule(String publicId, String schemaLocation){
        return ! springXsd(publicId, schemaLocation).isPresent();
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
     * TODO WIP-OPERATIONS too much work to load the resources by demand, this can easily be done at this class startup,
     * TODO         loading all the <module>s with its namespace, and even fail gracefully if 2 or more collide with its name.
     */
    protected <R> Optional<R> findInResources(Function<Resource, Optional<R>> seeker)
    {
        return Arrays.stream(getModulesResources())
                .map(resource -> seeker.apply(resource))
                .filter(resource -> resource.isPresent())
                .findFirst()
                .orElseGet(() -> Optional.empty());
    }

    public Resource[] getModulesResources(){
        Resource[] resources = new Resource[]{};
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        try
        {
            resources = pathMatchingResourcePatternResolver.getResources("classpath*:**/module-*.xml");
        }
        catch (IOException e)
        {
            //do nothing
        }
        return resources;
    }

    protected Optional<Document> parseResource(Resource resource, String schemaLocation)
    {
        Optional<Document> result = Optional.empty();
        try
        {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = db.parse(resource.getInputStream());
            Element rootElement = document.getDocumentElement();
            String namespace = rootElement.getAttribute(ModuleXmlParser.MODULE_NAMESPACE_ATTRIBUTE);
            if (rootElement.getNodeName().equals(ModuleXmlParser.MODULE_TAG)
                && isNotEmpty(namespace)
                && schemaLocation.startsWith(namespace))
            {
                result = Optional.of(document);
            }
        }
        catch (SAXException e)
        {
            //do nothing
        }
        catch (IOException e)
        {
            //do nothing
        }
        catch (ParserConfigurationException e)
        {
            //do nothing
        }
        return result;
    }
}
