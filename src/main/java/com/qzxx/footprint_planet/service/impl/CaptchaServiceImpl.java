package com.qzxx.footprint_planet.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.codec.Base64;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.service.CaptchaService;
import com.qzxx.footprint_planet.vo.CaptchaVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaServiceImpl implements CaptchaService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public CaptchaVO create() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 44, 4, 25);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set("captcha:" + uuid, captcha.getCode(), 2, TimeUnit.MINUTES);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        captcha.write(out);
        String img = "data:image/png;base64," + Base64.encode(out.toByteArray());
        return new CaptchaVO(uuid, img);
    }

    @Override
    public void verify(String uuid, String code) {
            String key = "captcha:" + uuid;
            String right = stringRedisTemplate.opsForValue().get(key);
            if (right == null) throw new BusinessException(400, "验证码已过期，请刷新重试");
            if (!right.equalsIgnoreCase(code)) throw new BusinessException(400, "验证码错误");
            stringRedisTemplate.delete(key);
    }
}
