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
    private static record Pixel(int x, int y, int colour) {}

    private final List<Pixel> drawnPixels = new ArrayList<>();
    private final List<PersistentLines> drawnLines = new ArrayList<>();
    private static PersistentLines previewLine = null;

    private boolean painting = false;
    private boolean onCanvas = false;
    private boolean lineToggle = false;
    private boolean lerp = false;
    private int pixelSize = 1;
    private boolean isMouseDown = false;
    private double x, y;

    private final int white = 0xFFFFFFFF;

    private static final Identifier ERASER = Identifier.of("immolation", "greyeraser32x.png");
    private static final Identifier PENCIL = Identifier.of("immolation", "pencil32x.png");

    public Drawing(Text title) {
        super(title);
    }

    // ===============================
    // Canvas helper methods
    // ===============================

    private int canvasWidth() { return width / 2; }
    private int canvasHeight() { return height / 2; }
    private int canvasX() { return (width - canvasWidth()) / 2; }
    private int canvasY() { return (height - canvasHeight()) / 2; }

    private boolean isInCanvas(double mx, double my) {
        return mx >= canvasX() && mx <= canvasX() + canvasWidth() &&
                my >= canvasY() && my <= canvasY() + canvasHeight();
    }

    private float clampCanvasX(double mx) {
        return (float) Math.max(canvasX(), Math.min(mx, canvasX() + canvasWidth()));
    }

    private float clampCanvasY(double my) {
        return (float) Math.max(canvasY(), Math.min(my, canvasY() + canvasHeight()));
    }

    // ===============================
    // Event handling
    // ===============================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInCanvas(mouseX, mouseY)) {
            isMouseDown = true;
            onCanvas = true;
            x = mouseX;
            y = mouseY;
        } else {
            onCanvas = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (lineToggle) {
            float clampedX = clampCanvasX(mouseX);
            float clampedY = clampCanvasY(mouseY);
            previewLine = new PersistentLines((float) x, (float) y, clampedX, clampedY, 1, white);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isMouseDown = false;

        if (lineToggle && onCanvas) {
            float clampedX = clampCanvasX(mouseX);
            float clampedY = clampCanvasY(mouseY);
            drawnLines.add(new PersistentLines((float) x, (float) y, clampedX, clampedY, 1, white));
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ===============================
    // GUI Setup
    // ===============================

    @Override
    protected void init() {
        ButtonWidget toggleButton = ButtonWidget.builder(Text.of("DOESNT MATTER : " + (lineToggle ? "Enabled" : "Disabled")), (button) -> {
            lineToggle = !lineToggle;
            button.setMessage(Text.of("DOESNT MATTER : " + lineToggle));
        }).dimensions(40, 40, 120, 20).build();
        addDrawableChild(toggleButton);
    }

    // ===============================
    // Rendering
    // ===============================

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int x = canvasX();
        int y = canvasY();
        int w = canvasWidth();
        int h = canvasHeight();
        int color = 0x88000000;

        // Canvas background
        drawSquare(context, x, y, x + w, y + h, color);

        // Draw pixels if painting
        if (isInCanvas(mouseX, mouseY) && isMouseDown && !lineToggle) {
            drawnPixels.add(new Pixel(mouseX, mouseY, white));
        }

        // Draw persistent lines
        for (PersistentLines line : drawnLines) {
            drawLine(context, line.x1, line.y1, line.x2, line.y2, line.width, line.color);
        }

        // Draw preview line (if any)
        if (previewLine != null) {
            drawLine(context, previewLine.x1, previewLine.y1, previewLine.x2, previewLine.y2, previewLine.width, previewLine.color);
        }

        // Draw pixels
        for (Pixel p : drawnPixels) {
            context.fill(p.x, p.y, p.x + pixelSize, p.y + pixelSize, white);
        }

        renderToolIcons(context);
    }

    private void renderToolIcons(DrawContext context) {
        int texWidth = 32;
        int texHeight = 32;
        int x = canvasX();
        int y = canvasY();
        int w = canvasWidth();
        int h = canvasHeight();
        int bgColor = 0x88000000;

        int buttonX = x + (w - texWidth) / 2;
        int buttonY = y + h + 10;

        // Pencil background
        drawSquare(context, buttonX - 21, buttonY - 1, buttonX + 11, buttonY + 31, bgColor);
        // Eraser background
        drawSquare(context, buttonX + 21, buttonY - 1, buttonX + 53, buttonY + 31, bgColor);

        // Icons
        RenderSystem.setShaderColor(1, 1, 1, 1);
        context.drawTexture(RenderLayer::getGuiTextured, PENCIL, buttonX - 20, buttonY, 0, 0, texWidth, texHeight, texWidth, texHeight);
        context.drawTexture(RenderLayer::getGuiTextured, ERASER, buttonX + 20, buttonY, 0, 0, texWidth, texHeight, texWidth, texHeight);
    }

    // ===============================
    // Rendering Helpers (unchanged)
    // ===============================

    public static BufferBuilder getBufferBuilder(MatrixStack matrixStack, VertexFormat.DrawMode drawMode) {
        matrixStack.push();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        return tessellator.begin(drawMode, VertexFormats.POSITION_COLOR);
    }

    public static void preRender() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_DEPTH_TEST);
    }

    public static void postRender(BufferBuilder bufferBuilder, MatrixStack matrixStack) {
        matrixStack.pop();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
    }

    public static void drawSquare(DrawContext context, float x1, float y1, float x2, float y2, int color) {
        MatrixStack matrix = context.getMatrices();
        BufferBuilder buffer = getBufferBuilder(matrix, VertexFormat.DrawMode.QUADS);
        preRender();

        Matrix4f pos = matrix.peek().getPositionMatrix();
        buffer.vertex(pos, x1, y1, 0).color(color);
        buffer.vertex(pos, x1, y2, 0).color(color);
        buffer.vertex(pos, x2, y2, 0).color(color);
        buffer.vertex(pos, x2, y1, 0).color(color);

        postRender(buffer, matrix);
    }

    public static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, float width, int color) {
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
}