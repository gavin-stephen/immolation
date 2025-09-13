package org.untitled.immolation.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL46;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import org.untitled.immolation.client.gui.PersistentLines;
public class Drawing extends Screen {
    //Could be sick to add a save/export feature for your drawings.
    //records are sick just auto make class without boilerplate
    private static record Pixel(int x, int y, int colour) {}
    //private static record PersistentLines(float x1,float y1, float x2, float y2, float width, int color) {}
    private List<Pixel> drawnPixels = new ArrayList<>();
    private List<PersistentLines> drawnLines = new ArrayList<>();
    private static PersistentLines previewLine = null;
    private boolean painting = false;
    private boolean onCanvas =  false;
    private boolean lineToggle = false;
    private boolean lerp = false;
    private int pixelSize = 1;
    private static final Identifier eraser = Identifier.of("immolation", "greyeraser32x.png");
    private static final Identifier pencil = Identifier.of("immolation", "pencil32x.png");
    private double x;
    private double y;
    private int clicked = 0;
    private boolean isMouseDown = false;
    private final int white = 0xFFFFFFFF;
    private void addPixel(Pixel pixel) {
        drawnPixels.add(pixel);
    }
    private void addLine(PersistentLines line) {
        drawnLines.add(line);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int height = this.height / 2;
        int width = this.width / 2;
        int canvasX = (this.width - width) / 2;
        int canvasY = (this.height - height) / 2;
        if (button == 0 && isHovered(canvasX, canvasY, (int) mouseX, (int) mouseY)) {
            isMouseDown = true;
            onCanvas = true;
            x = mouseX;
            y = mouseY;

            //could check for if mouse is inside box here but it would be way cleaner
            //to do just use a button or somehow get texturedbutton working ;(
            //might be worth to seperate eraser/pencil/line/shape tools into seperate classes (will get very bloated)
        } else {
            onCanvas = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        //legit have no clue im dumb need to clamp the preview line within the box

        int height = this.height / 2;
        int width = this.width / 2;
        int canvasX = (this.width - width) / 2;
        int canvasY = (this.height - height) / 2;
        // Clamp mouse position inside the box
        float clampedX = (float) Math.max(canvasX, Math.min(mouseX, canvasX + width));
        float clampedY = (float) Math.max(canvasY, Math.min(mouseY, canvasY + height));

        //felt clunky having this in an if(isHovered()) will have to think on this later
        if (lineToggle) {
            previewLine = new PersistentLines((float)x,(float) y, clampedX, clampedY, 1, white);
        }



        //previewLine = new PersistentLines((float)x,(float) y, (float)mouseX, (float)mouseY, 1, white);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {


            isMouseDown = false;
            //this is just a blanket fix maybe fix codebase later lol THIS SUCKS WHY DID I WRITE THIS
            int height = this.height / 2;
            int width = this.width / 2;
            int canvasX = (this.width - width) / 2;
            int canvasY = (this.height - height) / 2;
            //temporary clamping of X/Y so even if you try drawing line outside it will just clamp to edge
            float clampedX = (float) Math.max(canvasX, Math.min(mouseX, canvasX + width));
            float clampedY = (float) Math.max(canvasY, Math.min(mouseY, canvasY + height));

//            if (isHovered(canvasX, canvasY, (int) mouseX, (int) mouseY) && lineToggle) {
//
//                addLine(new PersistentLines((float)x, (float)y, (float)mouseX, (float)mouseY, 1, white));
//            }

            //currently just checks if line toggle and if mouseClicked was on canvas
            //mostly works and good enough for me for now
            if (lineToggle && onCanvas) {
                addLine(new PersistentLines((float)x, (float)y, clampedX, clampedY, 1, white));
            }



        return super.mouseReleased(mouseX, mouseY, button);
    }
    public Drawing(Text title) {
        super(title);
    }
    @Override
    protected void init() {
        //currently otggles whether drawing lines or pixels (probably add a seperate class to toggle between shapes
        // lines and pixels
        ButtonWidget buttonWidget = ButtonWidget.builder(Text.of("DOESNT MATTER : " + (lineToggle ? "Enabled" : "Disabled")), (button) -> {
            //THIS CODE IS RAN WHEN THE BUTTON IS PRESSED ( UPDATE THE CONFIG LATER AND OVERWRITE RENDERING OF LAVA
            lineToggle = !lineToggle;
            button.setMessage(Text.of("DOESNT MATTER : " + lineToggle));

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
        int height = this.height / 2;
        int width = this.width / 2;
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;
        int colour = 0x88000000;
        int white = 0xFFFFFFFF;

        int canvasX = (this.width - width) / 2;
        int canvasY = (this.height - height) / 2;
        int texWidth = 32;
        int texHeight = 32;

        // Button position: centered horizontally on canvas, 10 pixels  below canvas bottom
        int buttonX = canvasX + (width - texWidth) / 2;
        int buttonY = canvasY + height + 10;


        // colour is just grey here
        drawSquare(context, x, y, x+width, y+height, colour);
        //context.fill(x, y, x + width, y + height, colour);
        // NOT line toggle for now = pixel drawing
        if (isHovered(x, y, mouseX, mouseY) && isMouseDown && !lineToggle) {
            //allow drawing here

            addPixel(new Pixel(mouseX, mouseY, white));


        }


        //this is gonna get really messy really quick if i dont split it into multiple classes
        //draw all lines to screen
        for (PersistentLines line : drawnLines) {
            if (previewLine != null) {

                drawLine(context, previewLine.x1, previewLine.y1, previewLine.x2, previewLine.y2, previewLine.width, previewLine.color);
            }

            drawLine(context, line.x1, line.y1, line.x2, line.y2, line.width, line.color);
        }

        //draw all pixels to screen
        for (Pixel p : drawnPixels) {
            context.fill(p.x, p.y, p.x + pixelSize, p.y + pixelSize, white);
        }

        //draw background of pencil
        drawSquare(context, buttonX-21, buttonY-1, buttonX+11, buttonY+31, colour);
        //context.fill(buttonX-21, buttonY-1, buttonX+11, buttonY+31, colour );
        //draw backgronud of eraser
        //DRAWCONTEXT MESSES UP THE RENDERING ORDER IDK MAN I HATE THIS
        drawSquare(context, buttonX+21, buttonY-1, buttonX+53, buttonY+31, colour );
        //context.fill(buttonX+21, buttonY-1, buttonX+53, buttonY+31, colour );
        //draw both pencil and eraser
        //THIS IS SO UNBELIEVABLY UGLY BUT I REALLY CBA REFACTOR LATER
        RenderSystem.setShaderColor(1,1,1,1);
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
//    public static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, float width, int color) {
//        Tessellator tessellator = Tessellator.getInstance();
//        MatrixStack matrixStack = context.getMatrices();
//        Matrix4f transformationMatrix = context.getMatrices().peek().getPositionMatrix();
//
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        buf.vertex(transformationMatrix, x1, y1, 5).color(0xFFFFFFFF);
//        buf.vertex(transformationMatrix, x2, y2, 5).color(0xFFFFFFFF);
//
//        BufferRenderer.drawWithGlobalProgram(buf.end());
//    }
    public static BufferBuilder getBufferBuilder(MatrixStack matrixStack, VertexFormat.DrawMode drawMode) {
        matrixStack.push();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        return tessellator.begin(drawMode, VertexFormats.POSITION_COLOR);
    }
    public static void preRender() {

        RenderSystem.setShaderColor(1f,1f,1f,1f);
        RenderSystem.enableBlend();
        //RenderSystem.defaultBlendFunc();
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_DEPTH_TEST);
    }
    public static void postRender(BufferBuilder bufferBuilder, MatrixStack matrixStack) {
        matrixStack.pop();

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.disableBlend();
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
    }
    /*public static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, float width, int color) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.push();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        //prob check if in bounding box here then clamp to edge
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        if (len == 0 ) {
            return;
        }
        dx = dx / len;
        dy = dy / len;
        float px = -dy * (width / 2.0f);
        float py = dx * (width / 2.0f);

        buf.vertex(x1 + px, y1 + py, color);
        buf.vertex(x2 + px, y2 + py, color);
        buf.vertex(x2 - px, y2 - py, color);
        buf.vertex(x1 - px, y1 - py, color);
        postRender(buf, matrixStack);
        //logic behind it just modify to buf vertex
//        glBegin(GL_QUADS);
//        glVertex2f(x1 - px, y1 - py);
//        glVertex2f(x1 + px, y1 + py);
//        glVertex2f(x2 + px, y2 + py);
//        glVertex2f(x2 - px, y2 - py);
//        glEnd();
        //buf.vertex()
    }*/
    //if i decide to use matrix stack for drawing the background square (runs into other issues tho...
    public static void drawSquare(DrawContext context, float x1, float y1, float x2, float y2, int color) {
        MatrixStack matrix = context.getMatrices();
        BufferBuilder buffer = getBufferBuilder(matrix, VertexFormat.DrawMode.QUADS);
        preRender();

        Matrix4f pos = matrix.peek().getPositionMatrix();
        buffer.vertex(pos, x1, y1, 0).color(color);
        buffer.vertex(pos, x1, y2, 0).color(color);
        buffer.vertex(pos, x2, y2, 0).color(color);
        buffer.vertex(pos, x2, y1, 0).color(color);
        matrix.pop();

        BufferRenderer.drawWithGlobalProgram(buffer.end());
//        float a = ((color >> 24) & 0xFF) / 255.0f;
//        float r = ((color >> 16) & 0xFF) / 255.0f;
//        float g = ((color >> 8) & 0xFF) / 255.0f;
//        float b = (color & 0xFF) / 255.0f;
        RenderSystem.setShaderColor(1,1,1,(float)0.5);

        GL46.glDisable(GL46.GL_BLEND);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_DEPTH_TEST);

    }


    //drawline function works TY  https://github.com/SyutoBestCoder/Byte-1.21/blob/b96b4f6ab8b3199886bed53b6bda554c11bb8698/src/main/java/com/syuto/bytes/utils/impl/render/RenderUtils.java#L217
    public static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, float width, int color) {
        //will have to check if its on canvas but i really cba rn
        MatrixStack matrix = context.getMatrices();
        BufferBuilder buffer = getBufferBuilder(matrix, VertexFormat.DrawMode.QUADS);
        preRender();

        Matrix4f pos = matrix.peek().getPositionMatrix();
        float dx = x2 - x1;
        float dy = y2 - y1;

        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;

        dx /= len;
        dy /= len;

        float px = -dy * width / 2;
        float py = dx * width / 2;


        buffer.vertex(pos, x1 + px, y1 + py, 0).color(color);
        buffer.vertex(pos, x2 + px, y2 + py, 0).color(color);
        buffer.vertex(pos, x2 - px, y2 - py, 0).color(color);
        buffer.vertex(pos, x1 - px, y1 - py, 0).color(color);
        postRender(buffer, matrix);
    }
    //add a function to deal with swapping tools (pencil / eraser) + maybe add fill bucket :)
}
