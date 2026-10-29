package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 某一天内的完整条目顺序：dayNo 指定天，itemIds 为该天重排后的全部条目 id（按展示顺序） */
@Data
public class DayOrderDTO {

    @NotNull(message = "天数不能为空")
    private Integer dayNo;

    @NotNull(message = "顺序不能为空")
    private List<Long> itemIds;
}
