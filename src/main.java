import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class main extends JComponent {
    private static JFrame frame = new JFrame();
    private static mesh meshCube;
    private static float fTheta;
    private static vector vCamera = new vector(0, 0, 0);
    private static JLabel listener = new JLabel();
    private static boolean rotateY = false;
    private static boolean rotateX = false;
    private static boolean rotateZ = false;
    private static float fYaw = 0.0f;
    private static vector lookDir = new vector(0, 0, 1);

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
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK), "up");
        listener.getActionMap().put("up", new moveUp());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, InputEvent.CTRL_DOWN_MASK), "down");
        listener.getActionMap().put("down", new moveDown());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("W"), "forward");
        listener.getActionMap().put("forward", new moveForward());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("S"), "backward");
        listener.getActionMap().put("backward", new moveBackward());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("A"), "left");
        listener.getActionMap().put("left", new moveLeft());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("D"), "right");
        listener.getActionMap().put("right", new moveRight());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("E"), "yawRight");
        listener.getActionMap().put("yawRight", new yawRight());
        listener.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("Q"), "yawLeft");
        listener.getActionMap().put("yawLeft", new yawLeft());
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

                mat4x4 matProj = matrixMakeProjection(90, (float) (frame.getHeight() / frame.getWidth()), 0.1f, 1000.0f, 1.0f / (float) Math.tan(90.0f * 0.5f / 180.0f * 3.14159f));

                mat4x4 matRotZ = matrixMakeRotationZ(fTheta);
                mat4x4 matRotY = matrixMakeRotationY(fTheta);
                mat4x4 matRotX = matrixMakeRotationX(fTheta);

                mat4x4 matTrans = matrixMakeTranslation(0.0f, 0.0f, 5.0f);
                mat4x4 matWorld = matrixMakeIdentity();
                //matWorld = matrixMultiplyMatrix(matRotZ,matRotY);
                if (rotateZ) {
                    matWorld = matrixMultiplyMatrix(matWorld, matRotZ);
                }
                if (rotateY) {
                    matWorld = matrixMultiplyMatrix(matWorld, matRotY);
                }
                if (rotateX) {
                    matWorld = matrixMultiplyMatrix(matWorld, matRotX);
                }
                matWorld = matrixMultiplyMatrix(matWorld, matTrans);


                vector vUp = new vector(0, 1, 0);
                vector vTarget = new vector(0, 0, 1);
                mat4x4 matCameraRot = matrixMakeRotationY(fYaw);
                lookDir = matrixMultiplyVector(vTarget, matCameraRot);
                vTarget = vectorAdd(vCamera, lookDir);

                mat4x4 matCamera = matrixPointAt(vCamera, vTarget, vUp);

                //make view matrix from camera
                mat4x4 matView = matrixQuickInverse(matCamera);

                List<triangle> trianglesToRaster = new ArrayList<>();
                for (triangle t : meshCube.tris) {

                    triangle triTransformed = new triangle(t.p[0], t.p[1], t.p[2]);

                    //offset to fit viewport

                    triTransformed.p[0] = matrixMultiplyVector(t.p[0], matWorld);
                    triTransformed.p[1] = matrixMultiplyVector(t.p[1], matWorld);
                    triTransformed.p[2] = matrixMultiplyVector(t.p[2], matWorld);

                    vector line1 = vectorSub(triTransformed.p[1], triTransformed.p[0]);
                    vector line2 = vectorSub(triTransformed.p[2], triTransformed.p[0]);
                    vector normal = vectorCrossproduct(line1, line2);
                    normal = vectorNormalize(normal);

                    vector vCameraRay = vectorSub(triTransformed.p[0], vCamera);

                    triangle triViewed = new triangle(t.p[0], t.p[1], t.p[2]);
                    triViewed.p[0] = matrixMultiplyVector(triTransformed.p[0], matView);
                    triViewed.p[1] = matrixMultiplyVector(triTransformed.p[1], matView);
                    triViewed.p[2] = matrixMultiplyVector(triTransformed.p[2], matView);

                    if (vectorDot(normal, vCameraRay) < 0.0f) {

                        //Illumination

                        vector lightDirection = new vector(vCamera.x, vCamera.y, -1);
                        lightDirection = vectorNormalize(lightDirection);

                        //Convert world space to view space


                        //Projection
                        triangle triProjected = new triangle(triViewed.p[0], triViewed.p[1], triViewed.p[2]);

                        //get similarity of lightvector and vertexnormal

                        triProjected.dp = Math.max(0.1f, vectorDot(lightDirection, normal));

                        //more swing management

                        // Clip Viewed Triangle against near plane, this could form two additional
                        // additional triangles.
                        int nClippedTriangles = 0;
                        triangle[] clipped = new triangle[2];
                        triangle[] triParams = new triangle[1];
                        triParams[0] = triViewed;
                        clipped = triangleClipAgainstPlane(new vector(0.0f, 0.0f, 0.1f), new vector(0.0f, 0.0f, 1.0f), triParams);

                        // We may end up with multiple triangles form the clip, so project as
                        // required
                        for (int n = 0; n < clipped.length; n++) {
                            // Project triangles from 3D --> 2D
                            triProjected.p[0] = matrixMultiplyVector(clipped[n].p[0], matProj);
                            triProjected.p[1] = matrixMultiplyVector(clipped[n].p[1], matProj);
                            triProjected.p[2] = matrixMultiplyVector(clipped[n].p[2], matProj);
                            triProjected.dp = clipped[n].dp;

                            // Scale into view, we moved the normalising into cartesian space
                            // out of the matrix.vector function from the previous videos, so
                            // do this manually
                            triProjected.p[0] = vectorDiv(triProjected.p[0], triProjected.p[0].w);
                            triProjected.p[1] = vectorDiv(triProjected.p[1], triProjected.p[1].w);
                            triProjected.p[2] = vectorDiv(triProjected.p[2], triProjected.p[2].w);

                            // X/Y are inverted so put them back
                            triProjected.p[0].x *= -1.0f;
                            triProjected.p[1].x *= -1.0f;
                            triProjected.p[2].x *= -1.0f;
                            triProjected.p[0].y *= -1.0f;
                            triProjected.p[1].y *= -1.0f;
                            triProjected.p[2].y *= -1.0f;

                            // Offset verts into visible normalised space
                            vector vOffsetView = new vector(1, 1, 0);
                            triProjected.p[0] = vectorAdd(triProjected.p[0], vOffsetView);
                            triProjected.p[1] = vectorAdd(triProjected.p[1], vOffsetView);
                            triProjected.p[2] = vectorAdd(triProjected.p[2], vOffsetView);
                            float scale = 0.5f;
                            triProjected.p[0].x *= scale * (float) frame.getWidth();
                            triProjected.p[0].y *= scale * (float) frame.getHeight();
                            triProjected.p[1].x *= scale * (float) frame.getWidth();
                            triProjected.p[1].y *= scale * (float) frame.getHeight();
                            triProjected.p[2].x *= scale * (float) frame.getWidth();
                            triProjected.p[2].y *= scale * (float) frame.getHeight();


                            // Store triangle for sorting
                            trianglesToRaster.add(triProjected);
                        }
                    }

                }

                //sort so triangles closest to screen get drawn last

                Collections.sort(trianglesToRaster, new sortByZ());
                Collections.reverse(trianglesToRaster);


                // Loop through all transformed, viewed, projected, and sorted triangles
                /*for (triangle triToRaster : trianglesToRaster) {
                    // Clip triangles against all four screen edges, this could yield
                    // a bunch of triangles, so create a queue that we traverse to
                    //  ensure we only test new triangles generated against planes
                    triangle clipped[] = new triangle[2];
                    ArrayDeque<triangle> listTriangles = new ArrayDeque<>();

                    // Add initial triangle
                    listTriangles.add(triToRaster);
                    int nNewTriangles = 1;

                    for (int p = 0; p < 4; p++) {
                        int nTrisToAdd = 0;
                        while (nNewTriangles > 0) {
                            // Take triangle from front of queue
                            triangle test = listTriangles.getFirst();
                            nNewTriangles--;

                            // Clip it against a plane. We only need to test each
                            // subsequent plane, against subsequent new triangles
                            // as all triangles after a plane clip are guaranteed
                            // to lie on the inside of the plane. I like how this
                            // comment is almost completely and utterly justified
                            triangle[] parameters = new triangle[3];
                            parameters[0] = test;
                            switch (p) {
                                case 0:
                                    nTrisToAdd = triangleClipAgainstPlane(new vector(0.0f, 0.0f, 0.0f), new vector(0.0f, 1.0f, 0.0f), parameters);
                                    break;
                                case 1:
                                    nTrisToAdd = triangleClipAgainstPlane(new vector(0.0f, (float) frame.getHeight() - 1, 0.0f), new vector(0.0f, -1.0f, 0.0f), parameters);
                                    break;
                                case 2:
                                    nTrisToAdd = triangleClipAgainstPlane(new vector(0.0f, 0.0f, 0.0f), new vector(1.0f, 0.0f, 0.0f), parameters);
                                    break;
                                case 3:
                                    nTrisToAdd = triangleClipAgainstPlane(new vector((float) frame.getWidth() - 1, 0.0f, 0.0f), new vector(-1.0f, 0.0f, 0.0f), parameters);
                                    break;
                            }

                            // Clipping may yield a variable number of triangles, so
                            // add these new ones to the back of the queue for subsequent
                            // clipping against next planes
                            for (int w = 0; w < nTrisToAdd; w++)
                                listTriangles.addLast(clipped[w]);
                        }
                        nNewTriangles = listTriangles.size();
                    }*/


                    // Draw the transformed, viewed, clipped, projected, sorted, clipped triangles
                    for (triangle current : trianglesToRaster) {
                        Path2D path = new Path2D.Double();
                        path.moveTo(current.p[0].x, current.p[0].y);
                        path.lineTo(current.p[1].x, current.p[1].y);
                        path.lineTo(current.p[2].x, current.p[2].y);
                        path.closePath();
                        current.dp *= 255.0f;
                        if (current.dp < 0 || current.dp > 255) {
                            current.dp = 255;
                        }

                        //draw object vertex

                        g.setColor(new Color(255, 255, 0));
                        g2.fill(path);

                        //draw shader vertex

                        /*g.setColor(new Color(0, 0, 0, 255 - (int) current.dp));
                        g2.fill(path);*/
                    }
                }

        };
        frame.add(renderPanel, BorderLayout.CENTER);
        frame.repaint();

        frame.pack();
        frame.setSize(1000, 1000);

        //updates the scene and elapses time

        while (true) {
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
            if (rotateX) {
                rotateX = false;
            } else {
                rotateX = true;
            }
        }
    }

    static private class toggleZRot extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (rotateZ) {
                rotateZ = false;
            } else {
                rotateZ = true;
            }
        }
    }

    static private class toggleYRot extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (rotateY) {
                rotateY = false;
            } else {
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

    static private class yawLeft extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            main.fYaw += 0.1f;
        }
    }

    static private class yawRight extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            main.fYaw -= 0.1f;
        }
    }

    static private class moveForward extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            vector vForward = vectorMul(lookDir, 1.2f);
            vCamera = vectorAdd(vCamera, vForward);
        }
    }

    static private class moveBackward extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            vector vForward = vectorMul(lookDir, 1.2f);
            vCamera = vectorSub(vCamera, vForward);
        }
    }


    private static vector matrixMultiplyVector(vector in, mat4x4 m) {
        vector out = new vector(0, 0, 0);
        out.x = in.x * m.m[0][0] + in.y * m.m[1][0] + in.z * m.m[2][0] + in.w * m.m[3][0];
        out.y = in.x * m.m[0][1] + in.y * m.m[1][1] + in.z * m.m[2][1] + in.w * m.m[3][1];
        out.z = in.x * m.m[0][2] + in.y * m.m[1][2] + in.z * m.m[2][2] + in.w * m.m[3][2];
        out.w = in.x * m.m[0][3] + in.y * m.m[1][3] + in.z * m.m[2][3] + in.w * m.m[3][3];
        return out;
    }

    private static mat4x4 matrixMakeIdentity() {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = 1.0f;
        matrix.m[1][1] = 1.0f;
        matrix.m[2][2] = 1.0f;
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    private static mat4x4 matrixMakeRotationX(float fAngleRad) {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = 1.0f;
        matrix.m[1][1] = (float) Math.cos((fAngleRad));
        matrix.m[1][2] = (float) Math.sin((fAngleRad));
        matrix.m[2][1] = (float) -Math.sin((fAngleRad));
        matrix.m[2][2] = (float) Math.cos((fAngleRad));
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    private static mat4x4 matrixMakeRotationY(float fAngleRad) {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = (float) Math.cos(fAngleRad);
        matrix.m[0][2] = (float) Math.sin(fAngleRad);
        matrix.m[2][0] = (float) -Math.sin(fAngleRad);
        matrix.m[1][1] = 1.0f;
        matrix.m[2][2] = (float) Math.cos(fAngleRad);
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    private static mat4x4 matrixMakeRotationZ(float fAngleRad) {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = (float) Math.cos(fAngleRad);
        matrix.m[0][1] = (float) Math.sin(fAngleRad);
        matrix.m[1][0] = (float) -Math.sin(fAngleRad);
        matrix.m[1][1] = (float) Math.cos(fAngleRad);
        matrix.m[2][2] = 1.0f;
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    private static mat4x4 matrixMakeTranslation(float x, float y, float z) {
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

    private static mat4x4 matrixMakeProjection(float fFovDegrees, float fAspectRatio, float fNear, float fFar, float fFovRad) {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = fAspectRatio * fFovRad;
        matrix.m[1][1] = fFovRad;
        matrix.m[2][2] = fFar / (fFar - fNear);
        matrix.m[3][2] = (-fFar * fNear) / (fFar - fNear);
        matrix.m[2][3] = 1.0f;
        matrix.m[3][3] = 0.0f;
        return matrix;
    }

    private static mat4x4 matrixMultiplyMatrix(mat4x4 a, mat4x4 b) {
        mat4x4 matrix = new mat4x4();
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                matrix.m[r][c] = a.m[r][0] * b.m[0][c] + a.m[r][1] * b.m[1][c] + a.m[r][2] * b.m[2][c] + a.m[r][3] * b.m[3][c];
            }
        }
        return matrix;
    }

    private static mat4x4 matrixPointAt(vector pos, vector target, vector up) {

        // calculate new forward direction

        vector newForward = vectorSub(target, pos);
        newForward = vectorNormalize(newForward);

        // calculate new up direction

        vector a = vectorMul(newForward, vectorDot(up, newForward));
        vector newUp = vectorSub(up, a);
        newUp = vectorNormalize(newUp);

        //calculate new right direction
        vector newRight = vectorCrossproduct(newUp, newForward);

        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = newRight.x;
        matrix.m[0][1] = newRight.y;
        matrix.m[0][2] = newRight.z;
        matrix.m[0][3] = 0.0f;
        matrix.m[1][0] = newUp.x;
        matrix.m[1][1] = newUp.y;
        matrix.m[1][2] = newUp.z;
        matrix.m[1][3] = 0.0f;
        matrix.m[2][0] = newForward.x;
        matrix.m[2][1] = newForward.y;
        matrix.m[2][2] = newForward.z;
        matrix.m[2][3] = 0.0f;
        matrix.m[3][0] = pos.x;
        matrix.m[3][1] = pos.y;
        matrix.m[3][2] = pos.z;
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    private static mat4x4 matrixQuickInverse(mat4x4 m) // Only for Rotation/Translation Matrices
    {
        mat4x4 matrix = new mat4x4();
        matrix.m[0][0] = m.m[0][0];
        matrix.m[0][1] = m.m[1][0];
        matrix.m[0][2] = m.m[2][0];
        matrix.m[0][3] = 0.0f;
        matrix.m[1][0] = m.m[0][1];
        matrix.m[1][1] = m.m[1][1];
        matrix.m[1][2] = m.m[2][1];
        matrix.m[1][3] = 0.0f;
        matrix.m[2][0] = m.m[0][2];
        matrix.m[2][1] = m.m[1][2];
        matrix.m[2][2] = m.m[2][2];
        matrix.m[2][3] = 0.0f;
        matrix.m[3][0] = -(m.m[3][0] * matrix.m[0][0] + m.m[3][1] * matrix.m[1][0] + m.m[3][2] * matrix.m[2][0]);
        matrix.m[3][1] = -(m.m[3][0] * matrix.m[0][1] + m.m[3][1] * matrix.m[1][1] + m.m[3][2] * matrix.m[2][1]);
        matrix.m[3][2] = -(m.m[3][0] * matrix.m[0][2] + m.m[3][1] * matrix.m[1][2] + m.m[3][2] * matrix.m[2][2]);
        matrix.m[3][3] = 1.0f;
        return matrix;
    }

    private static vector vectorAdd(vector a, vector b) {
        return new vector(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    private static vector vectorSub(vector a, vector b) {
        return new vector(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    private static vector vectorMul(vector a, float k) {
        return new vector(a.x * k, a.y * k, a.z * k);
    }

    private static vector vectorDiv(vector a, float k) {
        return new vector(a.x / k, a.y / k, a.z / k);
    }

    private static float vectorDot(vector a, vector b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    private static float vectorLength(vector a) {
        return (float) Math.sqrt((double) (vectorDot(a, a)));
    }

    private static vector vectorNormalize(vector a) {
        float l = vectorLength(a);
        return new vector(a.x / l, a.y / l, a.z / l);
    }

    private static vector vectorCrossproduct(vector a, vector b) {
        vector v = new vector(0, 0, 0);
        v.x = a.y * b.z - a.z * b.y;
        v.y = a.z * b.x - a.x * b.z;
        v.z = a.x * b.y - a.y * b.x;
        return v;
    }

    private static float dist(vector p, vector plane_p, vector plane_n){
        vector n = vectorNormalize(p);
        return (plane_n.x * p.x + plane_n.y * p.y + plane_n.z * p.z - vectorDot(plane_n, plane_p));
    }

    private static vector vectorIntersectPlane(vector plane_p, vector plane_n, vector lineStart, vector lineEnd) {
        plane_n = vectorNormalize(plane_n);
        float plane_d = -vectorDot(plane_n, plane_p);
        float ad = vectorDot(lineStart, plane_n);
        float bd = vectorDot(lineEnd, plane_n);
        float t = (-plane_d - ad) / (bd - ad);
        vector lineStartToEnd = vectorSub(lineEnd, lineStart);
        vector lineToIntersect = vectorSub(lineStartToEnd, new vector(t, t, t));
        return vectorAdd(lineStart, lineToIntersect);
    }

    private static triangle[] triangleClipAgainstPlane(vector plane_p, vector plane_n, triangle[] triParams) {
        // Make sure plane normal is indeed normal
        triangle in_tri = triParams[0];
        plane_n = vectorNormalize(plane_n);

        // Create two temporary storage arrays to classify points either side of plane
        // If distance sign is positive, point lies on "inside" of plane
        vector inside_points[] = new vector[3];
        int nInsidePointCount = 0;
        vector outside_points[] = new vector[3];
        int nOutsidePointCount = 0;

        // Get signed distance of each point in triangle to plane
        float d0 = dist(in_tri.p[0], plane_p, plane_n);
        float d1 = dist(in_tri.p[1], plane_p, plane_n);
        float d2 = dist(in_tri.p[2], plane_p, plane_n);

        if (d0 >= 0) {
            inside_points[nInsidePointCount++] = in_tri.p[0];
        } else {
            outside_points[nOutsidePointCount++] = in_tri.p[0];
        }
        if (d1 >= 0) {
            inside_points[nInsidePointCount++] = in_tri.p[1];
        } else {
            outside_points[nOutsidePointCount++] = in_tri.p[1];
        }
        if (d2 >= 0) {
            inside_points[nInsidePointCount++] = in_tri.p[2];
        } else {
            outside_points[nOutsidePointCount++] = in_tri.p[2];
        }

        // Now classify triangle points, and break the input triangle into
        // smaller output triangles if required. There are four possible
        // outcomes...

        if (nInsidePointCount == 0) {
            // All points lie on the outside of plane, so clip whole triangle
            // It ceases to exist
            triParams = new triangle[0];
            // No returned triangles are valid
        }

        if (nInsidePointCount == 3) {
            triParams = new triangle[1];
            // All points lie on the inside of plane, so do nothing
            // and allow the triangle to simply pass through
            triParams[0] = in_tri;
            triParams[0].dp = in_tri.dp;
             // Just the one returned original triangle is valid
        }

        if (nInsidePointCount == 1 && nOutsidePointCount == 2) {
            // Triangle should be clipped. As two points lie outside
            // the plane, the triangle simply becomes a smaller triangle
            triParams = new triangle[1];
            triParams[0] = in_tri;
            // Copy appearance info to new triangle
            triParams[0].dp = in_tri.dp;

            // The inside point is valid, so keep that...
            triParams[0].p[0] = inside_points[0];

            // but the two new points are at the locations where the
            // original sides of the triangle (lines) intersect with the plane
            triParams[0].p[1] = vectorIntersectPlane(plane_p, plane_n, inside_points[0], outside_points[0]);
            triParams[0].p[2] = vectorIntersectPlane(plane_p, plane_n, inside_points[0], outside_points[1]);

            // Return the newly formed single triangle
        }

        if (nInsidePointCount == 2 && nOutsidePointCount == 1) {
            // Triangle should be clipped. As two points lie inside the plane,
            // the clipped triangle becomes a "quad". Fortunately, we can
            // represent a quad with two new triangles
            triParams = new triangle[2];
            // Copy appearance info to new triangles
            triParams[0] = in_tri;
            triParams[1] = in_tri;

            // The first triangle consists of the two inside points and a new
            // point determined by the location where one side of the triangle
            // intersects with the plane
            triParams[0].p[0] = inside_points[0];
            triParams[0].p[1] = inside_points[1];
            triParams[0].p[2] = vectorIntersectPlane(plane_p, plane_n, inside_points[0], outside_points[0]);

            // The second triangle is composed of one of he inside points, a
            // new point determined by the intersection of the other side of the
            // triangle and the plane, and the newly created point above
            triParams[1].p[0] = inside_points[1];
            triParams[1].p[1] = triParams[0].p[2];
            triParams[1].p[2] = vectorIntersectPlane(plane_p, plane_n, inside_points[1], outside_points[0]);

            // Return two newly formed triangles which form a quad
        }
        return triParams;
    }
}
