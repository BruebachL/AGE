import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class mesh {
    List<triangle> tris;
    public mesh(){
        tris = new ArrayList<>();
    }

    public boolean loadFromObjectFile(String fileName) throws IOException {
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);

        List<vector> verts = new ArrayList<>();
        String line;
        String[] subStrings;
        while((line = br.readLine()) != null){
            vector vec3d = new vector(0,0,0);
            subStrings = line.split(" ");
            if(subStrings[0].equals("v")){
                vec3d.x = Float.parseFloat(subStrings[1]);
                vec3d.y = Float.parseFloat(subStrings[2]);
                vec3d.z = Float.parseFloat(subStrings[3]);
                verts.add(vec3d);
            }
            if(subStrings[0].equals("f")){
                this.tris.add(new triangle(verts.get(Integer.parseInt(subStrings[1])-1),verts.get(Integer.parseInt(subStrings[2])-1),verts.get(Integer.parseInt(subStrings[3])-1)));

            }
            //System.out.println(subStrings[0] + " and " + subStrings[1]);
            //System.out.println(line);
        }
        System.out.println("Model loaded");
        br.close();
        return true;
    }
}
