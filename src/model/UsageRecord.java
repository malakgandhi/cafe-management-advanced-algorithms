package model;

import java.time.LocalDate;

public class UsageRecord {
	private final LocalDate date;
	private final String ingredient;
	private final double quantity;
	
	public UsageRecord(LocalDate date, String ingredient, double quantity) {
		if (date == null || ingredient == null || ingredient.isBlank() || ingredient.isEmpty()) {
			throw new IllegalArgumentException("Labels of date and ingredient are required.");
		}
		if(!Double.isFinite(quantity) || quantity < 0) {
			throw new IllegalArgumentException("Quantity must be finite and non-negative.");
		}
		this.date = date;
		this.ingredient = ingredient;
		this.quantity = quantity;
	}
	
	public LocalDate getDate() {
		return date;
	}
	
	public String getIngredient() {
		return ingredient;
	}
	
	public double getQuantity() {
		return quantity;
	}
}
