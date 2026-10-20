package com.example.rtsg;

public enum EntityType
{
    UNIT,
    STRUCTURE,
    RESOURCE,
    //a tile-aligned building under construction; becomes a STRUCTURE when finished
    CONSTRUCTION
}
