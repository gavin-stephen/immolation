package org.untitled.immolation.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class HueSlider extends SliderWidget {

    //private int hue;
    //value is how far the bar is positioned inside the slider
    public HueSlider(int x, int y, int width, int height, Text text, double value) {
        super(x,y,width,height,text,value);
//        this.value = value;
//        this.hue = (int)(value * 255);
//        updateMessage();

    }

    @Override
    protected void updateMessage() {
        //base function from extended class (dont want to use so override to no functionality)
        setMessage(Text.literal("Hue: " + ColorPicker.hue));
    }
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        //System.out.println("renderWidgetHueSlider");
        int x = getX();
        int y = getY();
        int width = getWidth();
        //draw selected colour
        context.fill(x-20,y,x,y+height,java.awt.Color.HSBtoRGB(ColorPicker.hue, 1f,1f));
        int outlineColor = 0xFF000000;

        RenderUtils.drawHollowBox(context,x-20,y,x,y+height, outlineColor, 1);

        for (int i = 0; i < width; i++) {
            float hue = (float)i / (width - 1);
            int color = java.awt.Color.HSBtoRGB(hue, 1.0F, 1.0F);
            context.fill(x + i, y, x + i + 1, y + height, 0xFF000000 | (color & 0xFFFFFF));
        }
        // Knob position
        int knobWidth = 8;
        int knobHeight = height;
        int knobX = x + (int)(value * (width - knobWidth));
        int knobY = y;

        // Draw knob background (white)
        context.fill(knobX, knobY, knobX + knobWidth, knobY + knobHeight, 0xFFFFFFFF);

        // Draw outline (black)
        RenderUtils.drawHollowBox(context,knobX,knobY,knobX+knobWidth,knobY+knobHeight, outlineColor, 1);
        //super.renderWidget(context, mouseX, mouseY, delta);
    }
    @Override
    protected void applyValue(){
        //base function from extended class
        ColorPicker.hue = (float) this.value;
    }
    /*
    protected void updateValue(double mouseX) {
        double relative = (mouseX - getX()) / (double) getWidth();

        // clamp between 0 and 1
        if (relative < 0) relative = 0;
        if (relative > 1) relative = 1;

        this.value = relative;
        this.hue = (int)(value * 255);
        updateMessage();
        //either add a alpha value within Drawing.java (make it more cluttered)
        //or have it in ColorPicker which calls both AlphaSlider HueSlider and ColorBox
        //ColorPicker.setHue(hue);
    }*/
}
