package com.caiotcruz.mygamelist.dto;
public record AchievementDTO(String id, String title, String description, String icon,
        String tier, long progress, long target, boolean unlocked) {}
