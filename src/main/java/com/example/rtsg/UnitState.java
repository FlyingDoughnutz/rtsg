package com.example.rtsg;

public enum UnitState {
    IDLE,
    MOVING,
    ATTACK,
    ATTACK_MOVE,
    HOLD,
    PATROL,
    GATHER,
    RETURN_CARGO,
    //worker walking to a construction site
    MOVE_TO_BUILD,
    //worker standing at the site and adding build progress
    BUILDING
}
