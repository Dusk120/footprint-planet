package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PublishNoteDTO(
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题长度不能超过 100")
        String title,

        @Size(max = 2000, message = "正文长度不能超过 2000")
        String content,

        String image,

        List<String> photos,

        @Valid
        LocationDTO location
) {
}
