package org.untitled.immolation.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
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
    private static record Pixel(int x, int y, int colour, int size) {}

    private final List<Pixel> drawnPixels = new ArrayList<>();
    private final List<PersistentLines> drawnLines = new ArrayList<>();
    private static PersistentLines previewLine = null;

    private boolean painting = false;
    private boolean onCanvas = false;
    //private boolean lineToggle = false;
    private boolean lerp = false;
    private int pixelSize = 1;
    private boolean isMouseDown = false;
    private double x, y;

    private final int white = 0xFFFFFFFF;

    private static final Identifier ERASER = Identifier.of("immolation", "greyeraser32x.png");
    private static final Identifier PENCIL = Identifier.of("immolation", "pencil32x.png");

    int MIN_BRUSH = 1;
    int MAX_BRUSH = 20;
    //generic Tool object to store current tool
    private Tool currentTool = Tool.PAINTBRUSH;
    private enum Tool {
        PAINTBRUSH,
        PENCIL,
        ERASER,
        CIRCLE,
        SQUARE,
        LINE
    }
    //creates a line out of canvas bounds when swapped to using button (prob just change rendering to check for it?)
    private Tool nextTool(Tool current) {
        Tool[] tools = Tool.values();
        int nextIndex = (current.ordinal() + 1) % tools.length;
        return tools[nextIndex];
    }

    public Drawing(Text title) {
        super(title);
    }

    // ===============================
    // Canvas helper methods
    // ===============================

    private int canvasWidth() {
        return width / 2;
    }
    private int canvasHeight() {
        return height / 2;
    }
    private int canvasX() {
        return (width - canvasWidth()) / 2;
    }
    private int canvasY() {
        return (height - canvasHeight()) / 2;
    }

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


    private void eraseAt(double mouseX, double mouseY) {
        int eraseRadius = pixelSize;
        drawnPixels.removeIf(p -> distance(p.x, p.y, mouseX, mouseY) < eraseRadius);
        List<PersistentLines> newLines = new ArrayList<>();

        for (PersistentLines line : drawnLines) {
            newLines.addAll(trimLineWithCircle(line, mouseX, mouseY, eraseRadius));
        }

        drawnLines.clear();
        drawnLines.addAll(newLines);
    }
    // EraseAt+trimLine method from GPT
    private List<PersistentLines> trimLineWithCircle(PersistentLines line, double cx, double cy, double r) {
        double x1 = line.x1, y1 = line.y1;
        double x2 = line.x2, y2 = line.y2;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double fx = x1 - cx;
        double fy = y1 - cy;

        double a = dx * dx + dy * dy;
        double b = 2 * (fx * dx + fy * dy);
        double c = (fx * fx + fy * fy) - r * r;

        double D = b * b - 4 * a * c;
        List<PersistentLines> result = new ArrayList<>();

        if (D < 0) {
            // No intersection, keep the line if it’s not entirely inside
            double dist1 = Math.hypot(x1 - cx, y1 - cy);
            double dist2 = Math.hypot(x2 - cx, y2 - cy);
            if (dist1 > r && dist2 > r) result.add(line);
            return result;
        }

        D = Math.sqrt(D);
        double t1 = (-b - D) / (2 * a);
        double t2 = (-b + D) / (2 * a);

        // Clamp to [0,1]
        t1 = Math.max(0, Math.min(1, t1));
        t2 = Math.max(0, Math.min(1, t2));

        if (t1 > t2) {
            double temp = t1;
            t1 = t2;
            t2 = temp;
        }

        // Compute intersection points
        float ix1 = (float)(x1 + t1 * dx);
        float iy1 = (float)(y1 + t1 * dy);
        float ix2 = (float)  (x1 + t2 * dx);
        float iy2 = ( float) (y1 + t2 * dy);

        // Add
        if (t1 > 0.01) {
            result.add(new PersistentLines((float)x1, (float)y1, ix1, iy1, line.width, line.color));
        }
        if (t2 < 0.99) {
            result.add(new PersistentLines(ix2, iy2, (float)x2, (float)y2, line.width, line.color));
        }

        return result;
    }
    private double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isInCanvas(mouseX, mouseY) && isMouseDown) {
            if (currentTool == Tool.LINE) {
                float clampedX = clampCanvasX(mouseX);
                float clampedY = clampCanvasY(mouseY);
                previewLine = new PersistentLines((float) x, (float) y, clampedX, clampedY, pixelSize, white);
            }
            if (currentTool == Tool.ERASER) {
                eraseAt(mouseX, mouseY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isMouseDown = false;
        //mostly works
        if (this.client != null && !isInCanvas(mouseX, mouseY))  {
            //instantly rerender the current frame if the mouse gets released outside of bounds
            //(to remove the inaccurate previewLine)
            this.client.setScreen(this);
        }
        if (isInCanvas(mouseX,mouseY) && currentTool == Tool.LINE && onCanvas) {
            float clampedX = clampCanvasX(mouseX);
            float clampedY = clampCanvasY(mouseY);
            drawnLines.add(new PersistentLines((float) x, (float) y, clampedX, clampedY, pixelSize, white));
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ===============================
    // GUI Setup
    // ===============================

    @Override
    protected void init() {
        previewLine = null;

        //This dynamically changes the size of each pixel,
        //TODO: ENSURE bigger brush sizes do not go out of bounds (add clipping)
        SliderWidget brushSizeSlider = new SliderWidget(0,0,200,20,
                Text.literal("Brush Size: "),
                (pixelSize - MIN_BRUSH) / (double)(MAX_BRUSH - MIN_BRUSH)
                ) {
            protected void applyValue() {
                // Map slider value (0–1) to brush size
                pixelSize = MIN_BRUSH + (int)(value * (MAX_BRUSH - MIN_BRUSH));
            }

            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Brush Size: " + pixelSize));
            }
        };
        addDrawableChild(brushSizeSlider);
        ButtonWidget toggleButton = ButtonWidget.builder(Text.of("DOESNT MATTER : " + (currentTool == Tool.LINE ? "Enabled" : "Disabled")), (button) -> {
            //just toggles between line and brush, will remove once have functioning tool buttons
            /*if (currentTool == Tool.LINE) {
                currentTool = Tool.PAINTBRUSH;

            } else {
                currentTool = Tool.LINE;
            }*/

            currentTool = nextTool(currentTool);
            //lineToggle = !lineToggle;
            button.setMessage(Text.of("Tool : " + (currentTool)));
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
        if (isInCanvas(mouseX, mouseY) && isMouseDown) {
            //just temp they have same usage rn
            if (currentTool == Tool.PAINTBRUSH) {
                drawnPixels.add(new Pixel(mouseX, mouseY, white, pixelSize));
            }
            if (currentTool == Tool.PENCIL) {
                drawnPixels.add(new Pixel(mouseX, mouseY, white, pixelSize));
            }

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
            //buggy af
            context.fill(p.x, p.y, p.x + p.size, p.y + p.size, white);
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