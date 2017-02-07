package nl.xs4all.pebbe.vrfracland;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

public class FracLand {

    private static final int LVL = 64; // gehele macht van 2
    private static final int ARRAY_SIZE = 9 * (LVL * LVL + 6 * LVL + 2);

    // lichtbron:
    private static final float X = -4;
    private static final float Y = 10;
    private static final float Z = 2;
    private static final float lenXYZ = 10.954451f; // sqrt(X^2 + Y^2 + Z^2)

    private static final float SUB = -.2f;
    private static final float DOWN = .8f;

    private static final float GreyR = .37647f;
    private static final float GreyG = .490196f;
    private static final float GreyB = .545098f;


    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMatrixHandle;

    private final static float hSqrt3 = .8660254f;

    private final String vertexShaderCode = "" +
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec3 vertexColor;" +
            "varying vec3 color;" +
            "void main() {" +
            "    gl_Position = uMVPMatrix * vPosition;" +
            "    color = vertexColor;" +
            "}";

    private final String fragmentShaderCode = "" +
            "precision mediump float;" +
            "varying vec3 color;" +
            "void main() {" +
            "    gl_FragColor = vec4(color, 1.0);" +
            "}";

    static final int COORDS_PER_VERTEX = 3;
    static float coords[] = new float[ARRAY_SIZE];
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    static final int COLORS_PER_VERTEX = 3;
    static float colors[] = new float[ARRAY_SIZE];
    private final int colorStride = COLORS_PER_VERTEX * 4; // 4 bytes per vertex

    private int vertexCount;

    private float y[][] = new float[LVL + 1][LVL + 1];

    private float toX(int x, int z) {
        return 2 * (((float) x) / ((float) LVL) + .5f * ((float) z) / (float) LVL) - 1;
    }

    private float toZ(int z) {
        return 2 * (.8660254f * ((float) z) / (float) LVL) - .57735f;
    }

    void driehoek(int x1, float y1, int z1, int x2, float y2, int z2, int x3, float y3, int z3, float r, float g, float b) {
        coords[3 * vertexCount] = toX(x1, z1);
        coords[3 * vertexCount + 1] = y1 - DOWN;
        coords[3 * vertexCount + 2] = toZ(z1);
        colors[3 * vertexCount] = r;
        colors[3 * vertexCount + 1] = g;
        colors[3 * vertexCount + 2] = b;
        vertexCount++;

        coords[3 * vertexCount] = toX(x2, z2);
        coords[3 * vertexCount + 1] = y2 - DOWN;
        coords[3 * vertexCount + 2] = toZ(z2);
        colors[3 * vertexCount] = r;
        colors[3 * vertexCount + 1] = g;
        colors[3 * vertexCount + 2] = b;
        vertexCount++;

        coords[3 * vertexCount] = toX(x3, z3);
        coords[3 * vertexCount + 1] = y3 - DOWN;
        coords[3 * vertexCount + 2] = toZ(z3);
        colors[3 * vertexCount] = r;
        colors[3 * vertexCount + 1] = g;
        colors[3 * vertexCount + 2] = b;
        vertexCount++;
    }

    private float cos1(int xi, int zi) {
        float xn, yn, zn, ax, ay, az, bx, by, bz;

        ax = .5f;
        ay = LVL*(y[xi][zi +1] - y[xi][zi]);
        az = hSqrt3;

        bx = 1;
        by = LVL*(y[xi + 1][zi] - y[xi][zi]);
        bz = 0;

        // normaalvector:
        xn = ay * bz - az * by;
        yn = az * bx - ax * bz;
        zn = ax * by - ay * bx;

        return (xn * X + yn * Y + zn * Z) / (float)Math.sqrt(xn * xn + yn * yn + zn * zn) / lenXYZ;
    }

    private float cos2(int xi, int zi) {
        float xn, yn, zn, ax, ay, az, bx, by, bz;

        ax = -.5f;
        ay = LVL*(y[xi][zi+1] - y[xi+1][zi]);
        az = hSqrt3;

        bx = .5f;
        by = LVL*(y[xi+1][zi+1] - y[xi+1][zi]);
        bz = hSqrt3;

        // normaalvector:
        xn = ay*bz - az*by;
        yn = az*bx - ax*bz;
        zn = ax*by - ay*bx;

        return (xn*X + yn*Y + zn*Z) / (float)Math.sqrt(xn*xn+yn*yn+zn*zn) / lenXYZ;
}

    private float min0(float f) {
        if (f < 0) {
            return 0;
        }
        return f;
    }

