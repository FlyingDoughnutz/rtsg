package com.example.rtsg;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class HealthBarComponent extends Component {

    int width = 50;
    static final int HPBAR_HEIGHT = 8;

    private final EntityComponent stats;
    private StackPane barRoot;
    private Rectangle hpBar;

    private boolean isSelected = false;
    private boolean isDamaged = false;

    public HealthBarComponent(EntityComponent stats) {
        this.stats = stats;
    }

    @Override
    public void onAdded(){
        //bounding box for hp bar
        width = (int) (entity.getWidth() - 2);
        double height = HPBAR_HEIGHT;
        //red background
        Rectangle backGround = new Rectangle(width, height, Color.DARKRED);
        backGround.setStroke(Color.BLACK);
        backGround.setStrokeWidth(1);
        //green actual hp bar
        hpBar = new Rectangle(width, height, Color.LIGHTGREEN);
        //put them in the same stackpane
        barRoot = new StackPane(backGround, hpBar);
        //align bar to left
        StackPane.setAlignment(hpBar, Pos.CENTER_LEFT);
        //slightly above unit depending on its height
        barRoot.setTranslateY(-(entity.getHeight() / 2.0) - 1.0);
        //centre it
        barRoot.setTranslateX((entity.getWidth() - width) / 2.0);
        barRoot.setVisible(false);
        //add to view of entity
        entity.getViewComponent().addChild(barRoot);
        //checks when hurt
        stats.hpProperty().addListener((obs, oldVal, newVal) -> {
            updateBarWidth();
            //updates when hp decreases
            if (newVal.intValue() < oldVal.intValue()) {
                checkVisibility();
            }
        });
        //initialize width of hp
        updateBarWidth();
    }

    @Override
    public void onUpdate (double timePerFrame) {
        if (barRoot.isVisible() && !isSelected && !isDamaged) {
            barRoot.setVisible(false);
        }
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
        checkVisibility();
    }

    private void checkVisibility() {
        if (stats.hpProperty().get() != stats.getMaxHp() ) {
            this.isDamaged = true;
        }
        barRoot.setVisible(isSelected || isDamaged);
    }

    private void updateBarWidth() {
        //calculate percenteage from 0.0 to 1.0 from hp remaining
        double percentageHurt = (double) stats.hpProperty().get() / stats.getMaxHp();
        //clamp between 0 and 1
        percentageHurt = Math.max(0, Math.min(percentageHurt, 1));
        hpBar.setWidth(width * percentageHurt);
        //change color depending on percentage of hp remaining
        if (percentageHurt < 0.33) {
            hpBar.setFill(Color.RED);
        } else if (percentageHurt < 0.66) {
            hpBar.setFill(Color.ORANGE);
        } else {
            hpBar.setFill(Color.LIGHTGREEN);
        }

    }

}
