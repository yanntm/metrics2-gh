package data.cohesion;

/**
 * This class is based on an example in
 * CHAE, H. S., KWON, Y. R., AND BAE, D.-H. A cohesion measure for
 * object-oriented classes. Software Practice and Experience 30, 12 (2000),
 * 1405–1431.
 * @author Keith Cassell
 */
public class ChaeB {
	private int v1 = 0;
	private int v2 = 0;
	private int v3 = 0;
	
	public int M1() { return v1 + 1; }
	public int M2() { return v1 + v2; }
	public int M3() { return v2 * v3; }
	public int M4() { return v3 + 1; }
}
