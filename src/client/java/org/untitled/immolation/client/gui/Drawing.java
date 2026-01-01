package org.untitled.immolation.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

//render methods like drawSquare / drawLine / etc
import static org.untitled.immolation.client.gui.RenderUtils.*;

public class Drawing extends Screen {

    //Basic Pixel object containing x y color and size
    private static record Pixel(int x, int y, int color, int size) {}

    //drawStack stores all drawing commands in order(e.g Draw a line, draw a box, draw a circle)
    private final List<DrawCommand> drawStack = new ArrayList<>();

    private static PersistentLines previewLine = null;
    private static PersistentLines previewBox = null; // to be used in Square tool
    private static List<Pixel> previewCircle = null;

    private boolean onCanvas = false;

    private int pixelSize = 1;
    private boolean isMouseDown = false;
    private int x, y;

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


    // Each DrawCommand must have draw + erase functions
    interface DrawCommand {
        void draw(DrawContext context);
        List<DrawCommand> eraseAt(int x, int y, int radius);
    }

    // Represents a collection of individual pixels (like a brush stroke)
    class PixelCommand implements DrawCommand {
        private final List<Pixel> pixels;

        PixelCommand(List<Pixel> pixels) {
            this.pixels = pixels;
        }

        @Override
        public void draw(DrawContext context) {
            for (Pixel p : pixels) {
                context.fill(p.x, p.y, p.x + p.size, p.y + p.size, p.color);
            }
        }

        @Override
        public List<DrawCommand> eraseAt(int x, int y, int r) {
            pixels.removeIf(p -> Math.hypot(p.x - x, p.y - y) < r);
            if (pixels.isEmpty()) {
                return List.of(); // fully erased
            } else {
                return List.of(this); // still has pixels remaining
            }
        }
    }

    // Represents a rectangular shape made of 4 edges (lines)
    class BoxCommand implements DrawCommand {
        List<LineCommand> edges;
        public BoxCommand(List<PersistentLines> lines) {
            edges = new ArrayList<>();
            for (PersistentLines l : lines) {

                edges.add(new LineCommand(l));
            }
        }
        public void draw(DrawContext context) {
            for (LineCommand edge : edges) {
                edge.draw(context);
            }
        }

        public List<DrawCommand> eraseAt(int x, int y, int r) {
            List<LineCommand> remainingEdges = new ArrayList<>();

            for (LineCommand edge : edges) {
                List<DrawCommand> erasedSegments = edge.eraseAt(x, y, r);
                for (DrawCommand cmd : erasedSegments) {
                    remainingEdges.add((LineCommand) cmd);
                }
            }

            if (remainingEdges.isEmpty()) {
                return new ArrayList<>();
            }

            BoxCommand updatedBox = new BoxCommand(new ArrayList<>()); // empty constructor
            updatedBox.edges = remainingEdges;
            return List.of(updatedBox);
        }
    }
    // Represents a single straight line (x0,y0 -> x1,y1)
    class LineCommand implements DrawCommand {

        private final List<Pixel> pixels;
        public LineCommand(PersistentLines line) {
            this.pixels = lineToPixels(
                    line.x1, line.y1,
                    line.x2, line.y2,
                    line.width,
                    line.color
            );
        }
        @Override
        public void draw(DrawContext context) {
            for (Pixel p : pixels) {
                context.fill(p.x(), p.y(), p.x() + 1, p.y() + 1, p.color());
            }
        }

        @Override
        public List<DrawCommand> eraseAt(int x, int y, int r) {

            int rSquared = (r * r);

            pixels.removeIf(p ->
                    distSquared(p.x(), p.y(), x, y) <= rSquared
            );

            return pixels.isEmpty() ? List.of() : List.of(this);
        }
    }
    // Represents the pixels that make up the circle
    class CircleCommand implements DrawCommand {
        List<Pixel> pixels;
        public CircleCommand (List<Pixel> circle) {
            this.pixels = circle;
        }
        public void draw(DrawContext context) {
            for (Pixel p : pixels) {
                context.fill(p.x, p.y, p.x + p.size, p.y + p.size, p.color);
            }
        }

