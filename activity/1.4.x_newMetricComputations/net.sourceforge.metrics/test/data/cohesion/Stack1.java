package data.cohesion;

/**
 * This class is derived from the one used as an example in BIEMAN, J. M., AND
 * KANG, B.-K. Cohesion and reuse in an object oriented system. SIGSOFT Softw.
 * Eng. Notes 20, SI (1995), 259–262.
 * 
 * @author Keith Cassell
 */
public class Stack1 {
    int top, size;
    int[] array;

    public Stack1(int s) {
	size = s;
	array = new int[size];
	top = 0;
    }

    public boolean Isempty() {
	return top == 0;
    }

    public int Size() {
	return size;
    }

    private int Vtop() {
	return array[top - 1];
    }

    public void Push(int item) {
	if (top == size)
	    print("Empty stack.");
	else
	    array[top++] = item;
    }

    public int Pop() {
	if (Isempty())
	    print("Full stack.");
	else
	    --top;
	return array[top + 1];
    }
    
    public void justPrint1() {
	print("1");
    }
    
    public void justPrint2() {
	print("2");
    }
    
    protected void print(String s) {
	System.out.println(s);
    }
}
