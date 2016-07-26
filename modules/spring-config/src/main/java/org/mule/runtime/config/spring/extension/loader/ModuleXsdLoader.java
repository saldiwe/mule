/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.loader;


import org.mule.runtime.config.spring.extension.xml.ModuleSchemaGenerator;
import org.mule.runtime.config.spring.extension.xml.ModuleXml;
import org.mule.runtime.config.spring.extension.xml.ModuleXmlParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import org.apache.ws.commons.schema.XmlSchema;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class ModuleXsdLoader extends ModuleLoader<XmlSchema, InputSource>
{

    public Optional<InputSource> loadModule(String publicId, String schemaLocation)
    {
        return lookupModuleResource(publicId, schemaLocation);
    }

    @Override
    protected Optional<XmlSchema> doGetValue(String publicId, String schemaLocation)
    {
        Optional<XmlSchema> value = Optional.empty();
        Function<Resource, Optional<Document>> seeker = getCriteria(schemaLocation);
        Optional<Document> moduleDocument = findInResources(seeker);
        if (moduleDocument.isPresent())
        {
            ModuleXml moduleXml = new ModuleXmlParser().parseDSLModuleXML(moduleDocument.get());
            XmlSchema schema = new ModuleSchemaGenerator().getSchema(moduleXml, schemaLocation);
            value = Optional.ofNullable(schema);
        }
        return value;
    }

    private Function<Resource, Optional<Document>> getCriteria(String schemaLocation)
    {
        return resource -> parseResource(resource, schemaLocation);
    }

    @Override
    protected Optional<InputSource> doProcessValue(Optional<XmlSchema> value, String publicId, String schemaLocation)
    {
        Optional<InputSource> result;
        if (value.isPresent())
        {
            try
            {
                InputSource inputSource = new InputSource();
                inputSource.setPublicId(publicId);
                inputSource.setSystemId(schemaLocation);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try
                {
                    value.get().write(out);
                    inputSource.setByteStream(new ByteArrayInputStream(out.toByteArray()));
                }
                finally
                {
                    out.close();
                }
                result = Optional.of(inputSource);
            }
            catch (IOException e)
            {
                result = Optional.empty();
            }
        }
        else
        {
            result = springXsd(publicId, schemaLocation);
        }
        return result;
    }
}
