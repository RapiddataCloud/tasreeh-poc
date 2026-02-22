package com.tasreeh.po.workflow_service.service;

import com.tasreeh.po.workflow_service.domain.WorkflowDecision;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class ApprovalDecisionService {

    private static final BigDecimal THRESHOLD = new BigDecimal("1000");

    public WorkflowDecision decide(BigDecimal amount) {
        if (amount == null) return WorkflowDecision.PENDING_MANAGER_APPROVAL;
        return amount.compareTo(THRESHOLD) > 0
                ? WorkflowDecision.PENDING_MANAGER_APPROVAL
                : WorkflowDecision.AUTO_APPROVE;
    }
}

