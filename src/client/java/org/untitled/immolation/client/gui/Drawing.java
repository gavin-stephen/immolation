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

//render methods like drawSquare / drawLine / etc
import static org.untitled.immolation.client.gui.RenderUtils.*;

public class Drawing extends Screen {
    private static record Pixel(int x, int y, int colour, int size) {}

    private final List<Pixel> drawnPixels = new ArrayList<>();
    private final List<PersistentLines> drawnLines = new ArrayList<>();
    private final List<PersistentLines> drawnBoxes = new ArrayList<>(); // this is getting really messy
    private static PersistentLines previewLine = null;
    private static PersistentLines previewBox = null; // to be used in Square tool
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

    //code is so unbelievably disgusting
    public static int alpha = 0;




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
//        for (PersistentLines box : drawnBoxes) {
//              maybe use this probably just have all in drawnlines
//        }
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
                int rgb = java.awt.Color.HSBtoRGB(ColorPicker.hue,1f,1f);
                int argb = ((int)(ColorPicker.alpha*255) << 24) | (rgb & 0x00FFFFFF);

                drawnPixels.add(new Pixel((int)mouseX, (int)mouseY, argb, pixelSize));

                //drawnPixels.add(new Pixel((int)mouseX,(int) mouseY, white, pixelSize));
            }
        }
        if (isInCanvas(mouseX, mouseY) && isMouseDown) {
            if (currentTool == Tool.LINE) {
                float clampedX = clampCanvasX(mouseX);
                float clampedY = clampCanvasY(mouseY);
                previewLine = new PersistentLines((float) x, (float) y, clampedX, clampedY, pixelSize, ColorPicker.getIntColor());
            }
            if (currentTool == Tool.ERASER) {
                //TODO: ERASING WITH SMALLER SIZE THAN DRAWN PIXELS IS INSANELY SCUFFED (JUST WORKS ON TOP LEFT CORNER)
                eraseAt(mouseX, mouseY);
            }
            if (currentTool == Tool.SQUARE ) {
                float clampedX = clampCanvasX(mouseX);
                float clampedY = clampCanvasY(mouseY);
                previewBox = new PersistentLines((float) x, (float) y, clampedX, clampedY, pixelSize, ColorPicker.getIntColor());
            }
            if (currentTool == Tool.PENCIL) {
                //PENCIL ALWAYS SIZE = 1
                //and smoother drawing than paintbrush (splotchy)
                drawInterpolated(x,y,mouseX,mouseY, ColorPicker.getIntColor(), 1);
                x = mouseX;
                y = mouseY;
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
            //TODO: commented this on 11/26/25, should add back if unable to fix (doesnt look like it does anything anyway)
            //this.client.setScreen(this);


            previewLine = null; //remove previewline it shouldnt matter tho bc it has explicit checking in draw
            previewBox = null;
        }
        if (isInCanvas(mouseX,mouseY) && currentTool == Tool.LINE && onCanvas) {
            float clampedX = clampCanvasX(mouseX);
            float clampedY = clampCanvasY(mouseY);
            drawnLines.add(new PersistentLines((float) x, (float) y, clampedX, clampedY, pixelSize, ColorPicker.getIntColor()));
        }
        if (isInCanvas(mouseX,mouseY) && currentTool == Tool.SQUARE && onCanvas) {
//            float clampedX = clampCanvasX(mouseX);
//            float clampedY = clampCanvasY(mouseY);
//            drawnBoxes.add(new PersistentLines((float) x, (float) y, clampedX, clampedY,  pixelSize, ColorPicker.getIntColor()));

            float left = Math.min(previewBox.x1, previewBox.x2);
            float top = Math.min(previewBox.y1, previewBox.y2);
            float right = Math.max(previewBox.x1, previewBox.x2);
            float bottom = Math.max(previewBox.y1, previewBox.y2);
            //TODO: FIX lines being aligned when drawn
            drawnLines.add(new PersistentLines(left, top, right, top, pixelSize, ColorPicker.getIntColor()));
            drawnLines.add(new PersistentLines(left, bottom, right, bottom, pixelSize, ColorPicker.getIntColor()));
            drawnLines.add(new PersistentLines(left, top, left, bottom, pixelSize, ColorPicker.getIntColor()));
            drawnLines.add(new PersistentLines(right, top, right, bottom, pixelSize, ColorPicker.getIntColor()));

        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ===============================
    // GUI Setup
    // ===============================

    @Override
    protected void init() {
        //clear boxes
        drawnBoxes.clear();
        previewBox = null;
        //clear line
        previewLine = null;
        System.out.println("INIT RAN");


        //This dynamically changes the size of each pixel,
        //TODO: ENSURE bigger brush sizes do not go out of bounds (add clipping)
        SliderWidget brushSizeSlider = new SliderWidget(0,0,200,20,
                Text.literal("Brush Size: " + pixelSize),
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

        ButtonWidget toggleButton = ButtonWidget.builder(Text.of("Tool : " + (currentTool)), (button) -> {

            //goes to next tool when click on button (change later)
            currentTool = nextTool(currentTool);

            button.setMessage(Text.of("Tool : " + (currentTool)));
        }).dimensions(40, 40, 120, 20).build();
        addDrawableChild(toggleButton);


        //this is really disgusting with perma typecasting, probably make it more clean later
        AlphaSlider alphaSlider = new AlphaSlider(width-100,20,100,20, Text.literal("Alpha: " + (int)(ColorPicker.alpha*255)), ColorPicker.alpha);
        addDrawableChild(alphaSlider);

        HueSlider hueSlider = new HueSlider(width-100, 80, 100, 20, Text.literal("Hue: " + ColorPicker.hue), ColorPicker.hue);
        addDrawableChild(hueSlider);

        ColorPicker colorPicker = new ColorPicker(width-100, 40, 100, 40, Text.literal("test"));
        addDrawableChild(colorPicker);
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
                    //just temp paintbrush paints with colour, pencil does not
                    int rgb = java.awt.Color.HSBtoRGB(ColorPicker.hue,ColorPicker.saturation,ColorPicker.brightness);
                    int argb = ((int)(ColorPicker.alpha*255) << 24) | (rgb & 0x00FFFFFF);

                    drawnPixels.add(new Pixel(mouseX, mouseY, argb, pixelSize));

                }
                if (currentTool == Tool.PENCIL) {
                    //pencil tool uses size of 1 no matter what
                    //prob useless
                    //drawnPixels.add(new Pixel(mouseX, mouseY, white, 1));

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
        //loop through all boxes and draw them
        //TODO: probably add boxes as a seperate data type holding 4x lines (that i then loop through)
//        for (PersistentLines box : drawnBoxes ) {
//
//            RenderUtils.drawHollowBox(context, (int) box.x1, (int) box.y1, (int) box.x2, (int) box.y2, box.color, (int)box.width);
//        }
        // Draw preview line (if any)
        if (previewLine != null) {
            drawLine(context, previewLine.x1, previewLine.y1, previewLine.x2, previewLine.y2, previewLine.width, previewLine.color);
        }
        //draw square if square tool
        if (previewBox != null) {

            float left = Math.min(previewBox.x1, previewBox.x2);
            float top = Math.min(previewBox.y1, previewBox.y2);
            float right = Math.max(previewBox.x1, previewBox.x2);
            float bottom = Math.max(previewBox.y1, previewBox.y2);

            drawLine(context, left, top, right, top, previewBox.width, previewBox.color);
            drawLine(context, left, bottom, right, bottom, previewBox.width, previewBox.color);
            drawLine(context, left, top, left, bottom, previewBox.width, previewBox.color);
            drawLine(context, right, top, right, bottom, previewBox.width, previewBox.color);

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

        //render all tool icons here
        renderToolIcons(context);

        //(probably)render color picker here
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

    /**
     * Interpolates extra drawn pixels between 2 points (smoother drawing)
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param color
     * @param size
     */
    private void drawInterpolated(double x0, double y0, double x1, double y1, int color, int size) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dist = Math.hypot(dx, dy); // sqrt(dx^2 + dy^2)

        // spacing controls smoothness
        double step = Math.max(1.0, size * 0.5);

        int steps = (int) (dist / step);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) (x0 + dx * t);
            int y = (int) (y0 + dy * t);
            drawnPixels.add(new Pixel(x, y, color, size));
        }
    }
}