package com.tasreeh.po.workflow_service.bpmn;

public final class BpmnElements {
    private BpmnElements() {}

    // Use names exactly as in the BPMN diagram
    public static final String GW_AMOUNT = "Amount > 1000?";
    public static final String TASK_RECEIVE = "Receive Purchase Request";
    public static final String TASK_AUTO_APPROVE = "Auto Approve";
    public static final String TASK_MANAGER_APPROVAL = "Manager Approval";
    public static final String END_PUBLISH = "Publish Status";
}

