package data.cohesion;

public class StackDCs {
	int top;
	int[] array;
	static int debugCalls = 0;

	public StackDCs(int s) {
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

	public static void PrintHello() {
		debugCalls++;
		debug("Hello");
	}

	private static void debug(String s) {
		debugCalls++;
		System.out.println(s);
	}

}
