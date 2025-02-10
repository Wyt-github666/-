package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;


    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        // 调用微信接口服务获取openid
        String openid = getOpenid(userLoginDTO.getCode());
        // 判断openid是否为空，如果为空，抛出业务异常
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        // 判断用户是否为新用户
        User user = userMapper.getByOpenid(openid);
        // 如果是新用户完成注册
        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        // 返回这个用户对象
        return user;
    }

    private String getOpenid(String code) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("grant_type", "authorization_code");
        map.put("js_code", code);
        String json = HttpClientUtil.doGet(WX_LOGIN,map);
        JSONObject jsonObject = JSON.parseObject(json);
        return jsonObject.getString("openid");
    }

}
