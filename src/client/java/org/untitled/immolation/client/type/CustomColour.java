package org.untitled.immolation.client.type;
import java.awt.Color;

public class CustomColour {
    int r;
    int g;
    int b;
    int a;
    public CustomColour(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

    }
    public CustomColour(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = 255;
    }

    /**
     *Method takes in a colour int in the format 0x(AA)RRGGBB
     * @param num (the colour int)
     * @return CustomColour(r,g,b,a) object
     */
    public CustomColour intToRGB(int num) {
        if ((num & 0xFF000000) == 0) {
            num |= 0xFF000000; //sets the Alpha to (255)
        }
        return new CustomColour(num >> 16 & 255, num >> 8 & 255, num & 255, num >> 24 & 255);
    }

}
