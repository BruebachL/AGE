import java.util.Comparator;

public class triangle{
    vector[] p = new vector[3];
    float dp;
    float zDepth;

    public triangle(vector a, vector b, vector c){
        p[0]=a;
        p[1]=b;
        p[2]=c;
    }
public void setzDepth(){
    zDepth = (this.p[0].z + this.p[1].z + this.p[2].z)/3.0f;
}

}
class sortByZ implements Comparator<triangle>{
    public int compare(triangle a, triangle b){
        Float z1 = a.zDepth;
        Float z2 = b.zDepth;
        return z1.compareTo(z2);
    }
}