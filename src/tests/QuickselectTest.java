package tests;

import algorithms.Quickselect;

public class QuickselectTest {
	public static void main(String[] args) {
        assertEquals(1, Quickselect.selectKth(new double[]{4, 1, 3}, 0), "minimum");
        assertEquals(3, Quickselect.selectKth(new double[]{4, 1, 3}, 1), "middle");
        assertEquals(4, Quickselect.selectKth(new double[]{4, 1, 3}, 2), "maximum");
        assertEquals(2, Quickselect.selectKth(new double[]{2, 2, 5, 1}, 1), "duplicates");

        expectError(() -> Quickselect.selectKth(new double[]{}, 0), "empty input");
        expectError(() -> Quickselect.selectKth(new double[]{1, 2}, 2), "invalid k");

        System.out.println("Quickselect tests passed.");
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
