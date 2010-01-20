package data.cohesion;

public class StackDC {
    int top;
    int[] array;

    public StackDC(int s) {
	array = new int[s];
	top = 0;
    }

    public boolean Isempty() {
	return top == 0;
    }

    public int Vtop() {
	return array[top - 1];
    }

    public void Push(int item) {
	debug("Push");
	array[top++] = item;
    }

    public int Pop() {
	if (Isempty())
	    System.out.println("Full stack.");
	else
	    --top;
	return array[top + 1];
    }

    public void PrintHello() {
	debug("Hello");
    }

    private void debug(String s) {
	System.out.println(s);
    }

}
