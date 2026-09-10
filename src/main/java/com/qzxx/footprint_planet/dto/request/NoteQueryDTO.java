package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class NoteQueryDTO {
    private String keyword;
    private Long authorId;

    @Min(value = 1, message = "页码不能小于 1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    private Integer pageSize = 6;

    /** 排序：latest 最新发布（默认）/ hot 按点赞数 */
    @Pattern(regexp = "latest|hot", message = "排序方式仅支持 latest 或 hot")
    private String sort = "latest";
}