        @Override
        public List<DrawCommand> eraseAt(int x, int y, int r) {
            pixels.removeIf(p -> Math.hypot(p.x - x, p.y - y) <= r); //if point is within the circle radius of point it gets removed
            return pixels.isEmpty() ? new ArrayList<>() : List.of(this);
        }
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

    private int clampCanvasX(double mx) {
        return (int) Math.max(canvasX(), Math.min(mx, canvasX() + canvasWidth()));
    }

    private int clampCanvasY(double my) {
        return (int) Math.max(canvasY(), Math.min(my, canvasY() + canvasHeight()));
    }

    private static int distSquared(int x1,int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy;
    }
    // ===============================
    // Event handling
    // ===============================

    /**
     * Called when mouseclicked, applying appropriate pixel strokes and setting globals.
     * @param mouseX
     * @param mouseY
     * @param button
     * @return
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInCanvas(mouseX, mouseY)) {
            isMouseDown = true;
            onCanvas = true;
            x = (int) mouseX;
            y = (int) mouseY;
        } else {
            onCanvas = false;
        }
        if(isInCanvas(mouseX+pixelSize, mouseY+pixelSize) && isMouseDown) {
            //Ensures that you can erase/paint single pixels from a still mouse click
            if (currentTool == Tool.PAINTBRUSH){

                List<Pixel> stroke = new ArrayList<>();
                stroke.add(new Pixel((int)mouseX, (int)mouseY, ColorPicker.getIntColor(), pixelSize));
                drawStack.add(new PixelCommand(stroke));

            }
            if (currentTool == Tool.PENCIL) {
                List<Pixel> stroke = new ArrayList<>();
                stroke.add(new Pixel((int)mouseX, (int)mouseY, ColorPicker.getIntColor(), 1));
                drawStack.add(new PixelCommand(stroke));
            }
            if (currentTool == Tool.ERASER) {
                List<DrawCommand> newStack = new ArrayList<>();
                for (DrawCommand cmd : drawStack) {
                    newStack.addAll(cmd.eraseAt( (int) mouseX, (int) mouseY, pixelSize));
                }
                drawStack.clear();
                drawStack.addAll(newStack);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    // EraseAt+trimLine method from GPT

    /**
     * Erases part of a line if it overlaps with provided circle (cx,cy,r) and returns the new line(s)
     * @param line
     * @param cx
     * @param cy
     * @param r
     * @return
     */
    private List<PersistentLines> trimLineWithCircle(PersistentLines line, double cx, double cy, double r) {
        int x1 = line.x1;
        int y1 = line.y1;
        int x2 = line.x2;
        int y2 = line.y2;

        float dx = x2 - x1;
        float dy = y2 - y1;
        double fx = x1 - cx;
        double fy = y1 - cy;

        double a = dx * dx + dy * dy;
        double b = 2 * (fx * dx + fy * dy);
        double c = (fx * fx + fy * fy) - r * r;

        double D = b * b - 4 * a * c; //discriminant
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
        double ix1 = (x1 + t1 * dx);
        double iy1 = (y1 + t1 * dy);
        double ix2 = (x1 + t2 * dx);
        double iy2 = (y1 + t2 * dy);

        // Add
        if (t1 > 0.01) {
            result.add(new PersistentLines(x1, y1, (int)ix1, (int)iy1, line.width, line.color));
        }
        if (t2 < 0.99) {
            result.add(new PersistentLines((int)ix2, (int)iy2, x2, y2, line.width, line.color));
        }

        return result;
    }

