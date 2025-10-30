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
        if(isInCanvas(mouseX+pixelSize, mouseY+pixelSize) && isMouseDown) {
            if (currentTool == Tool.PAINTBRUSH){
                //add color changing later
                drawnPixels.add(new Pixel((int)mouseX,(int) mouseY, white, pixelSize));
            }
        }
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

            //goes to next tool when click on button (change later)
            currentTool = nextTool(currentTool);

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
        if (isMouseDown) {
            //just temp they have same usage rn
            int brushLeft   = (int) Math.max(canvasX(), mouseX);
            int brushTop    = (int) Math.max(canvasY(), mouseY);
            int brushRight  = (int) Math.min(canvasX() + canvasWidth(),  mouseX + pixelSize);
            int brushBottom = (int) Math.min(canvasY() + canvasHeight(), mouseY + pixelSize);
            //just rough bounds around
            if (brushRight > brushLeft && brushBottom > brushTop) {
                if (currentTool == Tool.PAINTBRUSH) {

                    drawnPixels.add(new Pixel(mouseX, mouseY, white, pixelSize));

                }
                if (currentTool == Tool.PENCIL) {

                    drawnPixels.add(new Pixel(mouseX, mouseY, white, pixelSize));

                }
            }


        }

        // Draw persistent lines
        for (PersistentLines line : drawnLines) {
            //for clipping outside the bounding box
            //use sutherland-hodgman https://www.geeksforgeeks.org/dsa/polygon-clipping-sutherland-hodgman-algorithm/

            //check bounding box if entire line is outside, do not draw line
            //if part of line is inside part is outside apply the sutherland-hodgman algo
            //else draw line normally
            drawLine(context, line.x1, line.y1, line.x2, line.y2, line.width, line.color);
        }

        // Draw preview line (if any)
        if (previewLine != null) {
            drawLine(context, previewLine.x1, previewLine.y1, previewLine.x2, previewLine.y2, previewLine.width, previewLine.color);
        }
        float minX = canvasX();
        float minY = canvasY();
        float maxX = minX + canvasWidth();
        float maxY = minY + canvasHeight();
        // Draw pixels
        for (Pixel p : drawnPixels) {


            //new method
            float x1 = p.x;
            float y1 = p.y;
            float x2 = p.x + p.size;
            float y2 = p.y + p.size;

            // Skip if completely outside
            if (x2 < minX || x1 > maxX || y2 < minY || y1 > maxY)
                continue;

            // Clamp to visible area
            float drawX1 = Math.max(x1, minX);
            float drawY1 = Math.max(y1, minY);
            float drawX2 = Math.min(x2, maxX);
            float drawY2 = Math.min(y2, maxY);

            context.fill((int)drawX1, (int)drawY1, (int)drawX2, (int)drawY2, p.colour);
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


    //TODO: change clamp line drawing to be done at rendering, this will allow the user to draw as they please
    /*
    public void drawLineClamped(
            DrawContext context, float x1, float y1, float x2, float y2,
            float width, int color
            ) {
        float minX = canvasX();
        float minY = canvasY();
        float maxX = minX + canvasWidth();
        float maxY = minY + canvasHeight();
        // Cohen–Sutherland style trivial rejection
        if ((x1 < minX && x2 < minX) || (x1 > maxX && x2 > maxX) ||
                (y1 < minY && y2 < minY) || (y1 > maxY && y2 > maxY)) {
            return; // Entirely outside canvas
        }

        // Clamp endpoints inside canvas bounds
        x1 = Math.max(minX, Math.min(x1, maxX));
        y1 = Math.max(minY, Math.min(y1, maxY));
        x2 = Math.max(minX, Math.min(x2, maxX));
        y2 = Math.max(minY, Math.min(y2, maxY));

        // Then draw the clamped line
        drawLine(context, x1, y1, x2, y2, width, color);
    }*/
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


    //Sutherland-Hodgman Polygon clipping algorithm from
    //https://www.geeksforgeeks.org/dsa/polygon-clipping-sutherland-hodgman-algorithm/
    static int x_intersect(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        int num = (x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4);
        int den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        return num / den;
    }

    // Returns y-value of point of intersection of two lines
    static int y_intersect(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
        int num = (x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4);
        int den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        return num / den;
    }

    // This functions clips all the edges w.r.t one clip edge of clipping area
    static void clip(ArrayList<int[]> poly_points, int x1, int y1, int x2, int y2) {
        ArrayList<int[]> new_points = new ArrayList<>();

        for (int i = 0; i < poly_points.size(); i++) {
            // i and k form a line in polygon
            int k = (i + 1) % poly_points.size();
            int ix = poly_points.get(i)[0], iy = poly_points.get(i)[1];
            int kx = poly_points.get(k)[0], ky = poly_points.get(k)[1];

            // Calculating position of first point w.r.t. clipper line
            int i_pos = (x2 - x1) * (iy - y1) - (y2 - y1) * (ix - x1);

            // Calculating position of second point w.r.t. clipper line
            int k_pos = (x2 - x1) * (ky - y1) - (y2 - y1) * (kx - x1);

            // Case 1 : When both points are inside
            if (i_pos < 0 && k_pos < 0) {
                //Only second point is added
                new_points.add(new int[]{kx, ky});
            }

            // Case 2: When only first point is outside
            else if (i_pos >= 0 && k_pos < 0) {
                // Point of intersection with edge and the second point is added
                new_points.add(new int[]{x_intersect(x1, y1, x2, y2, ix, iy, kx, ky), y_intersect(x1, y1, x2, y2, ix, iy, kx, ky)});
                new_points.add(new int[]{kx, ky});
            }

            // Case 3: When only second point is outside
            else if (i_pos < 0 && k_pos >= 0) {
                //Only point of intersection with edge is added
                new_points.add(new int[]{x_intersect(x1, y1, x2, y2, ix, iy, kx, ky), y_intersect(x1, y1, x2, y2, ix, iy, kx, ky)});
            }

            // Case 4: When both points are outside
            else {
                //No points are added
            }
        }

        // Copying new points into original array and changing the no. of vertices
        poly_points.clear();
        poly_points.addAll(new_points);
    }

    // TODO:Implement Sutherland–Hodgman algorithm
    //BOUNDING BOX MUST BE IN CLOCKWISE ORDER (e.g
    // v1 -> v2
    // ↑     ↓
    // v0    v3
    static void suthHodgClip(ArrayList<int[]> poly_points, ArrayList<int[]> bounding_box) {
        //i and k are two consecutive indexes
        //bounding_box is the box that we are clipping the polygon(poly_points) to
        for (int i = 0; i < bounding_box.size(); i++) {
            int k = (i + 1) % bounding_box.size();

            // We pass the current array of vertices, it's size and the end points of the selected clipper line
            clip(poly_points, bounding_box.get(i)[0], bounding_box.get(i)[1], bounding_box.get(k)[0], bounding_box.get(k)[1]);
        }

        // Printing vertices of clipped polygon
        for (int[] point : poly_points)
            System.out.println("(" + point[0] + ", " + point[1] + ")");
    }


}