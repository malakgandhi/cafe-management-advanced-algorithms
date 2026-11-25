package tests;

import algorithms.SegmentTree;

public class SegmentTreeTest {
	public static void main(String[] args) {
        SegmentTree tree = new SegmentTree(new double[]{2, 4, 6, 8});

        assertEquals(20, tree.rangeSum(0, 3), "whole range");
        assertEquals(10, tree.rangeSum(1, 2), "subrange");

        tree.update(1, 10);
		assertEquals(26, tree.rangeSum(0, 3), "updated range");

        expectError(() -> tree.rangeSum(3, 1), "invalid range");
        expectError(() -> new SegmentTree(new double[]{}), "empty input");

        System.out.println("SegmentTree tests passed.");
    }

    private static void assertEquals(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 0.000001) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectError(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + ": expected an exception");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }	
}
