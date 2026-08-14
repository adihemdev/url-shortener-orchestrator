package com.cs.urlshortenerorchestrator.engine.execution;

import lombok.Builder;
import lombok.Getter;

/**
 * ApprovalResult: Represents human approval decision.
 */
@Builder
@Getter
public class ApprovalResult {
    private boolean approved;
    private String approver;
    private String reason;
    private long approvalTimeMs;
}
