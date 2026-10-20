package com.example.rtsg.Structures;

import com.example.rtsg.EntityComponent;
import com.example.rtsg.Player;
import com.example.rtsg.RTSCommand;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.List;

public class StructureComponent extends EntityComponent {

    private final StructureStats stats;
    private List<RTSCommand> commandList =  new ArrayList<RTSCommand>();

    private final IntegerProperty hp;
    private final StringProperty name;
    private final IntegerProperty armor;

    public StructureComponent(StructureStats stats, Player owner) {
        super(owner);
        this.stats = stats;
        this.hp = new SimpleIntegerProperty(stats.maxHp());
        this.name = new SimpleStringProperty(stats.name());
        this.armor = new SimpleIntegerProperty(stats.armor());
    }

    //add an ability/construction to a structure
    public void addCommand(String buttonText, Runnable action){
        commandList.add(new RTSCommand(buttonText, action));
    }

    public List<RTSCommand> getCommandsList() {
        return commandList;
    }

    @Override
    public void onAdded(){
        //this component keeps its own death listener, so it must NOT call
        //super.onAdded() or the structure would be killed twice
        hpProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() <= 0) onDeath();
        });
        getOwner().setMaxSupply(getOwner().getMaxSupply() + stats.supplyProvided());
    }

    @Override
    public void onRemoved(){
        getOwner().setMaxSupply(Math.max(0, getOwner().getMaxSupply() - stats.supplyProvided()));
    }

    @Override
    public IntegerProperty hpProperty(){return this.hp;}
    @Override
    public StringProperty nameProperty(){return this.name;}
    @Override
    public IntegerProperty armorProperty() {return this.armor;}

    @Override
    public int getMaxHp() {return stats.maxHp();}

}
