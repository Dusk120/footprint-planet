package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorProfileVO {

    private UserVO user;

    private Integer notesCount;

    private Integer plansCount;
}
