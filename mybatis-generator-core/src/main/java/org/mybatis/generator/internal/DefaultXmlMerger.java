/*
 *    Copyright 2006-2021 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.generator.internal;

import org.joox.Match;
import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.exception.ShellException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.joox.JOOX.$;
import static org.mybatis.generator.internal.util.messages.Messages.getString;

/**
 * Xml merger
 * @author wnow20
 */
public class DefaultXmlMerger implements XmlMerger {

    private static final String ID = "id";

    @Override
    public String merge(GeneratedXmlFile generatedXmlFile,
                        File existingFile) throws ShellException {
        try {
            FileInputStream old = new FileInputStream(existingFile);
            InputStream next = getInputSource(generatedXmlFile);
            return mergeInternal(old, next);
        } catch (SAXException | IOException e) {
            throw new ShellException(getString("Warning.13", //$NON-NLS-1$
                    existingFile.getName()), e);
        }
    }

    protected String mergeInternal(InputStream old, InputStream next) throws IOException, SAXException {
        Document document = $(old).document();
        Document nextDocument = $(next).document();

        List<String> generatedIds = new ArrayList<>();
        $(nextDocument).forEach(element -> {
            String elementId = $(element).attr(ID);
            if (elementId != null && !elementId.trim().isEmpty()) {
                generatedIds.add(elementId);
            }
        });
        List<Element> manualElements = new ArrayList<>();
        $(document).forEach(element -> {
            String elementId = $(element).attr(ID);
            if (elementId != null && !elementId.trim().isEmpty()) {
                if (generatedIds.contains(elementId)) {
                    generatedIds.remove(elementId);
                } else {
                    manualElements.add(element);
                }
            }
        });

        manualElements.forEach(element -> {
            String previousSiblingId = $(element.getPreviousSibling()).attr(ID);
            if (previousSiblingId != null) {
                Match foundPreviousSibling = $(nextDocument).find("#" + previousSiblingId);
                if (foundPreviousSibling.isNotEmpty()) {
                    foundPreviousSibling.after(element);
                } else {
                    String nextSiblingId = $(element.getNextSibling()).attr(ID);
                    Match foundNextSibling = $(nextDocument).find("#" + nextSiblingId);
                    if (foundNextSibling.isNotEmpty()) {
                        foundNextSibling.before(element);
                    } else {
                        $(nextDocument.appendChild(element));
                    }
                }
            }
        });

        return $(nextDocument).content();
    }

    private static InputStream getInputSource(GeneratedXmlFile generatedXmlFile) {
        String formattedContent = generatedXmlFile.getFormattedContent();
        return new ByteArrayInputStream(formattedContent.getBytes());
    }
}
