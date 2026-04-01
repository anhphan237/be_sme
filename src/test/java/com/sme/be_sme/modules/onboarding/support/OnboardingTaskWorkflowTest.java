package com.sme.be_sme.modules.onboarding.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingTaskWorkflowTest {

    @Test
    void normalizeStatus_mapsPendingAlias() {
        assertEquals(OnboardingTaskWorkflow.STATUS_PENDING_APPROVAL, OnboardingTaskWorkflow.normalizeStatus("pending"));
    }

    @Test
    void acknowledge_disallowsDone() {
        assertFalse(OnboardingTaskWorkflow.canAcknowledgeFrom(OnboardingTaskWorkflow.STATUS_DONE));
        assertTrue(OnboardingTaskWorkflow.canAcknowledgeFrom(OnboardingTaskWorkflow.STATUS_TODO));
    }

    @Test
    void transition_disallowsDoneToTodo() {
        assertFalse(OnboardingTaskWorkflow.canTransition(
                OnboardingTaskWorkflow.STATUS_DONE,
                OnboardingTaskWorkflow.STATUS_TODO));
    }

    @Test
    void transition_allowsWaitAckToDone() {
        assertTrue(OnboardingTaskWorkflow.canTransition(
                OnboardingTaskWorkflow.STATUS_WAIT_ACK,
                OnboardingTaskWorkflow.STATUS_DONE));
    }
}

