package com.cs.urlshortenerorchestrator.engine;

import com.cs.urlshortenerorchestrator.engine.domain.*;
import com.cs.urlshortenerorchestrator.engine.execution.DecisionRecorder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Decision Lineage - Recording and Audit Trail")
class DecisionRecorderTests {

    /**
     * TEST 1: Record approval decisions with context
     */
    @Test
    @DisplayName("records approval decision with reasoning")
    void testRecordApprovalDecision() {
        DecisionRecorder recorder = new DecisionRecorder("wf-001");

        recorder.recordApprovalDecision("impl", "exec-1", true, "TECH_LEAD", "Code review passed");

        assertThat(recorder.getDecisions()).hasSize(1);
        Decision decision = recorder.getDecisions().get(0);
        assertThat(decision.type()).isEqualTo(DecisionType.APPROVAL_DECISION);
        assertThat(decision.reasoning()).contains("impl");
        assertThat(decision.outcome()).isEqualTo("APPROVED - Code review passed");
        assertThat(decision.reversible()).isTrue();
    }

    /**
     * TEST 2: Record retry decisions with attempt context
     */
    @Test
    @DisplayName("records retry decision with backoff delay")
    void testRecordRetryDecision() {
        DecisionRecorder recorder = new DecisionRecorder("wf-001");

        recorder.recordRetryDecision("impl", "exec-1", 1, "Network timeout", 2);

        assertThat(recorder.getDecisions()).hasSize(1);
        Decision decision = recorder.getDecisions().get(0);
        assertThat(decision.type()).isEqualTo(DecisionType.RETRY_ON_FAILURE);
        assertThat(decision.reasoning()).contains("Network timeout");
        assertThat(decision.outcome()).contains("2 second backoff");
        assertThat(decision.reversible()).isTrue();
    }

    /**
     * TEST 3: Record rollback decisions
     */
    @Test
    @DisplayName("records rollback decision with operations")
    void testRecordRollbackDecision() {
        DecisionRecorder recorder = new DecisionRecorder("wf-001");
        List<String> ops = Arrays.asList("git reset --soft", "schema rollback");

        recorder.recordRollbackDecision("impl", "exec-1", "Approval rejected", ops);

        assertThat(recorder.getDecisions()).hasSize(1);
        Decision decision = recorder.getDecisions().get(0);
        assertThat(decision.type()).isEqualTo(DecisionType.ROLLBACK_DECISION);
        assertThat(decision.reasoning()).contains("Approval rejected");
        assertThat(decision.outcome()).contains("git reset --soft");
        assertThat(decision.reversible()).isFalse();
    }

    /**
     * TEST 4: Record safe-stop decisions
     */
    @Test
    @DisplayName("records safe-stop decision when rollback not possible")
    void testRecordSafeStopDecision() {
        DecisionRecorder recorder = new DecisionRecorder("wf-001");

        recorder.recordSafeStopDecision("release", "exec-1", "Database changes are irreversible");

        assertThat(recorder.getDecisions()).hasSize(1);
        Decision decision = recorder.getDecisions().get(0);
        assertThat(decision.type()).isEqualTo(DecisionType.SAFE_STOP_DECISION);
        assertThat(decision.reasoning()).contains("Database changes");
        assertThat(decision.outcome()).contains("SAFE_STOPPED");
        assertThat(decision.reversible()).isFalse();
    }

    /**
     * TEST 5: Record replan decisions
     */
    @Test
    @DisplayName("records replan decision with broken assumptions")
    void testRecordReplanDecision() {
        DecisionRecorder recorder = new DecisionRecorder("wf-001");
        List<String> assumptions = Arrays.asList("Schema compatibility", "API stability");

        recorder.recordReplanDecision("impl", "Exit gate validation failure",
            assumptions, 1);

        assertThat(recorder.getDecisions()).hasSize(1);
        Decision decision = recorder.getDecisions().get(0);
        assertThat(decision.type()).isEqualTo(DecisionType.REPLAN);
        assertThat(decision.reasoning()).contains("Replan #1");
        assertThat(decision.reasoning()).contains("Schema compatibility");
        assertThat(decision.reversible()).isTrue();
    }

