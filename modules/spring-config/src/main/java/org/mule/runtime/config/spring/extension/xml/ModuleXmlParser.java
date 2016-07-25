/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.xml;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class ModuleXmlParser
{

    public static final String PARAMETER_NAME = "parameterName";
    public static final String PARAMETER_DEFAULT_VALUE = "defaultValue";
    public static final String PARAMETER_TYPE = "type";


    public static final String PROPERTY_TAG = "property";
    public static final String OPERATION_TAG = "operation";
    public static final String OPERATION_NAME = "name";
    public static final String PARAMETER_TAG = "parameter";
    public static final String NAME = "name";

    public ModuleXml parseDSLModuleXML(InputStream is) {
        Document document = parseXML(is);

        Element moduleElement = document.getDocumentElement();
        ModuleXml moduleXml = new ModuleXml();
        moduleXml.setName(moduleElement.getAttribute(NAME));

        NodeList propertiesNodeList = moduleElement.getElementsByTagName(PROPERTY_TAG);
        List<PropertyXml> propertyXmls = parseProperties(propertiesNodeList);
        moduleXml.setProperties(propertyXmls);

        NodeList operationNodeList = moduleElement.getElementsByTagName(OPERATION_TAG);
        List<OperationXml> operations = parseOperations(operationNodeList);
        moduleXml.setOperations(operations);

        return moduleXml;
    }

    private Document parseXML(InputStream is) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(is);
        } catch (SAXException e) {
            throw new RuntimeException("Issue when parsing file", e);
        } catch (IOException e) {
            throw new RuntimeException("Issue when parsing file", e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Issue when parsing file", e);
        }
    }

    private List<OperationXml> parseOperations(NodeList operationNodeList) {
        List<OperationXml> operations = new ArrayList<>();
        for (int i = 0; i < operationNodeList.getLength(); i++) {
            OperationXml operation = parseOperation((Element) operationNodeList.item(i));
            operations.add(operation);
        }
        return operations;
    }

    private OperationXml parseOperation(Element operationElement) {
        OperationXml operation = new OperationXml();
        operation.setName(operationElement.getAttribute(OPERATION_NAME));
        NodeList parameterNodeList = operationElement.getElementsByTagName(PARAMETER_TAG);
        operation.setParameters(parseProperties(parameterNodeList));
        return operation;
    }

    //private MetadataType parseMetaDataType(Element operationElement, String tagElement) {
    //    Element payloadElement = (Element) operationElement.getElementsByTagName(tagElement).item(0);
    //    String format = payloadElement.getAttribute("format");
    //    MetadataFormat metadataFormat;
    //    if (MetadataFormat.JSON.getValidMimeTypes().contains(format)){
    //        metadataFormat = MetadataFormat.JSON;
    //    }else if (MetadataFormat.XML.getValidMimeTypes().contains(format)){
    //        metadataFormat = MetadataFormat.XML;
    //    }else throw new IllegalArgumentException(String.format("The supported formats are application/json and application/xml, [%s] is not supported", format));
    //
    //    BaseTypeBuilder<?> baseTypeBuilder = BaseTypeBuilder.create(metadataFormat);
    //    //BaseTypeBuilder.create(metadataFormat).arrayType().of().stringType().build()
    //    return parseMetadataType(payloadElement, baseTypeBuilder).build();
    //}

    //private BaseTypeBuilder<?> parseMetadataType(Element rootElement, BaseTypeBuilder<?> baseTypeBuilder) {
    //    Element childElement = null;
    //    for (int i = 0; i < rootElement.getChildNodes().getLength(); i++) {
    //        if (rootElement.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE){
    //            childElement = (Element)rootElement.getChildNodes().item(i);
    //            break;
    //        }
    //    }
    //    if (childElement == null){
    //        return baseTypeBuilder;
    //    }
    //
    //    switch (childElement.getNodeName()){
    //        case "null":
    //            baseTypeBuilder.nullType(); break;
    //        case "string":
    //            baseTypeBuilder.stringType(); break;
    //        case "boolean":
    //            baseTypeBuilder.booleanType(); break;
    //        case "datetime":
    //            baseTypeBuilder.dateTimeType(); break;
    //        case "date":
    //            baseTypeBuilder.dateType(); break;
    //        case "integer":
    //            baseTypeBuilder.numberType(); break;
    //        case "time":
    //            baseTypeBuilder.timeType(); break;
    //
    //        case "object":
    //            return parseMetadataObjectType(childElement, baseTypeBuilder);
    //        case "array":
    //            //TODO THIS ONE IS FAILING
    //            throw new RuntimeException("This is failing because I suck at coding.. fix later");
    //            //                return parseMetadataArrayType(childElement, baseTypeBuilder);
    //        default:
    //            throw new IllegalArgumentException("should not have reach here, supported types for <payload>/<output> are string, boolean, datetime, date, number or time");
    //    }
    //    return parseMetadataType(childElement, baseTypeBuilder);
    //}
    //
    //private BaseTypeBuilder<?> parseMetadataObjectType(Element rootElement, BaseTypeBuilder<?> baseTypeBuilder) {
    //    ObjectTypeBuilder<? extends BaseTypeBuilder<?>> baseTypeBuilderObjectTypeBuilder = baseTypeBuilder.objectType();
    //
    //    for (int i = 0; i < rootElement.getChildNodes().getLength(); i++) {
    //        if (rootElement.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE){
    //            Element childElement =  (Element)rootElement.getChildNodes().item(i);
    //            if (childElement.getNodeName().equals("field")){
    //                ObjectFieldTypeBuilder<? extends ObjectTypeBuilder<? extends BaseTypeBuilder<?>>> objectTypeBuilderObjectFieldTypeBuilder = baseTypeBuilderObjectTypeBuilder.addField();
    //                objectTypeBuilderObjectFieldTypeBuilder.key(childElement.getAttribute("name"));
    //                parseMetadataType(childElement, objectTypeBuilderObjectFieldTypeBuilder.value());
    //            }else {
    //                throw new IllegalStateException(String.format("Object types can only contain fields inside of it, not [%s]", childElement.getNodeName()));
    //            }
    //        }
    //    }
    //    return baseTypeBuilder;
    //}

    private List<PropertyXml> parseProperties(NodeList propertiesNodeList) {
        List<PropertyXml> properties = new ArrayList<>();
        for (int i = 0; i < propertiesNodeList.getLength(); i++)
        {
            PropertyXml propertyXml = parseParameter((Element) propertiesNodeList.item(i));
            properties.add(propertyXml);
        }
        return properties;
    }

    private PropertyXml parseParameter(Element parameterElement) {
        PropertyXml parameter = new PropertyXml();
        parameter.setName(parameterElement.getAttribute(PARAMETER_NAME));
        if (parameterElement.getAttributes().getNamedItem(PARAMETER_DEFAULT_VALUE) != null){
            parameter.setDefaultValue(parameterElement.getAttribute(PARAMETER_DEFAULT_VALUE));
        }

        String type = parameterElement.getAttribute(PARAMETER_TYPE);
        BaseTypeBuilder<?> baseTypeBuilder = BaseTypeBuilder.create(MetadataFormat.JSON);
        switch (type){
            case "string":
                baseTypeBuilder.stringType(); break;
            case "boolean":
                baseTypeBuilder.booleanType(); break;
            case "datetime":
                baseTypeBuilder.dateTimeType(); break;
            case "date":
                baseTypeBuilder.dateType(); break;
            case "integer":
                baseTypeBuilder.numberType(); break;
            case "time":
                baseTypeBuilder.timeType(); break;
            default:
                throw new IllegalArgumentException(String.format("should not have reach here, supported types for <parameter>(simple) are string, boolean, datetime, date, number or time FOR NOW, talk to Julian :). Type obtained [%s]", type));
        }
        //parameter.setType(baseTypeBuilder.build()); TODO TBD this relies on how this sheat are going to be treated by julian
        parameter.setType(type);
        return parameter;
    }
}
