package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlanLegDTO {

    @NotNull(message = "起点不能为空")
    private Long fromItemId;

    @NotNull(message = "终点不能为空")
    private Long toItemId;

    /** driving / walking / riding / transit */
    @NotBlank(message = "交通方式不能为空")
    private String mode;

    /** 时长（秒） */
    private Integer duration;

    /** 距离（米） */
    private Integer distance;

    /** 方案摘要 */
    private String summary;

    /** 简化路线坐标串 */
    private String polyline;
}
