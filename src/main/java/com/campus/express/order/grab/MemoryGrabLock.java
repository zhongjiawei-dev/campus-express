package com.campus.express.order.grab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单机版抢单锁：ConcurrentHashMap + 惰性过期，适合本地演示与单实例部署。
 * 集群部署请切换 app.grab.mode=redis。
 */
@Component
@ConditionalOnProperty(prefix = "app.grab", name = "mode", havingValue = "memory", matchIfMissing = true)
public class MemoryGrabLock implements GrabLock {

    private record Holder(Long courierId, long expireAt) {
    }

    private final ConcurrentHashMap<String, Holder> locks = new ConcurrentHashMap<>();

    @Value("${app.grab.ttl-seconds:300}")
    private long ttlSeconds;

    @Override
    public int tryLock(String orderNo, Long courierId) {
        AtomicInteger result = new AtomicInteger(HELD_BY_OTHER);
        long expireAt = System.currentTimeMillis() + ttlSeconds * 1000;
        locks.compute(orderNo, (key, holder) -> {
            if (holder == null || holder.expireAt() < System.currentTimeMillis()) {
                result.set(GRANTED);
                return new Holder(courierId, expireAt);
            }
            if (holder.courierId().equals(courierId)) {
                result.set(HELD_BY_SELF);
            } else {
                result.set(HELD_BY_OTHER);
            }
            return holder;
        });
        return result.get();
    }

    @Override
    public void unlock(String orderNo, Long courierId) {
        locks.computeIfPresent(orderNo, (key, holder) ->
                holder.courierId().equals(courierId) ? null : holder);
    }
}
