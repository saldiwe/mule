/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.config.spring.extension.loader.ModuleXsdLoader;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ModuleDelegatingEntityResolver implements EntityResolver
{
    private final ModuleXsdLoader moduleXsdLoader;

    public ModuleDelegatingEntityResolver()
    {
        moduleXsdLoader = new ModuleXsdLoader();
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
    {
        InputSource inputSource = moduleXsdLoader.loadModule(publicId, systemId)
                .orElse(null);
        return inputSource;
    }
}
