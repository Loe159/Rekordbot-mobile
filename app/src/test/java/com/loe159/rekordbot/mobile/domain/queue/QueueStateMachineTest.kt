package com.loe159.rekordbot.mobile.domain.queue

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueStateMachineTest {
    @Test
    fun `only pending and failed operations can be claimed`() {
        assertTrue(QueueStateMachine.canClaim(QueueStatus.PENDING))
        assertTrue(QueueStateMachine.canClaim(QueueStatus.FAILED))
        assertFalse(QueueStateMachine.canClaim(QueueStatus.DRAFT))
        assertFalse(QueueStateMachine.canClaim(QueueStatus.SENDING))
        assertFalse(QueueStateMachine.canClaim(QueueStatus.SENT))
    }

    @Test
    fun `sending operation cannot be deleted or retried concurrently`() {
        assertFalse(QueueStateMachine.canDelete(QueueStatus.SENDING))
        assertFalse(QueueStateMachine.canRetry(QueueStatus.SENDING))
        assertTrue(QueueStateMachine.canRetry(QueueStatus.FAILED))
    }
}
