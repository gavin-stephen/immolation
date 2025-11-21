package org.untitled.immolation.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;
public class ColorPicker extends ClickableWidget {

    //yea dont use color wheel 10x harder for no reason
    //if im going to make it more clean transfer the renderColorPicker here later

    //Store HSB (HSV) values
    private float hue;
    private float saturation;
    private float brightness;


    public ColorPicker(int x, int y, int width, int height, Text text) {
        super(x,y,width,height,text);

        System.out.println("ok");
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {

        //TODO:create a box with horizontal gradient white -> color, also has vertical gradient from black bottom -> transparent white top
        //https://github.com/Wynntils/Wynntils/blob/main/common/src/main/java/com/wynntils/screens/colorpicker/widgets/SaturationBrightnessWidget.java






        //very useful can copy methods like drawtexturedrect etc
        //https://github.com/Wynntils/Wynntils/blob/main/common/src/main/java/com/wynntils/utils/render/RenderUtils.java
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        //shouldnt need i dont care about accessibility
    }
    /*@Override
    public void mouseClicked(double mouseX, double mouseY) {

    }*/

}
