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
    public static float hue;
    public static float saturation = 0f;
    public static float brightness = 1f;
    public static float alpha;

    private int lastPosX;
    private int lastPosY;
    public ColorPicker(int x, int y, int width, int height, Text text) {
        super(x,y,width,height,text);

        System.out.println("ok");
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {

        //TODO:create a box with horizontal gradient white -> color, also has vertical gradient from black bottom -> transparent white top
        //https://github.com/Wynntils/Wynntils/blob/main/common/src/main/java/com/wynntils/screens/colorpicker/widgets/SaturationBrightnessWidget.java

//        AlphaSlider alphaSlider = new AlphaSlider(400,700,200,20, Text.literal("alpha"), 0);
//        addDrawableChild(alphaSlider);
        int x = getX();
        int y = getY();

        //System.out.println("renderwidfget colorpicker");

        //makes the gradient black -> transparent vertical and white -> hue horizontal
        RenderUtils.fillSidewaysGradient(context, x, y, x+width, y+height, 0xFFFFFFFF, java.awt.Color.HSBtoRGB(hue, 1f, 1f) );
        RenderUtils.fillGradient(context, x, y, x+width, y+height, 0x00FFFFFF, 0xFF000000);


        RenderUtils.drawHollowBox(context, getX() + lastPosX - 2, getY() + lastPosY -2, getX() + lastPosX + 2, getY() + lastPosY +2, 0xFF000000, 1 );
        //very useful can copy methods like drawtexturedrect etc
        //https://github.com/Wynntils/Wynntils/blob/main/common/src/main/java/com/wynntils/utils/render/RenderUtils.java
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        System.out.println("narration");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        updateValues(mouseX,mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void updateValues(double x, double y) {
        //update values works but might be worth looking into making the bottom of it easier to click ?
        saturation = (float) Math.clamp((x-getX()) / getWidth() , 0.0f, 1.0f);
        brightness = (float) (1.0f - Math.clamp((y-getY()) / getHeight()  , 0.0f, 1.0f));
        System.out.println("updateValues to : Saturation " + saturation + " Brightness " + brightness);
        System.out.println("x/width : " +  x/getWidth());
        System.out.println("y/width : " +  y/getHeight());
        lastPosX = Math.clamp((int) (x - getX()), 0, getWidth());
        lastPosY = Math.clamp((int) (y - getY()), 0, getHeight());
        System.out.println("lastPosX : " + lastPosX);
        System.out.println("lastPosY : " + lastPosY);

        return;
    }

}
