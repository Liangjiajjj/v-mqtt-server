package com.iot.mqtt.redis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author liangjiajun
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisBatch {

    /**
     * 跳过返回结果
     */
    boolean skipResult() default true;

    /**
     * 跳过返回结果
     */
    boolean aSync() default false;

}
