package com.campus.express.order;

import com.campus.express.common.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {

    @Test
    void happyPathShouldReachCompleted() {
        int status = 10;
        status = OrderStateMachine.next(status, OrderEvent.PAY);
        assertEquals(20, status);
        status = OrderStateMachine.next(status, OrderEvent.ACCEPT);
        assertEquals(30, status);
        status = OrderStateMachine.next(status, OrderEvent.PICK);
        assertEquals(40, status);
        status = OrderStateMachine.next(status, OrderEvent.DELIVER);
        assertEquals(50, status);
        status = OrderStateMachine.next(status, OrderEvent.CONFIRM);
        assertEquals(60, status);
        assertTrue(OrderStateMachine.isTerminal(status));
    }

    @Test
    void illegalTransitionShouldThrow() {
        assertThrows(BizException.class, () -> OrderStateMachine.next(30, OrderEvent.CONFIRM));
        assertThrows(BizException.class, () -> OrderStateMachine.next(20, OrderEvent.PAY));
        assertThrows(BizException.class, () -> OrderStateMachine.next(60, OrderEvent.CANCEL));
        assertThrows(BizException.class, () -> OrderStateMachine.next(10, OrderEvent.DELIVER));
    }

    @Test
    void cancelFromAcceptedShouldEnterRefunding() {
        assertEquals(80, OrderStateMachine.next(30, OrderEvent.CANCEL));
        assertEquals(90, OrderStateMachine.next(80, OrderEvent.REFUND_SUCCESS));
    }

    @Test
    void afterSaleShouldBeAllowedFromDeliveredOrCompleted() {
        assertEquals(100, OrderStateMachine.next(40, OrderEvent.AFTER_SALE));
        assertEquals(100, OrderStateMachine.next(50, OrderEvent.AFTER_SALE));
        assertEquals(100, OrderStateMachine.next(60, OrderEvent.AFTER_SALE));
        assertThrows(BizException.class, () -> OrderStateMachine.next(30, OrderEvent.AFTER_SALE));
    }

    @Test
    void arbitrationShouldRestoreCompletedStatus() {
        assertEquals(60, OrderStateMachine.next(100, OrderEvent.RESOLVE));
        assertThrows(BizException.class, () -> OrderStateMachine.next(50, OrderEvent.RESOLVE));
    }

    @Test
    void terminalStatusShouldBeMarked() {
        assertTrue(OrderStateMachine.isTerminal(60));
        assertTrue(OrderStateMachine.isTerminal(70));
        assertTrue(OrderStateMachine.isTerminal(90));
        assertFalse(OrderStateMachine.isTerminal(50));
    }
}
