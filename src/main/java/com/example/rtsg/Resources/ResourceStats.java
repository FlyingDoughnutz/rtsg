package com.example.rtsg.Resources;

import javafx.beans.property.IntegerProperty;

public record ResourceStats(
        String name,
        int resourceAmount,
        int resourceType
) {}
