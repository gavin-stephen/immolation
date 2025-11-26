package org.untitled.immolation.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL46;

import java.util.ArrayList;

public class RenderUtils {
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
    //x1 y1  is top left corner -> x2 y2 botom right
    public static void drawHollowBox(DrawContext context, int x1, int y1, int x2, int y2, int color, int thickness) {
        int left = Math.min(x1, x2);
        int top = Math.min(y1, y2);
        int right = Math.max(x1, x2);
        int bottom = Math.max(y1, y2);

        // Top
        context.fill(left, top, right, top + thickness, color);
        // Bottom
        context.fill(left, bottom - thickness, right, bottom, color);
        // Left
        context.fill(left, top, left + thickness, bottom, color);
        // Right
        context.fill(right - thickness, top, right, bottom, color);
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


    //Draw line always draws below paintbrush in drawcontext...
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



    public static void fillGradient(DrawContext context, float x1, float y1, float x2, float y2, int color1, int color2) {
        MatrixStack matrix = context.getMatrices();
        BufferBuilder buffer = getBufferBuilder(matrix, VertexFormat.DrawMode.QUADS);
        //preRender();
        //testing with manual pre here
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);



        Matrix4f pos = matrix.peek().getPositionMatrix();
        //add the 4 vertexes
        buffer.vertex(x2,y1,0).color(color1);
        buffer.vertex(x1,y1,0).color(color1);
        buffer.vertex(x1,y2,0).color(color2);
        buffer.vertex(x2,y2,0).color(color2);


        //postRender(buffer, matrix);
        //testing with manual here
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
    } // for now call with RenderUtils.fillGradient(context,500,500,900,900, Integer.parseInt("FFFF00", 16), Integer.parseInt("800080", 16));
    //TODO: set up CustomColor usage within Drawing tomorrow

    public static void fillSidewaysGradient(DrawContext context, float x1, float y1, float x2, float y2, int color1, int color2) {
        MatrixStack matrix = context.getMatrices();
        BufferBuilder buffer = getBufferBuilder(matrix, VertexFormat.DrawMode.QUADS);
        //preRender();
        //testing with manual pre here
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);



        Matrix4f pos = matrix.peek().getPositionMatrix();
        //add the 4 vertexes
        buffer.vertex(x1,y1,0).color(color1);
        buffer.vertex(x1,y2,0).color(color1);
        buffer.vertex(x2,y2,0).color(color2);
        buffer.vertex(x2,y1,0).color(color2);


        //postRender(buffer, matrix);
        //testing with manual here
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
    }










    /*
    // WILL IMPLEMENT LATER THIS IS JUST FOR LINE CLIPPING
    //
    //
     */
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
