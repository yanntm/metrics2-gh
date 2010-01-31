package data.cohesion;

public class MMA
{
    protected int a = 0;
    public int getA() { return a; }
    public void setA(int a) { this.a = a; }
    public int m2modA() { setA(2); return 2*getA(); }
}
