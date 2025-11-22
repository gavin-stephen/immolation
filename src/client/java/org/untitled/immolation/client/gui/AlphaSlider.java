package org.untitled.immolation.client.gui;

import org.untitled.immolation.client.gui.ColorPicker;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class AlphaSlider extends SliderWidget {

    //value is how far the bar is positioned inside the slider
    public AlphaSlider(int x, int y, int width, int height, Text text, double value) {
        super(x,y,width,height,text,value);

    }

    @Override
    protected void updateMessage() {
        //base function from extended class (dont want to use so override to no functionality)
        this.setMessage(Text.literal("Alpha: " + (int)(ColorPicker.alpha * 255)));
    }

    @Override
    protected void applyValue(){
        //base function from extended class
        ColorPicker.alpha = (float) this.value;

        System.out.println(alpha);
    }

}
