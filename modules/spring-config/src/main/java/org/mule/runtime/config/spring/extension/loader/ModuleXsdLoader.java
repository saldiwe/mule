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
import java.io.InputStream;
import java.util.Optional;

import org.apache.ws.commons.schema.XmlSchema;
import org.xml.sax.InputSource;

public class ModuleXsdLoader extends ModuleLoader<XmlSchema, InputSource>
{

    @Override
    protected Optional<XmlSchema> doGetValue(String publicId, String schemaLocation)
    {
        Optional<XmlSchema> value = Optional.empty();
        if (!springXsd(publicId, schemaLocation).isPresent())
        {
            String moduleFilename = getModuleFileName(schemaLocation);
            XmlSchema resource = createValue(moduleFilename, schemaLocation);
            value = Optional.ofNullable(resource);
        }
        return value;
    }

    @Override
    protected Optional<InputSource> getResult(Optional<XmlSchema> value, String publicId, String schemaLocation)
    {
        Optional<InputSource> result;
        if (value.isPresent()){
            try
            {
                result = Optional.of(computeValue(value.get(), publicId, schemaLocation));
            }
            catch (IOException e)
            {
                result = Optional.empty();
            }
        }else {
            result = springXsd(publicId, schemaLocation);
        }
        return result;
    }

    private XmlSchema createValue(String moduleFilename, String schemaLocation)
    {
        InputStream is = getClass().getResourceAsStream(moduleFilename);
        ModuleXml moduleXml = new ModuleXmlParser().parseDSLModuleXML(is);
        return new ModuleSchemaGenerator().getSchema(moduleXml, schemaLocation);
    }

    private InputSource computeValue(XmlSchema xmlSchema, String publicId, String schemaLocation) throws IOException
    {
        InputSource inputSource = new InputSource();
        inputSource.setPublicId(publicId);
        inputSource.setSystemId(schemaLocation);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            xmlSchema.write(out);
            inputSource.setByteStream(new ByteArrayInputStream(out.toByteArray()));
        }
        finally
        {
            out.close();
        }
        return inputSource;
    }
}
