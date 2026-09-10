package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过 100")
    private String title;

    @Size(max = 2000, message = "正文长度不能超过 2000")
    private String content;

    private String image;

    private List<String> photos;

    @Valid
    private LocationDTO location;
}
