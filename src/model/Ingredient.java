package model;

public class Ingredient {
	private final String name;
	private final String unit;
	
	public Ingredient(String name, String unit) {
		if (name == null || name.isBlank() || name.isEmpty() || unit == null || unit.isBlank() || unit.isEmpty()) {
			throw new IllegalArgumentException("Both the name and the unit are required.");
		}
		this.name = name;
		this.unit = unit;
	}
	
	public String getName() {
		return name;
	}
	
	public String getUnit() {
		return unit;
	}
}
