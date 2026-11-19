package com.campus.express.order.grab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 集群版抢单锁：Redis SETNX + Lua 原子脚本。
 * 锁只用于"抢"的瞬间，真正的防重仍由数据库状态 CAS 兜底。
 */
@Component
@ConditionalOnProperty(prefix = "app.grab", name = "mode", havingValue = "redis")
public class RedisGrabLock implements GrabLock {

    private static final String PREFIX = "grab:order:";
    private static final DefaultRedisScript<Long> LOCK_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('get', KEYS[1]); "
                    + "if v then "
                    + "  if v == ARGV[1] then return 3 else return 2 end "
                    + "else "
                    + "  redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2]); return 1 "
                    + "end", Long.class);

    private final StringRedisTemplate redisTemplate;

    @Value("${app.grab.ttl-seconds:300}")
    private long ttlSeconds;

    public RedisGrabLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int tryLock(String orderNo, Long courierId) {
        Long result = redisTemplate.execute(LOCK_SCRIPT,
                List.of(PREFIX + orderNo),
                String.valueOf(courierId),
                String.valueOf(ttlSeconds));
        return result == null ? HELD_BY_OTHER : result.intValue();
    }

    @Override
    public void unlock(String orderNo, Long courierId) {
        String key = PREFIX + orderNo;
        String value = redisTemplate.opsForValue().get(key);
        if (String.valueOf(courierId).equals(value)) {
            redisTemplate.delete(key);
        }
    }
}
