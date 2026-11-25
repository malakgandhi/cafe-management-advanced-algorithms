package algorithms;

public final class Quickselect {
	private Quickselect() {}
	
	public static double selectKth(double[] values, int k) {
		if (values == null || values.length == 0) {
			throw new IllegalArgumentException("Values out of range.");
		}
		if (k < 0 || k >= values.length) {
			throw new IllegalArgumentException("Index k is out of range");
		}
		
		double[] copy = values.clone();
		int left = 0;
		int right = copy.length - 1;
		
		while(left < right) {
			int pivotIndex = partition(copy, left, right);
			
			if (pivotIndex == k) {
				return copy[pivotIndex];
			} else if (k < pivotIndex) {
				right = pivotIndex - 1;
			} else {
				left = pivotIndex + 1;
			}
		}
		
		return copy[left];
	}
	
	private static int partition(double[] values, int left, int right) {
		double pivot = values[right];
		int boundary = left;
		
		for(int i = left; i < right; i++) {
			if(values[i] <= pivot) {
				swap(values, i, boundary);
				boundary++;
			}
		}
		swap(values, boundary, right);
		return boundary;
	}
	
	private static void swap(double[] values, int first, int second) {
		double temp = values[first];
		values[first] = values[second];
		values[second] = temp;
	}
}
