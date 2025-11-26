package org.untitled.immolation.client.type;

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
