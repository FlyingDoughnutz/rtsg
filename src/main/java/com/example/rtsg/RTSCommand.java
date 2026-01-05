package com.example.rtsg;
//represents a command assigned to a button in the control panel
public class RTSCommand {

    private String name;
    private Runnable action;

    public RTSCommand(String name, Runnable action) {
        this.name = name;
        this.action = action;
    }

    public String getName() {
        return this.name;
    }

    public void execute() {
        this.action.run();
    }

}
