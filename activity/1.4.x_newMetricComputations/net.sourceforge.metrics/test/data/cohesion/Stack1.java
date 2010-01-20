package data.cohesion;

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

    public void Push(int item) {
	if (top == size)
	    print("Full stack.");
	else
	    array[top++] = item;
    }

    public int Pop() {
	if (Isempty())
	    print("Empty stack.");
	else
	    --top;
	return array[top + 1];
    }

    public void printStack() {
	printLoop();
    }

    private void printLoop() {
	for (int i = 0; i < top; i++) {
	    println("" + array[i]);
	}
    }

    private void println(String s) {
	print(s + "\n");
    }

    private void print(String s) {
	System.out.println(s);
    }
}
