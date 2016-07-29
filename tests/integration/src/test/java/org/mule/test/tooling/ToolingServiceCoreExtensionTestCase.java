/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tooling;

import static com.mashape.unirest.http.Unirest.post;
import static java.lang.String.format;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.MuleException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.core.Is;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

public class ToolingServiceCoreExtensionTestCase  extends AbstractFakeMuleServerTestCase
{

    @Rule
    public DynamicPort toolingServerPort = new DynamicPort(ToolingServerClientCoreExtension.TOOLING_PORT);

    public ToolingServiceCoreExtensionTestCase()
    {
        JSONObject request = new JSONObject();
        JSONArray parameters = new JSONArray();
        JSONObject parameter = new JSONObject();
        parameters.put(parameter);
        parameter.put("name", "fileConfig").put("value", "myConfig");
        parameter = new JSONObject();
        parameters.put(parameter);
        parameter.put("name", "baseDir").put("value", "/");
        request.put("groupId", "org.mule.extensions")
                .put("artifactId", "mule-module-file")
                .put("version", "4.0-SNAPSHOT")
                .put("namespace", "file")
                .put("identifier", "config")
                .put("parameters", parameters);
        System.out.println(request.toString(3));
    }

    @Test
    public void test() throws UnirestException, IOException, MuleException
    {
        muleServer.start();

        //ComponentConfiguration componentConfiguration = new ComponentConfiguration.Builder()
        //        .setNamespace("file")
        //        .setIdentifier("config")
        //        .addParameter("baseDir", "/Users/lalala/pop")
        //        .addParameter("name", "fileConfig")
        //        .build();
        //
        ////ComponentConfiguration componentConfiguration = new ComponentConfiguration.Builder()
        ////        .setNamespace("mule")
        ////        .setIdentifier("catch-exception-strategy")
        ////        //.addParameter("baseDir", "/")
        ////        .addParameter("name", "fileConfig")
        ////        .build();
        //
        //
        //ArtifactConfiguration artifactConfiguration = new ArtifactConfiguration(asList(componentConfiguration));
        //ToolingService toolingService =  new DefaultToolingService(createFakeRepositorySystem(), MuleContainer.createArtifactBuilderFactory(containerClassLoader, domainManager));
        //ToolingContext toolingContext = toolingService.newToolingContextBuilder()
        //        .setArtifactConfiguration(artifactConfiguration)
        //        .addExtension("org.mule.extensions", "mule-module-file", "4.0-SNAPSHOT")

        JSONObject request = new JSONObject();
        JSONArray parameters = new JSONArray();
        JSONObject parameter = new JSONObject();
        parameters.put(parameter);
        parameter.put("name", "fileConfig").put("value", "myConfig");
        parameter = new JSONObject();
        parameters.put(parameter);
        parameter.put("name", "baseDir").put("value", "/");
        request.put("groupId", "org.mule.extensions")
                .put("artifactId", "mule-module-file")
                .put("version", "4.0-SNAPSHOT")
                .put("namespace", "file")
                .put("identifier", "config")
                .put("parameters", parameters);
        HttpResponse<JsonNode> httpResponse = post(format("http://localhost:%s/tooling", toolingServerPort.getNumber()))
                .header("accept", "application/json")
                .body(request)
                .asJson();

        org.junit.Assert.assertThat(httpResponse.getBody().getObject().getString("status"), Is.is("successful"));
    }

    @Override
    protected List<MuleCoreExtension> getCoreExtensions()
    {
        ToolingServerClientCoreExtension toolingServerClientCoreExtension = new ToolingServerClientCoreExtension();
        return Arrays.asList(toolingServerClientCoreExtension);
    }
}

