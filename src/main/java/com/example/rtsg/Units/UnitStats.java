package com.example.rtsg.Units;

public record UnitStats(
        String name,
        int maxHp,
        double moveSpeed,
        double attackRange,
        double attackDamage,
        double attackSpeed,
        int armor
) {}
