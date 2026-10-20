package com.example.rtsg.Units;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class UnitDatabase {

    private static final Map<String, UnitStats> statsMap = new HashMap<>();

    static {
        statsMap.put("marine", new UnitStats("Marine", 70, 125, 150, 7, 0.5, 1, false, 1, 50, 25));
        statsMap.put("scv", new UnitStats("SCV", 60, 125, 15, 5, 1, 0, true, 1, 50, 12));
        //was a copy of the SCV row: name "SCV", worker flag on, ranged attack
        statsMap.put("zergling", new UnitStats("Zergling", 60, 125, 5, 5, 1, 0, false, 1, 50, 8));
    }

    public static UnitStats getStats(String unitName) {
        //an unknown/absent name has no stats; callers must handle null
        if (unitName == null) {
            return null;
        }
        //the name of the unit is converted to lowercase,
        //then the associated UnitStats is returned
        return statsMap.get(unitName.toLowerCase(Locale.ROOT));
    }

}
