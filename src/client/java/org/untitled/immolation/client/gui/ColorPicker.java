package org.untitled.immolation.client.gui;

import net.minecraft.util.Identifier;

public class ColorPicker {
    //IDEAS
    //1 ) use a static image button that you can click, when clicked it calls a function with mouseX and mouseY
    // and calculates the colour of that pixel
    //2) Dynamically render a Colour wheel and interpolate colour on click
    private final Identifier colorWheel;
    public ColorPicker(Identifier colorWheel) {
        this.colorWheel = colorWheel;
    }
    @Override
    public void mouseClicked(double mouseX, double mouseY) {

    }
}
