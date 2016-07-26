/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.extension.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.utils.NamespaceMap;

public class ModuleSchemaGenerator
{
    private static final String MULE_NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/core";
    private static final String MULE_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
    private static final String MULE_NAMESPACE = "mule";

    private static final String ABSTRACT_MESSAGE_PROCESSOR_ELEMENT = "abstract-message-processor";
    private static final String ABSTRACT_MESSAGE_PROCESSOR_TYPE = "abstractMessageProcessorType";
    private static final String MULE_SUBSTITUTABLE_NAME_TYPE = "substitutableName";
    private static final String TYPE_SUFFIX = "-type";
    private static final String CONFIG_GLOBAL_ELEMENT_NAME = "config";
    public static final String CONFIG_REF_GLOBAL_ELEMENT_NAME = CONFIG_GLOBAL_ELEMENT_NAME + "-ref";
    private static final String ABSTRACT_EXTENSION_ELEMENT = "abstract-extension";
    private static final String ABSTRACT_EXTENSION_TYPE = "abstractExtensionType";
    private static final String NAME_ATTRIBUTE = "name";

    public XmlSchema getSchema(ModuleXml moduleXml, String systemId)
    {
        XmlSchema schema =new XmlSchema(XMLConstants.W3C_XML_SCHEMA_NS_URI, systemId, new XmlSchemaCollection());
        schema.setTargetNamespace(moduleXml.getNamespace());
        schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);

        //adding mule namespace and import
        NamespaceMap namespaceContext = new NamespaceMap();
        namespaceContext.add("mule", MULE_NAMESPACE_URI);
        schema.setNamespaceContext(namespaceContext);

        XmlSchemaImport muleSchemaImport = new XmlSchemaImport(schema);
        muleSchemaImport.setNamespace(MULE_NAMESPACE_URI);
        muleSchemaImport.setSchemaLocation(MULE_SCHEMA_LOCATION);

        //add config element if necessary
        if (moduleXml.hasConfig()){
            generateConfig(moduleXml, schema);
        }
        //add operations elements
        moduleXml.getOperations().stream()
                .forEach(operationXml -> generateOperation(schema, moduleXml, operationXml));

        //try { schema.write(System.out); } catch (java.io.UnsupportedEncodingException e){ e.printStackTrace(); }
        return schema;
    }

    private void generateConfig(ModuleXml moduleXml, XmlSchema schema)
    {
        final XmlSchemaElement schemaElement = new XmlSchemaElement(schema, true);
        schemaElement.setName(CONFIG_GLOBAL_ELEMENT_NAME);

        XmlSchemaComplexType configSchemaType = generateConfigSchemaType(schema, moduleXml.getProperties(), CONFIG_GLOBAL_ELEMENT_NAME);

        schemaElement.setSchemaTypeName(configSchemaType.getQName());

        QName messageProcessorQName = new QName(MULE_NAMESPACE_URI, ABSTRACT_EXTENSION_ELEMENT, MULE_NAMESPACE);
        schemaElement.setSubstitutionGroup(messageProcessorQName);
    }

    private void generateOperation(XmlSchema schema, ModuleXml moduleXml, OperationXml operationXml)
    {
        final XmlSchemaElement schemaElement = new XmlSchemaElement(schema, true);
        schemaElement.setName(operationXml.getName());

        XmlSchemaComplexType operationSchemaType = generateOperationSchemaType(schema, moduleXml, operationXml);
        schemaElement.setSchemaTypeName(operationSchemaType.getQName());

        QName messageProcessorQName = new QName(MULE_NAMESPACE_URI, ABSTRACT_MESSAGE_PROCESSOR_ELEMENT, MULE_NAMESPACE);
        schemaElement.setSubstitutionGroup(messageProcessorQName);
    }

    private XmlSchemaComplexType generateOperationSchemaType(XmlSchema schema, ModuleXml moduleXml, OperationXml operationXml)
    {
        ArrayList<XmlSchemaAttributeOrGroupRef> attributes = generateAttributes(schema, operationXml.getParameters());
        if (moduleXml.hasConfig()){
            XmlSchemaAttribute configRefAttribute = new XmlSchemaAttribute(schema, false);
            configRefAttribute.setName(CONFIG_REF_GLOBAL_ELEMENT_NAME);
            QName attributeTypeQName = new QName(MULE_NAMESPACE_URI, MULE_SUBSTITUTABLE_NAME_TYPE, MULE_NAMESPACE);
            configRefAttribute.setSchemaTypeName(attributeTypeQName);
            configRefAttribute.setUse(XmlSchemaUse.REQUIRED);
            attributes.add(configRefAttribute);
        }

        return generateSchemaType(schema, operationXml.getName(), ABSTRACT_MESSAGE_PROCESSOR_TYPE, attributes);
    }

    private XmlSchemaComplexType generateConfigSchemaType(XmlSchema schema, List<PropertyXml> parameters, String operationXmlName)
    {
        ArrayList<XmlSchemaAttributeOrGroupRef> attributes = generateAttributes(schema, parameters);

        XmlSchemaAttribute configRefAttribute = new XmlSchemaAttribute(schema, false);
        configRefAttribute.setName(NAME_ATTRIBUTE);
        QName attributeTypeQName = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string", "tns");
        configRefAttribute.setSchemaTypeName(attributeTypeQName);
        configRefAttribute.setUse(XmlSchemaUse.REQUIRED);
        attributes.add(configRefAttribute);

        return generateSchemaType(schema, operationXmlName, ABSTRACT_EXTENSION_TYPE, attributes);
    }

    private XmlSchemaComplexType generateSchemaType(XmlSchema schema, String xmlElementName, String localPart, List<XmlSchemaAttributeOrGroupRef> attributes)
    {
        XmlSchemaComplexType operationSchemaType = new XmlSchemaComplexType(schema, true);
        operationSchemaType.setName(xmlElementName.concat(TYPE_SUFFIX));

        XmlSchemaComplexContent complexContent = new XmlSchemaComplexContent();
        XmlSchemaComplexContentExtension complexContentExtension = new XmlSchemaComplexContentExtension();

        QName baseQName = new QName(MULE_NAMESPACE_URI, localPart, MULE_NAMESPACE);
        complexContentExtension.setBaseTypeName(baseQName);
        complexContentExtension.getAttributes().addAll(attributes);

        complexContent.setContent(complexContentExtension);
        operationSchemaType.setContentModel(complexContent);

        return operationSchemaType;
    }

    private ArrayList<XmlSchemaAttributeOrGroupRef> generateAttributes(XmlSchema schema, List<PropertyXml> parameters)
    {
        ArrayList<XmlSchemaAttributeOrGroupRef> attributes = new ArrayList<>();
        for (PropertyXml propertyXml : parameters)
        {
            XmlSchemaAttribute attribute = new XmlSchemaAttribute(schema, false);
            attribute.setName(propertyXml.getName());

            QName attributeTypeQName = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, propertyXml.getType(), "tns");
            attribute.setSchemaTypeName(attributeTypeQName);

            if( propertyXml.hasDefaultValue()){
                attribute.setUse(XmlSchemaUse.OPTIONAL);
                attribute.setDefaultValue(propertyXml.getDefaultValue());
            }
            else{
                attribute.setUse(XmlSchemaUse.REQUIRED);
            }
            attributes.add(attribute);
        }
        return attributes;
    }
}