    /**
     * Handles mouse dragged events over the canvas
     * Paintbrush - draws pixels at current position (mouseX, mouseY) with size = pixelSize
     * Pencil - draws pixels at current position (mouseX, mouseY) with size always = 1
     * Eraser - removes pixels that intersect at current position (mouseX, mouseY)
     * Line - updates a preview line from the drag start to current position (mouseX, mouseY)
     * Square - updates a preview rectangle bounded by the drag start to current position (mouseX, mouseY)
     * Circle - updates a preview circle bounded by the drag start to current position (mouseX, mouseY)
     * @param mouseX - current x coordinate of the mouse
     * @param mouseY - current y coordinate of the mouse
     * @param button - mouse button being held
     * @param deltaX - change in x since last mouseDragged event
     * @param deltaY - change in y since last mouseDragged event
     * @return
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if(isInCanvas(mouseX+pixelSize, mouseY+pixelSize) && isMouseDown) {
            if (currentTool == Tool.PAINTBRUSH){

                List<Pixel> stroke = new ArrayList<>();
                stroke.add(new Pixel((int)mouseX, (int)mouseY, ColorPicker.getIntColor(), pixelSize));
                drawStack.add(new PixelCommand(stroke));

            }
            if (currentTool == Tool.PENCIL) {
                //Pencil is ALWAYS 1 thick and smoother drawing
                List<Pixel> stroke = new ArrayList<>();
                drawInterpolated(x,y,mouseX,mouseY,ColorPicker.getIntColor(), 1, stroke);
                x = (int) mouseX;
                y = (int) mouseY;
                drawStack.add(new PixelCommand(stroke));

            }
        }
        if (isInCanvas(mouseX, mouseY) && isMouseDown) {
            if (currentTool == Tool.LINE) {
                int clampedX = clampCanvasX(mouseX);
                int clampedY = clampCanvasY(mouseY);
                previewLine = new PersistentLines((int) x, (int) y, clampedX, clampedY, pixelSize, ColorPicker.getIntColor());
            }
            if (currentTool == Tool.ERASER) {
                //TODO: ERASING WITH SMALLER SIZE THAN DRAWN PIXELS IS INSANELY SCUFFED (JUST WORKS ON TOP LEFT CORNER)
                List<DrawCommand> newStack = new ArrayList<>();
                for (DrawCommand cmd : drawStack) {
                    newStack.addAll(cmd.eraseAt( (int) mouseX, (int) mouseY, pixelSize));
                }
                drawStack.clear();
                drawStack.addAll(newStack);
                //eraseAt(mouseX, mouseY);
            }
            if (currentTool == Tool.SQUARE ) {
                int clampedX = clampCanvasX(mouseX);
                int clampedY = clampCanvasY(mouseY);
                previewBox = new PersistentLines((int) x,(int) y, clampedX, clampedY, pixelSize, ColorPicker.getIntColor());
            }
            if (currentTool == Tool.CIRCLE) {
                previewCircle = drawCircle(x, y, mouseX, mouseY, pixelSize, ColorPicker.getIntColor());
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    /**
     * Completes the drawing operation on mouse button released
     *
     * Adds any previewed shape (line, square, circle) to the draw stack if released within the canvas.
     * Discards the previewed shape if released outside the canvas.
     *
     * @param mouseX - current x coordinate of the mouse
     * @param mouseY - current y coordinate of the mouse
     * @param button - mouse button being released
     * @return
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isMouseDown = false;
        //mostly works
        if (this.client != null && !isInCanvas(mouseX, mouseY))  {

            previewLine = null; //remove previewline it shouldnt matter tho bc it has explicit checking in draw
            previewBox = null;
            previewCircle = null;
        }
        if (isInCanvas(mouseX,mouseY) && currentTool == Tool.LINE && onCanvas) {
            int clampedX = clampCanvasX(mouseX);
            int clampedY = clampCanvasY(mouseY);
            drawStack.add(new LineCommand(new PersistentLines((int) x, (int) y, clampedX, clampedY, pixelSize, ColorPicker.getIntColor())));

        }
        if (isInCanvas(mouseX,mouseY) && currentTool == Tool.SQUARE && onCanvas) {

            int left = Math.min(previewBox.x1, previewBox.x2);
            int top = Math.min(previewBox.y1, previewBox.y2);
            int right = Math.max(previewBox.x1, previewBox.x2);
            int bottom = Math.max(previewBox.y1, previewBox.y2);

            List<PersistentLines> box = new ArrayList<>();

            box.add(new PersistentLines(left, top, right, top, pixelSize, ColorPicker.getIntColor()));
            box.add(new PersistentLines(left, bottom, right, bottom, pixelSize, ColorPicker.getIntColor()));
            box.add(new PersistentLines(left, top, left, bottom, pixelSize, ColorPicker.getIntColor()));
            box.add(new PersistentLines(right, top, right, bottom, pixelSize, ColorPicker.getIntColor()));
            drawStack.add(new BoxCommand(box));
        }
        if (isInCanvas(mouseX, mouseY) && currentTool == Tool.CIRCLE) {

            List<Pixel> circle = drawCircle(x, y, mouseX, mouseY, pixelSize, ColorPicker.getIntColor());
            drawStack.add(new CircleCommand(circle));
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ===============================
    // GUI Setup
    // ===============================

    /**
     * Sets up the window, drawing elements and ensuring previews are null to start.
     */
    @Override
    protected void init() {
        //clear boxes
        //drawnBoxes.clear();
        previewBox = null;
        //clear line
        previewCircle = null;
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

        //Draw parts of the ColorPicker
        AlphaSlider alphaSlider = new AlphaSlider(width-100,20,100,20, Text.literal("Alpha: " + (int)(ColorPicker.alpha*255)), ColorPicker.alpha);
        addDrawableChild(alphaSlider);

        HueSlider hueSlider = new HueSlider(width-100, 80, 100, 20, Text.literal("Hue: " + ColorPicker.hue), ColorPicker.hue);
        addDrawableChild(hueSlider);

        ColorPicker colorPicker = new ColorPicker(width-100, 40, 100, 40, Text.literal("test"));
        //Ensure the ColorPicker gets reset back to white on load.
        ColorPicker.saturation = 0f;
        ColorPicker.brightness = 1f;
        ColorPicker.alpha = 1f;
        addDrawableChild(colorPicker);
    }

