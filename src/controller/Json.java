package controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import model.UsageRecord;

public class Json {
	private Json() {}
	
	public static String stringify(Object value) {
		if (value == null) return "null";
		if (value instanceof String text) return quote(text);
		if (value instanceof Number || value instanceof Boolean) return value.toString();
		
		if (value instanceof UsageRecord record) {
			return "{"
                    + "\"date\":" + quote(record.getDate().toString()) + ","
                    + "\"ingredient\":" + quote(record.getIngredient()) + ","
                    + "\"quantity\":" + record.getQuantity()
                    + "}";
		}
		
		if (value instanceof Map<?,?> map) {
			List<String> entries = map.entrySet().stream().map(entry -> quote(String.valueOf(entry.getKey()))
                            + ":" + stringify(entry.getValue())).toList();
			return "{" + String.join(",", entries) + "}";
		}
		
		if (value instanceof Iterable<?> items) {
			List<String> entries = new ArrayList<>();
			for (Object item : items) {
				entries.add(stringify(item));
			}
			return "[" + String.join(",", entries) + "]";
		}
		
		return quote(value.toString());
	}
	
	private static String quote(String text) {
		return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
	}
}
