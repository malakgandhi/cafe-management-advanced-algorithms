package algorithms;

public class SegmentTree {
	private final int size;
	private final double[] tree;
	
	public SegmentTree(double[] values) {
		if (values == null || values.length == 0) {
			throw new IllegalArgumentException("Values cannot be empty.");
		}
		
		size = values.length;
		tree = new double[size * 4];
		build(values, 1, 0, size - 1);
	}
	
	private void build(double[] values, int node, int left, int right) {
		if (left == right) {
			tree[node] = values[left];
			return;
		}
		
		int mid = (left + right) / 2;
		build(values, node * 2, left, mid);
		build(values, node * 2 + 1, mid + 1, right);
		tree[node] = tree[node * 2] + tree[2 * node + 1];
	}
	
	public double rangeSum(int queryLeft, int queryRight) {
		if(queryLeft < 0 || queryRight >= size || queryLeft > queryRight) {
			throw new IllegalArgumentException("Invalid query range.");
		}
		
		return rangeSum(1, 0, size - 1, queryLeft, queryRight);
	}
	
	private double rangeSum(int node, int left, int right, int queryLeft, int queryRight) {
		if (queryLeft <= left && right <= queryRight) {
			return tree[node];
		}
		if (queryLeft > right || queryRight < left) {
			return 0;
		}
		
		int mid = left + (right - left) / 2;
		return rangeSum(2 * node, left, mid, queryLeft, queryRight) 
				+ rangeSum(2 * node + 1, mid + 1, right, queryLeft, queryRight);
	}
	
	public void update(int index, double newValue) {
		if (index < 0 || index >= size || !Double.isFinite(newValue)) {
			throw new IllegalArgumentException("Invalid index or value.");
		}
		
		update(1, 0, size - 1, index, newValue);
	}
	
	private void update(int node, int left, int right, int index, double newValue) {
		if (left == right) {
			tree[node] = newValue;
			return;
		}
		int mid = left + (right - left) / 2;
		if (index <= mid) {
			update(2 * node, left, mid, index, newValue);
		} else {
			update(2 * node + 1, mid + 1, right, index, newValue);
		}
		
		tree[node] = tree[2 * node] + tree[2 * node + 1];
	}
}
