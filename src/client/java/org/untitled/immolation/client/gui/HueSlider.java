package org.untitled.immolation.client.gui;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class HueSlider extends SliderWidget {

    private int value;
    //value is how far the bar is positioned inside the slider
    public HueSlider(int x, int y, int width, int height, Text text, double value) {
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
        int hue = (int)(value * 255);

        //either add a alpha value within Drawing.java (make it more cluttered)
        //or have it in ColorPicker which calls both AlphaSlider HueSlider and ColorBox
        //ColorPicker.setHue(hue);
    }
}