    // ===============================
    // Rendering
    // ===============================

    /**
     * Draws the shapes from the drawStack to the screen every frame.
     *
     * @param context current DrawContext (current screen)
     * @param mouseX - current x coordinate of the mouse
     * @param mouseY - current y coordinate of the mouse
     * @param delta - (TODO: look into what the delta is in render)
     */
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


        //Draw all elements THEN the previewShape
        for (DrawCommand cmd: drawStack) {
            cmd.draw(context);
        }

        if (previewLine != null) {
            drawLine(context, previewLine.x1, previewLine.y1, previewLine.x2, previewLine.y2, (int) previewLine.width, previewLine.color);
        }
        //draw square if square tool
        if (previewBox != null) {

            float left = Math.min(previewBox.x1, previewBox.x2);
            float top = Math.min(previewBox.y1, previewBox.y2);
            float right = Math.max(previewBox.x1, previewBox.x2);
            float bottom = Math.max(previewBox.y1, previewBox.y2);

            drawLine(context, left, top, right, top, (int) previewBox.width, previewBox.color);
            drawLine(context, left, bottom, right, bottom, (int) previewBox.width, previewBox.color);
            drawLine(context, left, top, left, bottom, (int) previewBox.width, previewBox.color);
            drawLine(context, right, top, right, bottom, (int) previewBox.width, previewBox.color);

        }
        if (previewCircle != null) {
            //kinda scuffed preview circle shifting around if you move mouse backwards but it works
            for (Pixel p : previewCircle) {

                context.fill(p.x, p.y, p.x+p.size, p.y+p.size, p.color);

            }
        }

        //render all tool icons here
        //renderToolIcons(context);

    }

