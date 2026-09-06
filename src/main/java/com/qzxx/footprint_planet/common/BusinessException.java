package com.qzxx.footprint_planet.common;

import lombok.Getter;

/**
 * 业务异常：Service / 拦截器抛出，code 给前端区分错误类别（默认 400）。
 * 常见约定：400 参数/业务错误，401 未认证，403 无权限，404 不存在。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
