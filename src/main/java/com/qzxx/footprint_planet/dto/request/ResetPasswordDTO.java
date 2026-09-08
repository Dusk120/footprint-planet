package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordDTO {

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度须在 6~50 位之间")
    private String password;
}
