package com.campus.express.order.grab;

public interface GrabLock {

    int GRANTED = 1;
    int HELD_BY_OTHER = 2;
    int HELD_BY_SELF = 3;

    int tryLock(String orderNo, Long courierId);

    void unlock(String orderNo, Long courierId);
}
