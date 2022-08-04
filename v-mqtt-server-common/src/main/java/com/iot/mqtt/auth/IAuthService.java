package com.iot.mqtt.auth;

public interface IAuthService {

    /**
     * 验证用户名和密码是否正确
     */
    boolean checkValid(String username, String password);

}