    /**
     * Renders all the drawing tool icons to the current screen
     * @param context - DrawContext (current screen)
     */
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
     * Interpolates extra drawn pixels between 2 points (x1,y1), (x2,y2) (smoother drawing)
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param color
     * @param size
     */
    private void drawInterpolated(double x1, double y1, double x2, double y2, int color, int size, List<Pixel> lp) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dist = Math.hypot(dx, dy); // sqrt(dx^2 + dy^2)

        // spacing controls smoothness
        double step = Math.max(1.0, size * 0.5);

        int steps = (int) (dist / step);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) (x1 + dx * t);
            int y = (int) (y1 + dy * t);
            lp.add(new Pixel(x, y, color, size));
        }
    }


    /**
     * Helper method for adding points to the drawn circle
     * @param xc
     * @param yc
     * @param x
     * @param y
     * @param size
     * @param color
     * @param circle
     */
    private void plotPoints(int xc, int yc, int x, int y, int size, int color, List<Pixel> circle) {
        circle.add(new Pixel(xc+x, yc+y, color, size));
        circle.add(new Pixel(xc-x, yc+y, color, size));
        circle.add(new Pixel(xc+x, yc-y, color, size));
        circle.add(new Pixel(xc-x, yc-y, color, size));
        circle.add(new Pixel(xc+y, yc+x, color, size));
        circle.add(new Pixel(xc-y, yc+x, color, size));
        circle.add(new Pixel(xc+y, yc-x, color, size));
        circle.add(new Pixel(xc-y, yc-x, color, size));
    }

    /**
     * Draws circle using the Bresenham Circle Algorithm, adding the points to an array to be drawn
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param size
     * @param color
     * @return
     */
    private List<Pixel> drawCircle(double x1, double y1, double x2, double y2, int size, int color) {
        List<Pixel> circle = new ArrayList<>();

        double left   = Math.min(x1, x2);
        double right  = Math.max(x1, x2);
        double top    = Math.min(y1, y2);
        double bottom = Math.max(y1, y2);

        int width  = (int) (right - left);
        int height = (int) (bottom - top);

        // center of the bounding box
        int xc = ((int) x1 + (int) x2) / 2;
        int yc = ((int) y1 + (int) y2) / 2;

        int r = Math.min(width, height) / 2;

        int curX = 0;
        int curY = r;
        int d = 3 - 2 * r;

        while (curY >= curX) {
            plotPoints(xc, yc, curX, curY, size, color, circle);

            if (d > 0) {
                curY--;
                d += 4 * (curX - curY) + 10;
            } else {
                d += 4 * curX + 6;
            }
            curX++;
        }

        return circle;
    }

    /**
     * Draws line from x1,y1 to x2,y2 using Bresenham's Line algorithm
     * @param context
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param width
     * @param color
     */
    public static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, int width, int color) {
        int intx1 = Math.round(x1);
        int inty1 = Math.round(y1);
        int intx2 = Math.round(x2);
        int inty2 = Math.round(y2);

        int dx = Math.abs(intx2 - intx1);
        int dy = Math.abs(inty2 - inty1);

        int sx = intx1 < intx2 ? 1 : -1; //step in x direction (go back or forward 1)
        int sy = inty1 < inty2 ? 1 : -1; //step in y direction (go back or forward 1)

        int err = dx - dy;
        int half = width / 2;
        while (true) {
            // Draw a small square centered on the current point to simulate line width

            context.fill(intx1 - half, inty1 - half, intx1 + half + 1, inty1 + half + 1, color);

            if (intx1 == intx2 && inty1 == inty2) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                intx1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                inty1 += sy;
            }
        }
    }
    public static List<Pixel> lineToPixels(int x1, int y1, int x2, int y2, int width, int color) {
        List<Pixel> out = new ArrayList<>();

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        int err = dx - dy;
        int half = width / 2;

        while (true) {
            // expand thickness
            for (int ox = -half; ox <= half; ox++) {
                for (int oy = -half; oy <= half; oy++) {
                    out.add(new Pixel(x1 + ox, y1 + oy, color, 1));
                }
            }

            if (x1 == x2 && y1 == y2) break;

            int e2 = err << 1;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }

        return out;
    }
}
