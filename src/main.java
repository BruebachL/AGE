import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class main extends JComponent {
    static JFrame frame = new JFrame();
    static mesh meshCube;
    static float fTheta;
    static float fNear = 0.1f;
    static float fFar = 1000.0f;
    static float fFov = 90.0f;
    static float fAspectRatio = 1.0f;
    static float fFovRad = 1.0f / (float)Math.tan(fFov * 0.5f/180.0f*3.14159f);
    static mat4x4 proj = new mat4x4();
    static vector camera = new vector(0,0,0);

    public static void main(String[] args) throws InterruptedException, IOException {
        long start = System.currentTimeMillis();
        frame.setSize(500, 500);
        //frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setFocusable(true);
        frame.requestFocusInWindow();
        meshCube = new mesh();

        meshCube.loadFromObjectFile("/home/ascor/Desktop/teapot.obj");
        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                List<triangle> trianglesToRaster = new ArrayList<>();
                for (triangle t : meshCube.tris) {

                    triangle triTransformed = new triangle(t.p[0],t.p[1],t.p[2]);

                    mat4x4 matProj = matrixMakeProjection(90,(float)(frame.getHeight()/frame.getWidth()),0.1f, 1000.0f,1.0f / (float)Math.tan(90.0f * 0.5f/180.0f*3.14159f));

                    mat4x4 matRotZ = matrixMakeRotationZ(fTheta);
                    mat4x4 matRotY = matrixMakeRotationY(fTheta);
                    mat4x4 matRotX = matrixMakeRotationX(fTheta);

                    mat4x4 matTrans = matrixMakeTranslation(0.0f,0.0f,16.0f);
                    mat4x4 matWorld = matrixMakeIdentity();
                    //matWorld = matrixMultiplyMatrix(matRotZ,matRotY);
                    //matWorld = matrixMultiplyMatrix(matWorld, matRotZ);
                    matWorld = matrixMultiplyMatrix(matWorld, matRotY);
                    matWorld = matrixMultiplyMatrix(matWorld, matRotX);
                    matWorld = matrixMultiplyMatrix(matWorld,matTrans);


                    //offset to fit viewport

                    triTransformed.p[0] = matrixMultiplyVector(t.p[0],matWorld);
                    triTransformed.p[1] = matrixMultiplyVector(t.p[1],matWorld);
                    triTransformed.p[2] = matrixMultiplyVector(t.p[2],matWorld);

                    triTransformed.p[0].z = triTransformed.p[0].z + 9.5f;
                    triTransformed.p[1].z = triTransformed.p[1].z + 9.5f;
                    triTransformed.p[2].z = triTransformed.p[2].z + 9.5f;

                    vector line1 = vectorSub(triTransformed.p[1],triTransformed.p[0]);
                    vector line2 = vectorSub(triTransformed.p[2],triTransformed.p[0]);
                    vector normal = vectorCrossproduct(line1,line2);
                    normal = vectorNormalize(normal);

                    vector vCameraRay = vectorSub(triTransformed.p[0], camera);

                    if(vectorDot(normal,vCameraRay)<0.0f) {

                        //Illumination

                        vector lightDirection = new vector(0,0,-1);
                        lightDirection = vectorNormalize(lightDirection);

                        //Projection

                        triangle triProjected = new triangle(triTransformed.p[0], triTransformed.p[1], triTransformed.p[2]);

                        triProjected.dp = Math.max(0.1f, vectorDot(lightDirection,normal));

                        triProjected.p[0] = matrixMultiplyVector(triTransformed.p[0], matProj);
                        triProjected.p[1] = matrixMultiplyVector(triTransformed.p[1], matProj);
                        triProjected.p[2] = matrixMultiplyVector(triTransformed.p[2], matProj);

                        triProjected.setzDepth();

                        triProjected.p[0] = vectorDiv(triProjected.p[0], triProjected.p[0].w);
                        triProjected.p[1] = vectorDiv(triProjected.p[1], triProjected.p[1].w);
                        triProjected.p[2] = vectorDiv(triProjected.p[2], triProjected.p[2].w);

                        // Scale into view
                        vector vOffsetView = new vector(1.0f,1.0f,0.0f);

                        triProjected.p[0] = vectorAdd(triProjected.p[0],vOffsetView);
                        triProjected.p[1] = vectorAdd(triProjected.p[1],vOffsetView);
                        triProjected.p[2] = vectorAdd(triProjected.p[2],vOffsetView);
                        triProjected.p[0].x *= 0.5f * (float)frame.getWidth();
                        triProjected.p[0].y *= 0.5f * (float)frame.getHeight();
                        triProjected.p[1].x *= 0.5f * (float)frame.getWidth();
                        triProjected.p[1].y *= 0.5f * (float)frame.getHeight();
                        triProjected.p[2].x *= 0.5f * (float)frame.getWidth();
                        triProjected.p[2].y *= 0.5f * (float)frame.getHeight();

                        //rasterize triangles

                        trianglesToRaster.add(triProjected);

                        // draw triangles

                    }
                }
                Collections.sort(trianglesToRaster, new sortByZ());
                Collections.reverse(trianglesToRaster);
                System.out.println("Sorted:");

                for(triangle current : trianglesToRaster){
                    Path2D path = new Path2D.Double();
                        path.moveTo(current.p[0].x, current.p[0].y);
                        path.lineTo(current.p[1].x, current.p[1].y);
                        path.lineTo(current.p[2].x, current.p[2].y);
                        path.closePath();
                        current.dp *= 255.0f;
                        if(current.dp<0||current.dp>255) {
                            current.dp = 255;
                        }
                        g.setColor(new Color((int)current.dp,(int)current.dp,(int)current.dp));
                        g2.fill(path);
                    /*g2.setColor(Color.white);
                    g2.draw(path);*/
                }

            }
        };
        frame.add(renderPanel,BorderLayout.CENTER);
        frame.repaint();
        frame.pack();
        frame.setSize(600, 600);
        while(true){
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            TimeUnit.MILLISECONDS.sleep(100);
            fTheta += 0.1f;
            renderPanel.repaint();
        }

    }
    public static vector multiplyMatrixVector(vector in, mat4x4 m){
        vector out = new vector(0,0,0);
        out.x = in.x * m.m[0][0] + in.y * m.m[1][0] + in.z * m.m[2][0] + m.m[3][0];
        out.y = in.x * m.m[0][1] + in.y * m.m[1][1] + in.z * m.m[2][1] + m.m[3][1];
        out.z = in.x * m.m[0][2] + in.y * m.m[1][2] + in.z * m.m[2][2] + m.m[3][2];
        float w = in.x * m.m[0][3] + in.y * m.m[1][3] + in.z * m.m[2][3] + m.m[3][3];
        if(w!=0.0f) {
            out.x /= w;
            out.y /= w;
            out.z /= w;
            return out;
        }
        System.out.println("W is 0.");
        return out;
    }
    public static vector matrixMultiplyVector(vector in, mat4x4 m){
        vector out = new vector(0,0,0);
        out.x = in.x * m.m[0][0] + in.y * m.m[1][0] + in.z * m.m[2][0] + in.w * m.m[3][0];
        out.y = in.x * m.m[0][1] + in.y * m.m[1][1] + in.z * m.m[2][1] + in.w * m.m[3][1];
        out.z = in.x * m.m[0][2] + in.y * m.m[1][2] + in.z * m.m[2][2] + in.w * m.m[3][2];
        out.w = in.x * m.m[0][3] + in.y * m.m[1][3] + in.z * m.m[2][3] + in.w * m.m[3][3];
        return out;
    }

    public static mat4x4 matrixMakeIdentity(){
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = 1.0f;
        matrix.m[1][1] = 1.0f;
        matrix.m[2][2] = 1.0f;
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    public static mat4x4 matrixMakeRotationX(float fAngleRad){
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = 1.0f;
        matrix.m[1][1] = (float)Math.cos((fAngleRad));
        matrix.m[1][2] = (float)Math.sin((fAngleRad));
        matrix.m[2][1] = (float)-Math.sin((fAngleRad));
        matrix.m[2][2] = (float)Math.cos((fAngleRad));
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    public static mat4x4 matrixMakeRotationY(float fAngleRad){
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = (float)Math.cos(fAngleRad);
        matrix.m[0][2] = (float)Math.sin(fAngleRad);
        matrix.m[2][0] = (float)-Math.sin(fAngleRad);
        matrix.m[1][1] = 1.0f;
        matrix.m[2][2] = (float)Math.cos(fAngleRad);
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    public static mat4x4 matrixMakeRotationZ(float fAngleRad){
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = (float)Math.cos(fAngleRad);
        matrix.m[0][1] = (float)Math.sin(fAngleRad);
        matrix.m[1][0] = (float)-Math.sin(fAngleRad);
        matrix.m[1][1] = (float)Math.cos(fAngleRad);
        matrix.m[2][2] = 1.0f;
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    public static mat4x4 matrixMakeTranslation(float x, float y, float z){
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = 1.0f;
        matrix.m[1][1] = 1.0f;
        matrix.m[2][2] = 1.0f;
        matrix.m[3][3] = 1.0f;
        matrix.m[3][0] = x;
        matrix.m[3][1] = y;
        matrix.m[3][2] = z;
        return matrix;
    }

    public static mat4x4 matrixMakeProjection(float fFovDegrees, float fAspectRatio, float fNear, float fFar, float fFovRad){
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = fAspectRatio *fFovRad;
        matrix.m[1][1] = fFovRad;
        matrix.m[2][2] = fFar / (fFar-fNear);
        matrix.m[3][2] = (-fFar*fNear)/(fFar-fNear);
        matrix.m[2][3] = 1.0f;
        matrix.m[3][3] = 0.0f;
        return matrix;
    }

    public static mat4x4 matrixMultiplyMatrix(mat4x4 a, mat4x4 b){
        mat4x4 matrix = new mat4x4();
        for(int c = 0; c < 4; c++){
            for(int r = 0; r < 4; r++){
                matrix.m[r][c] = a.m[r][0] * b.m[0][c] + a.m[r][1] * b.m[1][c] + a.m[r][2] * b.m[2][c] + a.m[r][3] * b.m[3][c];
            }
        }
        return matrix;
    }

    public static vector vectorAdd(vector a, vector b){
        return new vector(a.x+b.x,a.y+b.y,a.z+b.z);
    }

    public static vector vectorSub(vector a, vector b){
        return new vector(a.x-b.x,a.y-b.y,a.z-b.z);
    }

    public static vector vectorMul(vector a, float k){
        return new vector(a.x*k,a.y*k,a.z*k);
    }

    public static vector vectorDiv(vector a, float k){
        return new vector(a.x/k,a.y/k,a.z/k);
    }

    public static float vectorDot(vector a, vector b){
        return a.x*b.x + a.y*b.y +a.z*b.z;
    }

    public static float vectorLength(vector a){
        return  (float)Math.sqrt((double)(vectorDot(a,a)));
    }

    public static vector vectorNormalize(vector a){
        float l = vectorLength(a);
        return new vector(a.x/l, a.y/l, a.z/l);
    }

    public static vector vectorCrossproduct(vector a, vector b){
        vector v = new vector(0,0,0);
        v.x = a.y * b.z - a.z * b.y;
        v.y = a.z * b.x - a.x * b.z;
        v.z = a.x * b.y - a.y * b.x;
        return v;
    }
}
