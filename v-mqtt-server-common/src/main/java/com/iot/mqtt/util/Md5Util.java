package com.iot.mqtt.util;

import org.springframework.util.DigestUtils;

/**
 * @author liangjiajun
 */
public class Md5Util {

    /**
     * 实现一致性哈希算法中使用的哈希函数,使用MD5算法来保证一致性哈希的平衡性
     * @param key
     * @return
     */
    public static long hash(String key) {
        byte[] bKey = DigestUtils.md5Digest(key.getBytes());
        //具体的哈希函数实现细节--每个字节 & 0xFF 再移位
        long result = ((long) (bKey[3] & 0xFF) << 24)
                | ((long) (bKey[2] & 0xFF) << 16
                | ((long) (bKey[1] & 0xFF) << 8) | (long) (bKey[0] & 0xFF));
        return result & 0xffffffffL;
    }
}
