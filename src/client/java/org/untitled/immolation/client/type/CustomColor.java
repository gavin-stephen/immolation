package org.untitled.immolation.client.type;

import java.util.HashMap;
import java.util.Map;

public class CustomColor {

    int r;
    int g;
    int b;
    int a;
    public CustomColor(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

    }
    public CustomColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = 255;
    }
    //Common Colors used for easy use
    public static final Map<String, CustomColor> CommonColors = Map.ofEntries(
            Map.entry("darkred",     new CustomColor(170, 0, 0, 255)),
            Map.entry("red",         new CustomColor(255, 85, 85, 255)),
            Map.entry("gold",        new CustomColor(255, 170, 0, 255)),
            Map.entry("yellow",      new CustomColor(255, 255, 85, 255)),
            Map.entry("darkgreen",   new CustomColor(0, 170, 0, 255)),
            Map.entry("green",       new CustomColor(85, 255, 85, 255)),
            Map.entry("aqua",        new CustomColor(85, 255, 255, 255)),
            Map.entry("darkaqua",    new CustomColor(0, 170, 170, 255)),
            Map.entry("darkblue",    new CustomColor(0, 0, 170, 255)),
            Map.entry("blue",        new CustomColor(85,  85,  255, 255)),
            Map.entry("lightpurple", new CustomColor(255, 85,  255, 255)),
            Map.entry("darkpurple",  new CustomColor(170, 0,   170, 255)),
            Map.entry("white",       new CustomColor(255, 255, 255, 255)),
            Map.entry("gray",        new CustomColor(170, 170, 170, 255)),
            Map.entry("darkgray",    new CustomColor(85,  85,  85,  255)),
            Map.entry("black",       new CustomColor(0,   0,   0,   255))


    );

    /**
     *Method takes in a colour int in the format 0x(AA)RRGGBB
     * @param num (the colour int)
     * @return CustomColour(r,g,b,a) object
     */
    public CustomColor intToRGB(int num) {
        if ((num & 0xFF000000) == 0) {
            num |= 0xFF000000; //sets the Alpha to (255)
        }
        return new CustomColor(num >> 16 & 255, num >> 8 & 255, num & 255, num >> 24 & 255);
    }
    public CustomColor HSBtoRGB(float hue, float saturation, float brightness) {
        return intToRGB(java.awt.Color.HSBtoRGB(hue,saturation,brightness)); //i think im stupid why would i ever do this ????
    }
}
