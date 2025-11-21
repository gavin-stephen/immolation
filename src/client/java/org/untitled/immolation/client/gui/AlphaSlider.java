package org.untitled.immolation.client.gui;

import org.untitled.immolation.client.gui.ColorPicker;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class AlphaSlider extends SliderWidget {

    //value is how far the bar is positioned inside the slider
    public AlphaSlider(int x, int y, int width, int height, Text text, double value) {
        super(x,y,width,height,text,value);

    }
    public void onClick(double mouseX, double mouseY) {
        updateValue(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        updateValue(mouseX);
    }
    @Override
    protected void updateMessage() {
        //base function from extended class (dont want to use so override to no functionality)
    }

    @Override
    protected void applyValue(){
        //base function from extended class
    }
    protected void updateValue(double mouseX) {
        int alpha = (int)((mouseX - getX()) / getWidth() * 255); //wynntils does it this way

        //either add a alpha value within Drawing.java (make it more cluttered)
        //or have it in ColorPicker which calls both AlphaSlider HueSlider and ColorBox

    }
}
