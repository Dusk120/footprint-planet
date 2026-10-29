package com.qzxx.footprint_planet.common;

/** 计划协作者角色。owner 由 plan.user_id 判定，editor/viewer 存 plan_collaborator 表 */
public enum PlanRole {
    OWNER, EDITOR, VIEWER
}
