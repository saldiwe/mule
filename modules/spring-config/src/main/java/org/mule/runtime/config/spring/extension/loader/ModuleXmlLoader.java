/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.loader;


import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.w3c.dom.Document;

public class ModuleXmlLoader extends ModuleLoader<URL, URL>
{

    public Optional<URL> loadModule(String schemaLocation)
    {
        return lookupModuleResource(null, schemaLocation);
    }

    @Override
    protected Optional<URL> doGetValue(String publicId, String schemaLocation)
    {
        Function<Resource, Optional<URL>> seeker = getCriteria(schemaLocation);
        return findInResources(seeker);
    }

    private Function<Resource, Optional<URL>> getCriteria(String schemaLocation)
    {
        return resource -> {
            Optional<URL> result = Optional.empty();
            Optional<Document> document = parseResource(resource, schemaLocation);
            if (document.isPresent())
            {
                try
                {
                    result = Optional.of(resource.getFile().toURI().toURL());
                }
                catch (IOException e)
                {
                    //do nothing
                }
            }
            return result;
        };
    }

    @Override
    protected Optional<URL> doProcessValue(Optional<URL> value, String publicId, String schemaLocation)
    {
        return value;
    }
}
