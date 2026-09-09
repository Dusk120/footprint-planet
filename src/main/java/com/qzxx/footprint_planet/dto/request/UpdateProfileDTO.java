package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDTO {

    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    @Size(max = 200, message = "签名长度不能超过 200")
    private String motto;

    private String oldPassword;

    private String newPassword;
}
