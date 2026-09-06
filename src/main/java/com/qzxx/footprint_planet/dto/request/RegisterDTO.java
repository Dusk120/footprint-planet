package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名长度不能超过 50")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度须在 6~50 位之间")
        String password,

        @Size(max = 50, message = "昵称长度不能超过 50")
        String nickname
) {
}
