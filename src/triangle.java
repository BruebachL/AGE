import java.util.Comparator;

public class triangle{
    vector[] p = new vector[3];
    float dp;

    public triangle(vector a, vector b, vector c){
        p[0]=a;
        p[1]=b;
        p[2]=c;
    }
    public void printZAverage(){
        System.out.println((this.p[0].z + this.p[1].z + this.p[2].z)/3.0f);
    }

}
class sortByZ implements Comparator<triangle>{
    public int compare(triangle a, triangle b){
        Float z1 = (a.p[0].z + a.p[1].z + a.p[2].z)/3.0f;
        Float z2 = (b.p[0].z + b.p[1].z + b.p[2].z)/3.0f;
        return z1.compareTo(z2);
    }
}