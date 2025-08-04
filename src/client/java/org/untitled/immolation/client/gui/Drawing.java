package org.untitled.immolation.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class Drawing extends Screen {
    //Could be sick to add a save/export feature for your drawings.
    //records are sick just auto make class without boilerplate
    private static record Pixel(int x, int y, int colour) {}
    private List<Pixel> drawnPixels = new ArrayList<>();
    private boolean painting = false;
    private boolean onCanvas =  false;
    private boolean lerp = false;
    private int pixelSize = 1;
    private static final Identifier eraser = Identifier.of("immolation", "greyeraser32x.png");
    private static final Identifier pencil = Identifier.of("immolation", "pencil32x.png");
    private double x;
    private double y;
    private boolean isMouseDown = false;
    private void addPixel(Pixel pixel) {
        drawnPixels.add(pixel);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isMouseDown = true;
            x = mouseX;
            y = mouseY;
            //could check for if mouse is inside box here but it would be way cleaner
            //to do just use a button or somehow get texturedbutton working ;(
            //might be worth to seperate eraser/pencil/line/shape tools into seperate classes (will get very bloated)
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isMouseDown = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    public Drawing(Text title) {
        super(title);
    }
    @Override
    protected void init() {
        ButtonWidget buttonWidget = ButtonWidget.builder(Text.of("LERP : " + (lerp ? "Enabled" : "Disabled")), (button) -> {
            //THIS CODE IS RAN WHEN THE BUTTON IS PRESSED ( UPDATE THE CONFIG LATER AND OVERWRITE RENDERING OF LAVA
            lerp = !lerp;
            button.setMessage(Text.of("LERP : " + lerp));

        }).dimensions(40,40,120,20).build();
        this.addDrawableChild(buttonWidget);
        int canvasWidth = this.width / 2;
        int canvasHeight = this.height / 2;
        int canvasX = (this.width - canvasWidth) / 2;
        int canvasY = (this.height - canvasHeight) / 2;
        int texWidth = 32;
        int texHeight = 32;

        // Button position: centered horizontally on canvas, 10 pixels  below canvas bottom
        int buttonX = canvasX + (canvasWidth - texWidth) / 2;
        int buttonY = canvasY + canvasHeight + 10;


        //give up on buttonTextures and instead manually check mouseX and mouseY within the pencil/eraser box
        /*ButtonTextures textures = new ButtonTextures(pencil, pencil, pencil, pencil);
        TexturedButtonWidget pencilWidget = new TexturedButtonWidget(
                buttonX,
                buttonY,
                texWidth,
                texHeight,
                textures,
                b -> {
                    painting = true;
                    System.out.println("pencil clicked");
                },
                Text.empty()

        );
        addDrawableChild(pencilWidget);*/

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
//        context.drawTexture(RenderLayer::getGuiTextured, pencil,1,2,0,0,32,32,32,32);
        super.render(context, mouseX, mouseY, delta);
        int canvasWidth = this.width / 2;
        int canvasHeight = this.height / 2;
        int canvasX = (this.width - canvasWidth) / 2;
        int canvasY = (this.height - canvasHeight) / 2;
        int texWidth = 32;
        int texHeight = 32;

        // Button position: centered horizontally on canvas, 10 pixels  below canvas bottom
        int buttonX = canvasX + (canvasWidth - texWidth) / 2;
        int buttonY = canvasY + canvasHeight + 10;

        //the texture literally works itsjust buttontextures that doesnt ...
        int height = this.height / 2;
        int width = this.width / 2;
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;
        int colour = 0x88000000;
        int white = 0xFFFFFFFF;
        context.fill(x, y, x + width, y + height, colour);

        if (isHovered(x, y, mouseX, mouseY) && isMouseDown) {
            //allow drawing here

            addPixel(new Pixel(mouseX, mouseY, white));


        }
        if (lerp) {
            drawLine(context, 0, 0, 55, 55, 2, white);
            drawLine(context, (float)x, (float)y, (float)mouseX, (float)mouseY, 1, white);
//            for (Pixel p : drawnPixels) {
//                //yea i have no clue how im gonna do this prob have to directly work with
//                //matrices and use a custom drawLine function .. .. . . . .
//                context.fill(p.x, p.y, p.x + pixelSize, p.y + pixelSize, white);
//            }
        } else {
            for (Pixel p : drawnPixels) {
                context.fill(p.x, p.y, p.x + pixelSize, p.y + pixelSize, white);
            }
        }

        //draw background of pencil
        context.fill(buttonX-21, buttonY-1, buttonX+11, buttonY+31, colour );
        //draw backgronud of eraser
        context.fill(buttonX+21, buttonY-1, buttonX+53, buttonY+31, colour );
        //draw both pencil and eraser
        context.drawTexture(RenderLayer::getGuiTextured, pencil,buttonX-20, buttonY, 0,0, texWidth, texHeight,texWidth, texHeight);
        context.drawTexture(RenderLayer::getGuiTextured, eraser,buttonX+20, buttonY, 0,0, texWidth, texHeight,texWidth, texHeight);
    }
    boolean isHovered(int x, int y, int mouseX, int mouseY) {
        if (mouseX > x && mouseX < x + this.width/2 && mouseY > y && mouseY < y + this.height/2) {
            return true;
        } else {
            return false;
        }
    }
    public static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, float width, int color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        Matrix4f transformationMatrix = context.getMatrices().peek().getPositionMatrix();
        buf.vertex(transformationMatrix, x1, y1, 0).color(0xFFFFFFFF);
        buf.vertex(transformationMatrix, x2, y2, 0).color(0xFFFFFFFF);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
    //add a function to deal with swapping tools (pencil / eraser) + maybe add fill bucket :)
}
