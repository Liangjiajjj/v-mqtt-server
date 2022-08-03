package com.iot.mqtt.auth;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 只检验私钥一致就通过
 * @author liangjiajun
 */
@Slf4j
@Service
public class RsaAuthServiceImpl implements IAuthService {

    @Value("${rsa.public_key}")
    private String publicKey;

    @Value("${rsa.private_key}")
    private String privateKey;

    @Override
    public boolean checkValid(String username, String password) {
        if (StrUtil.isBlank(username)) return false;
        if (StrUtil.isBlank(password)) return false;
        RSA rsa = new RSA(privateKey, null);
        String value = rsa.encryptBcd(username, KeyType.PrivateKey);
        return value.equals(password) ? true : false;
    }

}
