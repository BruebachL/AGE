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

        proj.m[0][0]= fAspectRatio *fFovRad;
        proj.m[1][1]= fFovRad;
        proj.m[2][2]= fFar / (fFar-fNear);
        proj.m[3][2]=(-fFar*fNear)/(fFar-fNear);
        proj.m[2][3]=1.0f;
        proj.m[3][3] = 0.0f;

        // SOUTH
        meshCube.tris.add(new triangle(new vector(0.0f,0.0f,0.0f),   new vector(0.0f,1.0f,0.0f),   new vector(1.0f,1.0f,0.0f)));
        meshCube.tris.add(new triangle(new vector(0.0f,0.0f,0.0f),   new vector(1.0f,1.0f,0.0f),   new vector(1.0f,0.0f,0.0f)));

        // EAST
        meshCube.tris.add(new triangle(new vector(1.0f,0.0f,0.0f),   new vector(1.0f,1.0f,0.0f),   new vector(1.0f,1.0f,1.0f)));
        meshCube.tris.add(new triangle(new vector(1.0f,0.0f,0.0f),   new vector(1.0f,1.0f,1.0f),   new vector(1.0f,0.0f,1.0f)));

        // NORTH
        meshCube.tris.add(new triangle(new vector(1.0f,0.0f,1.0f),   new vector(1.0f,1.0f,1.0f),   new vector(0.0f,1.0f,1.0f)));
        meshCube.tris.add(new triangle(new vector(1.0f,0.0f,1.0f),   new vector(0.0f,1.0f,1.0f),   new vector(0.0f,0.0f,1.0f)));

        // WEST
        meshCube.tris.add(new triangle(new vector(0.0f,0.0f,1.0f),   new vector(0.0f,1.0f,1.0f),   new vector(0.0f,1.0f,0.0f)));
        meshCube.tris.add(new triangle(new vector(0.0f,0.0f,1.0f),   new vector(0.0f,1.0f,0.0f),   new vector(0.0f,0.0f,0.0f)));

        // TOP
        meshCube.tris.add(new triangle(new vector(0.0f,1.0f,0.0f),   new vector(0.0f,1.0f,1.0f),   new vector(1.0f,1.0f,1.0f)));
        meshCube.tris.add(new triangle(new vector(0.0f,1.0f,0.0f),   new vector(1.0f,1.0f,1.0f),   new vector(1.0f,1.0f,0.0f)));

        // BOTTOM
        meshCube.tris.add(new triangle(new vector(1.0f,0.0f,1.0f),   new vector(0.0f,0.0f,1.0f),   new vector(0.0f,0.0f,0.0f)));
        meshCube.tris.add(new triangle(new vector(1.0f,0.0f,1.0f),   new vector(0.0f,0.0f,0.0f),   new vector(1.0f,0.0f,0.0f)));

        // meshCube.loadFromObjectFile("C:\\Users\\Ascor\\Documents\\baileybunbun.obj");
        JPanel renderPanel = new JPanel() {
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                List<triangle> trianglesToRaster = new ArrayList<>();
                for (triangle t : meshCube.tris) {

                    triangle triRotatedZ = new triangle(t.p[0],t.p[1],t.p[2]);



                    mat4x4 matRotZ = new mat4x4();
                    mat4x4 matRotX = new mat4x4();

                    matRotZ.m[0][0] = (float)Math.cos(fTheta);
                    matRotZ.m[0][1] = (float)Math.sin(fTheta);
                    matRotZ.m[1][0] = (float)-Math.sin(fTheta);
                    matRotZ.m[1][1] = (float)Math.cos(fTheta);
                    matRotZ.m[2][2] = 1;
                    matRotZ.m[3][3] = 1;

                    matRotX.m[0][0] = 1;
                    matRotX.m[1][1] = (float)Math.cos((fTheta*0.5f));
                    matRotX.m[1][2] = (float)Math.sin((fTheta*0.5f));
                    matRotX.m[2][1] = (float)-Math.sin((fTheta*0.5f));
                    matRotX.m[2][2] = (float)Math.cos((fTheta*0.5f));
                    matRotX.m[3][3] = 1;

                    triRotatedZ.p[0] = multiplyMatrixVector(t.p[0],matRotZ);
                    triRotatedZ.p[1] = multiplyMatrixVector(t.p[1],matRotZ);
                    triRotatedZ.p[2] = multiplyMatrixVector(t.p[2],matRotZ);

                    triangle triRotatedZX = new triangle(triRotatedZ.p[0],triRotatedZ.p[1],triRotatedZ.p[2]);

                    triRotatedZX.p[0] = multiplyMatrixVector(triRotatedZ.p[0],matRotX);
                    triRotatedZX.p[1] = multiplyMatrixVector(triRotatedZ.p[1],matRotX);
                    triRotatedZX.p[2] = multiplyMatrixVector(triRotatedZ.p[2],matRotX);

                    //offset to fit viewport

                    triangle triTranslated = new triangle(triRotatedZX.p[0],triRotatedZX.p[1],triRotatedZX.p[2]);

                    triTranslated.p[0].z = triRotatedZX.p[0].z + 9.5f;
                    triTranslated.p[1].z = triRotatedZX.p[1].z + 9.5f;
                    triTranslated.p[2].z = triRotatedZX.p[2].z + 9.5f;

                    vector normal = new vector(0,0,0);
                    vector line1 = new vector(0,0,0);
                    vector line2 = new vector(0,0,0);

                    line1.x = triTranslated.p[1].x - triTranslated.p[0].x;
                    line1.y = triTranslated.p[1].y - triTranslated.p[0].y;
                    line1.z = triTranslated.p[1].z - triTranslated.p[0].z;

                    line2.x = triTranslated.p[2].x - triTranslated.p[0].x;
                    line2.y = triTranslated.p[2].y - triTranslated.p[0].y;
                    line2.z = triTranslated.p[2].z - triTranslated.p[0].z;

                    normal.x = line1.y * line2.z - line1.z * line2.y;
                    normal.y = line1.z * line2.x - line1.x * line2.z;
                    normal.z = line1.x * line2.y - line1.y * line2.x;

                    float l = (float)Math.sqrt((double)(normal.x*normal.x + normal.y*normal.y + normal.z*normal.z));

                    normal.x /= l;
                    normal.y /= l;
                    normal.z /= l;
                    if(normal.x * (triTranslated.p[0].x - camera.x)
                            + normal.y * (triTranslated.p[0].y - camera.y)
                            + normal.z * (triTranslated.p[0].z - camera.z) < 0) {

                        //Illumination

                        vector lightDirection = new vector(0,0,-1);
                        float lightLength = (float)Math.sqrt((double)(lightDirection.x*lightDirection.x + lightDirection.y*lightDirection.y+lightDirection.z*lightDirection.z));
                        lightDirection.x /= lightLength;
                        lightDirection.y /= lightLength;
                        lightDirection.z /= lightLength;



                        //Projection

                        triangle triProjected = new triangle(triTranslated.p[0], triTranslated.p[1], triTranslated.p[2]);

                        triProjected.dp = normal.x*lightDirection.x + normal.y*lightDirection.y + normal.z*lightDirection.z;

                        triProjected.p[0] = multiplyMatrixVector(triTranslated.p[0], proj);
                        triProjected.p[1] = multiplyMatrixVector(triTranslated.p[1], proj);
                        triProjected.p[2] = multiplyMatrixVector(triTranslated.p[2], proj);

                        // Scale into view
                        triProjected.p[0].x += 1.0f;
                        triProjected.p[0].y += 1.0f;

                        triProjected.p[1].x += 1.0f;
                        triProjected.p[1].y += 1.0f;

                        triProjected.p[2].x += 1.0f;
                        triProjected.p[2].y += 1.0f;

                        triProjected.p[0].x *= 0.5f * 500.0f;
                        triProjected.p[0].y *= 0.5f * 500.0f;
                        triProjected.p[1].x *= 0.5f * 500.0f;
                        triProjected.p[1].y *= 0.5f * 500.0f;
                        triProjected.p[2].x *= 0.5f * 500.0f;
                        triProjected.p[2].y *= 0.5f * 500.0f;

                        //rasterize triangles

                        trianglesToRaster.add(triProjected);

                        // draw triangles

                    }
                }
                Collections.sort(trianglesToRaster, new sortByZ());
                System.out.println("Sorted:");
                for(triangle current : trianglesToRaster){
                    current.printZAverage();
                }
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
        frame.setSize(500, 500);
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
}
