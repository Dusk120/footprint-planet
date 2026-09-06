package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileDTO(
        @Size(max = 50, message = "昵称长度不能超过 50")
        String nickname,

        @Size(max = 200, message = "签名长度不能超过 200")
        String motto,

        String oldPassword,

        @Size(min = 6, max = 50, message = "新密码长度须在 6~50 位之间")
        String newPassword
) {
}
