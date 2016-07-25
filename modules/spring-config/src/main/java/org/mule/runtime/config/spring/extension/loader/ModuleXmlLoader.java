/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.loader;


import java.net.URL;
import java.util.Optional;

public class ModuleXmlLoader extends ModuleLoader<URL, URL>
{
    @Override
    protected Optional<URL> doGetValue(String publicId, String schemaLocation)
    {
        Optional<URL> value = Optional.empty();
        if (! springXsd(publicId, schemaLocation).isPresent()){
            String moduleFile = getModuleFileName(schemaLocation);
            value = Optional.ofNullable(getClass().getResource(moduleFile));
        }
        return value;
    }
    @Override
    protected Optional<URL> getResult(Optional<URL> value, String publicId, String schemaLocation)
    {
        return value;
    }

    //TODO WIP-OPERATIONS check if it's better to return the actual stream.. it seems that it is not from its consumption from the ApplicationModel.
    //@Override
    //protected Optional<InputStream> getResult(Optional<URL> value, String publicId, String schemaLocation)
    //{
    //    Optional<InputStream> result = Optional.empty();
    //
    //    if (value.isPresent()){
    //        try
    //        {
    //            result = Optional.of(value.get().openStream());
    //        }
    //        catch (IOException e)
    //        {
    //            //do nothing
    //            //throw new IllegalArgumentException(String.format("There was a failure loading the module [%s]", url.getPath()));
    //        }
    //    }
    //
    //    return result;
    //}
}
