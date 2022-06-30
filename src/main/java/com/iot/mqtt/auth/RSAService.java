package com.iot.mqtt.auth;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.interfaces.RSAPrivateKey;

@Service
public class RSAService implements IAuthService{

    private RSAPrivateKey privateKey;

    @Override
    public boolean checkValid(String username, String password) {
        if (StrUtil.isBlank(username)) return false;
        if (StrUtil.isBlank(password)) return false;
        RSA rsa = new RSA(privateKey, null);
        String value = rsa.encryptBcd(username, KeyType.PrivateKey);
        return value.equals(password);
    }

    @PostConstruct
    public void init() {
        // privateKey = IoUtil.readObj(RSAService.class.getClassLoader().getResourceAsStream("keystore/auth-private.key"));
    }

}