    public void init(long seed) {

        Random rnd = new Random();
        rnd.setSeed(seed);

        vertexCount = 0;

        driehoek(
                0, 0, 0,
                0, 0, LVL,
                LVL, 0, 0,
                0, .3f, .7f);

        driehoek(
                0, SUB, 0,
                LVL, SUB, 0,
                0, SUB, LVL,
                GreyR / 5 * 2, GreyG / 5 * 2, GreyB / 5 * 2);

        y[0][0] = rnd.nextFloat() * 2 - 1;
        y[LVL][0] = rnd.nextFloat() * 2 - 1;
        y[0][LVL] = rnd.nextFloat() * 2 - 1;

        for (int step = LVL; step > 1; step /= 2) {
            float mul = ((float) step) / (float) LVL;
            for (int x = 0; x < LVL; x += step) {
                for (int z = 0; x + z < LVL; z += step) {
                    y[x + step / 2][z] = (y[x][z] + y[x + step][z]) / 2 + (rnd.nextFloat() * 2 - 1) * mul;
                    y[x][z + step / 2] = (y[x][z] + y[x][z + step]) / 2 + (rnd.nextFloat() * 2 - 1) * mul;
                    y[x + step / 2][z + step / 2] = (y[x][z + step] + y[x + step][z]) / 2 + (rnd.nextFloat() * 2 - 1) * mul;
                }
            }
        }

        for (int x = 0; x < LVL; x++) {
            for (int z = 0; x + z < LVL; z++) {
                if (y[x][z] > 0 || y[x][z + 1] > 0 || y[x + 1][z] > 0) {
                    float c = cos1(x, z);
                    driehoek(
                            x, y[x][z], z,
                            x, y[x][z + 1], z + 1,
                            x + 1, y[x + 1][z], z,
                            0, .5f * c + .5f, 0);
                }
                if (x + z < LVL - 1 && (y[x][z + 1] > 0 || y[x + 1][z] > 0 || y[x + 1][z + 1] > 0)) {
                    float c = cos2(x, z);
                    driehoek(
                            x, y[x][z + 1], z + 1,
                            x + 1, y[x + 1][z + 1], z + 1,
                            x + 1, y[x + 1][z], z,
                            0, .5f * c + .5f, 0);
                }
            }
            driehoek(
                    0, min0(y[0][x]), x,
                    0, SUB, x,
                    0, min0(y[0][x + 1]), x + 1,
                    GreyR, GreyG, GreyB);
            driehoek(
                    0, SUB, x,
                    0, SUB, x + 1,
                    0, min0(y[0][x + 1]), x + 1,
                    GreyR, GreyG, GreyB);
            driehoek(
                    x, SUB, 0,
                    x, min0(y[x][0]), 0,
                    x + 1, min0(y[x + 1][0]), 0,
                    GreyR / 5 * 4, GreyG / 5 * 4, GreyB / 5 * 4);
            driehoek(
                    x + 1, SUB, 0,
                    x, SUB, 0,
                    x + 1, min0(y[x + 1][0]), 0,
                    GreyR / 5 * 4, GreyG / 5 * 4, GreyB / 5 * 4);
            driehoek(
                    x, min0(y[x][LVL - x]), LVL - x,
                    x, SUB, LVL - x,
                    x + 1, min0(y[x + 1][LVL - x - 1]), LVL - x - 1,
                    GreyR / 5 * 3, GreyG / 5 * 3, GreyB / 5 * 3);
            driehoek(
                    x, SUB, LVL - x,
                    x + 1, SUB, LVL - x - 1,
                    x + 1, min0(y[x + 1][LVL - x - 1]), LVL - x - 1,
                    GreyR / 5 * 3, GreyG / 5 * 3, GreyB / 5 * 3);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(4 * ARRAY_SIZE);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(coords);
        vertexBuffer.position(0);

        ByteBuffer cb = ByteBuffer.allocateDirect(4 * ARRAY_SIZE);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);
    }

    public FracLand() {
        int vertexShader = Util.loadShader(
                GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = Util.loadShader(
                GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        Util.checkGlError("glAttachShader vertexShader");
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        Util.checkGlError("glAttachShader fragmentShader");
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
        Util.checkGlError("glLinkProgram");
    }

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        Util.checkGlError("glUseProgram");

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        Util.checkGlError("glGetAttribLocation vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        Util.checkGlError("glEnableVertexAttribArray vPosition");
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);
        Util.checkGlError("glVertexAttribPointer vPosition");

        mColorHandle = GLES20.glGetAttribLocation(mProgram, "vertexColor");
        Util.checkGlError("glGetAttribLocation vertexColor");
        GLES20.glEnableVertexAttribArray(mColorHandle);
        Util.checkGlError("glEnableVertexAttribArray vertexColor");
        GLES20.glVertexAttribPointer(
                mColorHandle, COLORS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                colorStride, colorBuffer);
        Util.checkGlError("glVertexAttribPointer vertexColor");

        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        Util.checkGlError("glGetUniformLocation uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mvpMatrix, 0);
        Util.checkGlError("glUniformMatrix4fv uMVPMatrix");

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        Util.checkGlError("glDrawArrays");

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mColorHandle);
        Util.checkGlError("glDisableVertexAttribArray mColorHandle");
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        Util.checkGlError("glDisableVertexAttribArray mPositionHandle");
    }

}