    /**
     * TEST 6: Decision causality chain
     */
    @Test
    @DisplayName("chains decisions showing causality")
    void testDecisionCausalityChain() {
        DecisionRecorder recorder = new DecisionRecorder("wf-001");

        // Sequence: retry → retry → success
        recorder.recordRetryDecision("impl", "exec-1", 1, "Timeout", 1);
        recorder.recordRetryDecision("impl", "exec-2", 2, "Connection error", 2);

        List<Decision> decisions = recorder.getDecisions();
        assertThat(decisions).hasSize(2);

        // Second decision should reference first
        Decision second = decisions.get(1);
        assertThat(second.relatedDecisionIds()).contains(decisions.get(0).id());

        // Verify chain
        List<Decision> chain = recorder.getDecisionChain(decisions.get(1).id());
        assertThat(chain).hasSize(2);
        assertThat(chain.get(0).reasoning()).contains("Timeout");
        assertThat(chain.get(1).reasoning()).contains("Connection error");
    }

    /**
     * TEST 7: Get decisions by node
     */
    @Test
    @DisplayName("retrieves decisions filtered by node")
    void testGetDecisionsByNode() {
        DecisionRecorder recorder = new DecisionRecorder("wf-001");

        recorder.recordRetryDecision("impl", "exec-1", 1, "Error", 1);
        recorder.recordApprovalDecision("validation", "exec-2", true, "USER", "OK");
        recorder.recordRollbackDecision("impl", "exec-3", "Failed", Arrays.asList());

        List<Decision> implDecisions = recorder.getDecisionsForNode("impl");
        assertThat(implDecisions).hasSize(2);
        assertThat(implDecisions).extracting(d -> d.type())
            .containsExactlyInAnyOrder(DecisionType.RETRY_ON_FAILURE, DecisionType.ROLLBACK_DECISION);

        List<Decision> validationDecisions = recorder.getDecisionsForNode("validation");
        assertThat(validationDecisions).hasSize(1);
        assertThat(validationDecisions.get(0).type()).isEqualTo(DecisionType.APPROVAL_DECISION);
    }

    /**
     * TEST 8: Complete execution scenario with multiple decisions
     */
    @Test
    @DisplayName("records complex scenario with approval, retry, and rollback")
    void testComplexDecisionScenario() {
        DecisionRecorder recorder = new DecisionRecorder("wf-release-001");

        // Implementation retry
        recorder.recordRetryDecision("impl", "exec-impl-1", 1, "Network timeout", 5);
        recorder.recordRetryDecision("impl", "exec-impl-2", 2, "Transient connection error", 10);

        // Validation approval
        recorder.recordApprovalDecision("validation", "exec-val-1", true,
            "QA_LEAD", "All tests passed");

        // Release approval rejected
        recorder.recordApprovalDecision("release", "exec-rel-1", false,
            "RELEASE_MGR", "Requires hotfix for critical bug");

        // Rollback after rejection
        recorder.recordRollbackDecision("release", "exec-rel-1",
            "Approval rejected",
            Arrays.asList("git revert", "deployment rollback"));

        // Verify full chain
        List<Decision> decisions = recorder.getDecisions();
        assertThat(decisions).hasSize(5);

        // Verify types
        assertThat(decisions.stream().map(Decision::type).toList())
            .isEqualTo(Arrays.asList(
                DecisionType.RETRY_ON_FAILURE,
                DecisionType.RETRY_ON_FAILURE,
                DecisionType.APPROVAL_DECISION,
                DecisionType.APPROVAL_DECISION,
                DecisionType.ROLLBACK_DECISION
            ));

        // Verify causality
        for (int i = 1; i < decisions.size(); i++) {
            assertThat(decisions.get(i).relatedDecisionIds())
                .contains(decisions.get(i - 1).id());
        }
    }
}
