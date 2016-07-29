/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tooling;

import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.api.config.ComponentConfiguration;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.ToolingServiceAware;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


public class ToolingServerClientCoreExtension implements MuleCoreExtension, ToolingServiceAware
{


    public static final String TOOLING_PORT = "tooling.port";
    private ToolingService toolingService;
    private ArtifactClassLoader containerClassLoader;

    @Override
    public void setContainerClassLoader(ArtifactClassLoader containerClassLoader)
    {
        this.containerClassLoader = containerClassLoader;
    }

    @Override
    public String getName()
    {
        return "tooling-client";
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            int port = Integer.valueOf(System.getProperty(TOOLING_PORT, "10000"));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/tooling", new ConnectivityTestingHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
        }
        catch (IOException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    public void start() throws MuleException
    {

    }

    class ConnectivityTestingHandler implements HttpHandler
    {

        @Override
        public void handle(HttpExchange t) throws IOException
        {

            try
            {
                String requestBody = IOUtils.toString(t.getRequestBody());

                JSONObject jsonRequest = new JSONObject(requestBody);
                String groupId = jsonRequest.getString("groupId");
                String artifactId = jsonRequest.getString("artifactId");
                String version = jsonRequest.getString("version");
                String namespace = jsonRequest.getString("namespace");
                String identifier = jsonRequest.getString("identifier");
                JSONArray parameters = jsonRequest.getJSONArray("parameters");

                List<ComponentConfiguration> componentConfigurationList = new ArrayList<>();
                ComponentConfiguration.Builder builder = new ComponentConfiguration.Builder()
                        .setNamespace(namespace)
                        .setIdentifier(identifier);
                componentConfigurationList.add(builder
                                                       .build());

                for (int i = 0; i < parameters.length(); i++)
                {
                    Object parameter = parameters.get(i);
                    if (parameter instanceof JSONObject)
                    {
                        JSONObject parameterObject = (JSONObject) parameter;
                        builder.addParameter(parameterObject.getString("name"), parameterObject.getString("value"));
                    }
                    else
                    {
                        throw new RuntimeException("parameters items must always be a json object");
                    }
                }

                JSONObject jsonResponse = new JSONObject();
                try
                {
                    ConnectivityTestingServiceBuilder toolingContext = toolingService.newConnectivityTestingServiceBuilder();
                    toolingContext
                            .addExtension(groupId, artifactId, version)
                            .setArtifactConfiguration(new ArtifactConfiguration(componentConfigurationList));
                    jsonResponse = new JSONObject();
                    jsonResponse.append("status", "success");
                }
                catch (Exception e)
                {
                    jsonResponse.append("status", "failure");
                    jsonResponse.append("message", e.getMessage());
                }

                String response = jsonResponse.toString(3);
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();

                JSONObject jsonResponse = new JSONObject();
                jsonResponse.append("status", "failure");
                jsonResponse.append("message", e.getMessage());
                String response = jsonResponse.toString(3);
                t.sendResponseHeaders(500, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    @Override
    public void stop() throws MuleException
    {

    }

    @Override
    public void setToolingService(ToolingService toolingService)
    {
        this.toolingService = toolingService;
    }
}
