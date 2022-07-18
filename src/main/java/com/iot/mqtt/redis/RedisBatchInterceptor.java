package com.iot.mqtt.redis;

import com.iot.mqtt.redis.annotation.RedisBatch;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Redis 管道切面
 *
 * @author liangjiajun
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "mqtt.cluster_enabled", havingValue = "true")
public class RedisBatchInterceptor {

    @Autowired
    private RedissonClient redissonClient;

    private static final ThreadLocal<RBatch> THREAD_LOCAL = new ThreadLocal<>();

    @Pointcut("@annotation(com.iot.mqtt.redis.annotation.RedisBatch)")
    public void excludeExecutionPointcut() {
    }

    @Around("excludeExecutionPointcut() && @annotation(redisBatch)")
    public Object execute(ProceedingJoinPoint joinPoint, RedisBatch redisBatch) throws Throwable {
        try {
            BatchOptions options = BatchOptions.defaults();
            if (redisBatch.skipResult()) {
                options.skipResult();
            }
            RBatch batch = redissonClient.createBatch(options);
            if (log.isTraceEnabled()) {
                log.trace("RedisBatch start ... Method name : {} , skipResult {}", joinPoint.getSignature().getName(), redisBatch.skipResult());
            }
            THREAD_LOCAL.set(batch);
            Object proceed = joinPoint.proceed();
            // todo: 是否要立即提交？如果方法之间的嵌套的解决思路?
            if (redisBatch.aSync()) {
                batch.executeAsync();
            } else {
                batch.execute();
            }
            if (log.isTraceEnabled()) {
                log.trace("RedisBatch execute ... Method name : {} , skipResult {}", joinPoint.getSignature().getName(), redisBatch.skipResult());
            }
            return proceed;
        } finally {
            THREAD_LOCAL.remove();
        }
    }

    public static ThreadLocal<RBatch> getThreadLocal() {
        return THREAD_LOCAL;
    }
}