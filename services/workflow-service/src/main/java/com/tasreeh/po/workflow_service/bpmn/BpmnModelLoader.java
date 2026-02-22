package com.tasreeh.po.workflow_service.bpmn;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashSet;
import java.util.Set;

@Component
public class BpmnModelLoader {

    public Set<String> loadElementNames() {
        try {
            var resource = new ClassPathResource("bpmn/po-approval.bpmn");
            var file = resource.getInputStream();

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(file);

            // In BPMN XML, element names are stored in attribute: name="..."
            NodeList all = doc.getElementsByTagName("*");

            Set<String> names = new HashSet<>();
            for (int i = 0; i < all.getLength(); i++) {
                var node = all.item(i);
                if (node.getAttributes() != null && node.getAttributes().getNamedItem("name") != null) {
                    names.add(node.getAttributes().getNamedItem("name").getNodeValue());
                }
            }
            return names;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load BPMN file from resources/bpmn/po-approval.bpmn", e);
        }
    }
}

