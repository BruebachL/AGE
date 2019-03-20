import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
    static vector vCamera = new vector(0,0,0);
    static JLabel listener = new JLabel();
    static boolean rotateY = false;
    static boolean rotateX = false;
    static boolean rotateZ = false;

    public static void main(String[] args) throws InterruptedException, IOException {

        //swing management and init
        long start = System.currentTimeMillis();
        frame.setSize(1000, 1000);
        frame.getContentPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setFocusable(true);
        frame.requestFocusInWindow();
        frame.add(listener);
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("W"), "up");
        listener.getActionMap().put("up", new moveUp());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("S"), "down");
        listener.getActionMap().put("down", new moveDown());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("A"), "left");
        listener.getActionMap().put("left", new moveLeft());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("D"), "right");
        listener.getActionMap().put("right", new moveRight());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F"), "rotateX");
        listener.getActionMap().put("rotateX", new toggleXRot());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("G"), "rotateY");
        listener.getActionMap().put("rotateY", new toggleYRot());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("H"), "rotateZ");
        listener.getActionMap().put("rotateZ", new toggleZRot());


        //load and create model

        meshCube = new mesh();
        meshCube.loadFromObjectFile("C:\\Users\\Ascor\\Documents\\teapot.obj");

        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                mat4x4 matProj = matrixMakeProjection(90,(float)(frame.getHeight()/frame.getWidth()),0.1f, 1000.0f,1.0f / (float)Math.tan(90.0f * 0.5f/180.0f*3.14159f));

                mat4x4 matRotZ = matrixMakeRotationZ(fTheta);
                mat4x4 matRotY = matrixMakeRotationY(fTheta);
                mat4x4 matRotX = matrixMakeRotationX(fTheta);

                mat4x4 matTrans = matrixMakeTranslation(0.0f,0.0f,5.0f);
                mat4x4 matWorld = matrixMakeIdentity();
                //matWorld = matrixMultiplyMatrix(matRotZ,matRotY);
                if(rotateZ) {
                    matWorld = matrixMultiplyMatrix(matWorld, matRotZ);
                }
                if(rotateY) {
                    matWorld = matrixMultiplyMatrix(matWorld, matRotY);
                }
                    if(rotateX) {
                        matWorld = matrixMultiplyMatrix(matWorld, matRotX);
                    }
                matWorld = matrixMultiplyMatrix(matWorld,matTrans);

                vector lookDir = new vector(0,0,1);
                vector vUp = new vector(0,1,0);
                vector vTarget = vectorAdd(vCamera,lookDir);

                mat4x4 matCamera = matrixPointAt(vCamera, vTarget, vUp);

                //make view matrix from camera
                mat4x4 matView =  matrixQuickInverse(matCamera);

                List<triangle> trianglesToRaster = new ArrayList<>();
                for (triangle t : meshCube.tris) {

                    triangle triTransformed = new triangle(t.p[0],t.p[1],t.p[2]);

                    //offset to fit viewport

                    triTransformed.p[0] = matrixMultiplyVector(t.p[0],matWorld);
                    triTransformed.p[1] = matrixMultiplyVector(t.p[1],matWorld);
                    triTransformed.p[2] = matrixMultiplyVector(t.p[2],matWorld);

                    vector line1 = vectorSub(triTransformed.p[1],triTransformed.p[0]);
                    vector line2 = vectorSub(triTransformed.p[2],triTransformed.p[0]);
                    vector normal = vectorCrossproduct(line1,line2);
                    normal = vectorNormalize(normal);

                    vector vCameraRay = vectorSub(triTransformed.p[0], vCamera);

                    triangle triViewed = new triangle(t.p[0], t.p[1],t.p[2]);
                    triViewed.p[0] = matrixMultiplyVector(triTransformed.p[0], matView);
                    triViewed.p[1] = matrixMultiplyVector(triTransformed.p[1], matView);
                    triViewed.p[2] = matrixMultiplyVector(triTransformed.p[2], matView);

                    if(vectorDot(normal,vCameraRay)<0.0f) {

                        //Illumination

                        vector lightDirection = new vector(0,0,-1);
                        lightDirection = vectorNormalize(lightDirection);

                        //Convert world space to view space



                        //Projection

                        triangle triProjected = new triangle(triViewed.p[0], triViewed.p[1], triViewed.p[2]);

                        //get similarity of lightvector and vertexnormal

                        triProjected.dp = Math.max(0.1f, vectorDot(lightDirection,normal));

                        //actually project

                        triProjected.p[0] = matrixMultiplyVector(triViewed.p[0], matProj);
                        triProjected.p[1] = matrixMultiplyVector(triViewed.p[1], matProj);
                        triProjected.p[2] = matrixMultiplyVector(triViewed.p[2], matProj);

                        //store depth for sorting

                        triProjected.setzDepth();

                        //normalize the projection

                        triProjected.p[0] = vectorDiv(triProjected.p[0], triProjected.p[0].w);
                        triProjected.p[1] = vectorDiv(triProjected.p[1], triProjected.p[1].w);
                        triProjected.p[2] = vectorDiv(triProjected.p[2], triProjected.p[2].w);

                        // Scale into view
                        vector vOffsetView = new vector(1.0f,1.0f,0.0f);

                        triProjected.p[0] = vectorAdd(triProjected.p[0],vOffsetView);
                        triProjected.p[1] = vectorAdd(triProjected.p[1],vOffsetView);
                        triProjected.p[2] = vectorAdd(triProjected.p[2],vOffsetView);
                        float scale = 0.5f;
                        triProjected.p[0].x *= scale * (float)frame.getWidth();
                        triProjected.p[0].y *= scale * (float)frame.getHeight();
                        triProjected.p[1].x *= scale * (float)frame.getWidth();
                        triProjected.p[1].y *= scale * (float)frame.getHeight();
                        triProjected.p[2].x *= scale * (float)frame.getWidth();
                        triProjected.p[2].y *= scale * (float)frame.getHeight();

                        //rasterize triangles

                        trianglesToRaster.add(triProjected);
                    }
                }

                //sort so triangles closest to screen get drawn last

                Collections.sort(trianglesToRaster, new sortByZ());
                Collections.reverse(trianglesToRaster);

                //draw

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

                        //draw object vertex

                        g.setColor(new Color(255,255,0));
                        g2.fill(path);

                        //draw shader vertex

                        g.setColor(new Color(0,0,0,255-(int)current.dp));
                        g2.fill(path);
                }

            }
        };

        //more swing management

        frame.add(renderPanel,BorderLayout.CENTER);
        frame.repaint();

        frame.pack();
        frame.setSize(1000, 1000);

        //updates the scene and elapses time

        while(true){
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            TimeUnit.MILLISECONDS.sleep(50);
            fTheta += 0.1f;
            renderPanel.repaint();
        }

    }

    static private class toggleXRot extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(rotateX){
                rotateX = false;
            }else{
                rotateX = true;
            }
        }
    }
    static private class toggleZRot extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(rotateZ){
                rotateZ = false;
            }else{
                rotateZ = true;
            }
        }
    }
    static private class toggleYRot extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(rotateY){
                rotateY = false;
            }else{
                rotateY = true;
            }
        }
    }
    static private class moveUp extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            main.vCamera.y -= 0.1f;
        }
    }

    static private class moveDown extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            main.vCamera.y += 0.1f;
        }
    }

    static private class moveRight extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            main.vCamera.x += 0.1f;
        }
    }

    static private class moveLeft extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            main.vCamera.x -= 0.1f;
        }
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

    static mat4x4 matrixPointAt(vector pos, vector target, vector up){

        // calculate new forward direction

        vector newForward = vectorSub(target,pos);
        newForward = vectorNormalize(newForward);

        // calculate new up direction

        vector a = vectorMul(newForward, vectorDot(up,newForward));
        vector newUp = vectorSub(up, a);
        newUp = vectorNormalize(newUp);

        //calculate new right direction
        vector newRight = vectorCrossproduct(newUp,newForward);

        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = newRight.x;	matrix.m[0][1] = newRight.y;	matrix.m[0][2] = newRight.z;	matrix.m[0][3] = 0.0f;
        matrix.m[1][0] = newUp.x;		matrix.m[1][1] = newUp.y;		matrix.m[1][2] = newUp.z;		matrix.m[1][3] = 0.0f;
        matrix.m[2][0] = newForward.x;	matrix.m[2][1] = newForward.y;	matrix.m[2][2] = newForward.z;	matrix.m[2][3] = 0.0f;
        matrix.m[3][0] = pos.x;			matrix.m[3][1] = pos.y;			matrix.m[3][2] = pos.z;			matrix.m[3][3] = 1.0f;
        return matrix;
    }

    static mat4x4 matrixQuickInverse(mat4x4 m) // Only for Rotation/Translation Matrices
    {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = m.m[0][0]; matrix.m[0][1] = m.m[1][0]; matrix.m[0][2] = m.m[2][0]; matrix.m[0][3] = 0.0f;
        matrix.m[1][0] = m.m[0][1]; matrix.m[1][1] = m.m[1][1]; matrix.m[1][2] = m.m[2][1]; matrix.m[1][3] = 0.0f;
        matrix.m[2][0] = m.m[0][2]; matrix.m[2][1] = m.m[1][2]; matrix.m[2][2] = m.m[2][2]; matrix.m[2][3] = 0.0f;
        matrix.m[3][0] = -(m.m[3][0] * matrix.m[0][0] + m.m[3][1] * matrix.m[1][0] + m.m[3][2] * matrix.m[2][0]);
        matrix.m[3][1] = -(m.m[3][0] * matrix.m[0][1] + m.m[3][1] * matrix.m[1][1] + m.m[3][2] * matrix.m[2][1]);
        matrix.m[3][2] = -(m.m[3][0] * matrix.m[0][2] + m.m[3][1] * matrix.m[1][2] + m.m[3][2] * matrix.m[2][2]);
        matrix.m[3][3] = 1.0f;
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
