package com.example.rtsg.Units;

import java.util.HashMap;
import java.util.Map;

public class UnitDatabase {

    private static final Map<String, UnitStats> statsMap = new HashMap<>();

    static {
        statsMap.put("marine", new UnitStats("Marine", 70, 125, 150, 7, 0.5, 1, false, 1, 50, 25));
        statsMap.put("scv", new UnitStats("SCV", 60, 125, 15, 5, 1, 0, true, 1, 50, 12));
        statsMap.put("zergling", new UnitStats("SCV", 60, 125, 15, 5, 1, 0, true, 1, 50, 8));
    }

    public static UnitStats getStats(String unitName) {
        //converts name of unit is converted to lowercase,
        //then returns the associated UnitStats
        return statsMap.get(unitName.toLowerCase());
    }

}
