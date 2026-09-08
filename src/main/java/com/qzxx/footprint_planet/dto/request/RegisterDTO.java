package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度须在 6~50 位之间")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @NotBlank(message = "验证码编号不能为空")
    private String uuid;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
