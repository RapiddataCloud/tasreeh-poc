package com.tasreeh.po.workflow_service.bpmn;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.tasreeh.po.workflow_service.bpmn.BpmnElements.*;


@Component
@RequiredArgsConstructor
public class BpmnValidator {

    private final BpmnModelLoader loader;

    @PostConstruct
    public void validate() {
        Set<String> names = loader.loadElementNames();

        require(names, TASK_RECEIVE);
        require(names, GW_AMOUNT);
        require(names, TASK_AUTO_APPROVE);
        require(names, TASK_MANAGER_APPROVAL);
        require(names, END_PUBLISH);
    }

    private void require(Set<String> names, String elementName) {
        if (!names.contains(elementName)) {
            throw new IllegalStateException("BPMN validation failed: missing element name: " + elementName);
        }
    }
}

