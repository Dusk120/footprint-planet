package com.qzxx.footprint_planet.vo;

public record AuthorProfileVO(
        UserVO user,
        Integer notesCount,
        Integer plansCount
) {
}
