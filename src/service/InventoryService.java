package service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import algorithms.Quickselect;
import algorithms.SegmentTree;
import model.UsageRecord;
import repository.UsageRepository;

public class InventoryService {
	public final UsageRepository repository;
	
	public InventoryService(UsageRepository repository) {
		this.repository = repository;
	}
	
	public List<UsageRecord> getUsage() throws IOException {
		return repository.findAll().stream().sorted(Comparator.comparing(UsageRecord::getDate)).toList();
	}
	
	public List<String> getIngredients() throws IOException {
		return repository.findAll().stream().map(UsageRecord::getIngredient).distinct().sorted().toList();
	}

	public UsageRecord addUsage(UsageRecord record) throws IOException {
		if (record == null) {
			throw new IllegalArgumentException("Usage record is required.");
		}
		repository.append(record);
		return record;
	}

	public UsageRecord updateUsage(UsageRecord original, UsageRecord updated) throws IOException {
		if (original == null || updated == null) {
			throw new IllegalArgumentException("Original and updated records are required.");
		}
		if (!repository.updateFirst(original, updated)) {
			throw new IllegalArgumentException("The record was not found. Refresh and try again.");
		}
		return updated;
	}
	
	public Map<String, Object> analyze(String ingredient, LocalDate from, LocalDate to) throws IOException {
		if (ingredient == null || ingredient.isBlank()) {
			throw new IllegalArgumentException("Ingredient is required.");
		}
		if (from == null || to == null || from.isAfter(to)) {
			throw new IllegalArgumentException("Date range is invalid.");
		}
		
		List<UsageRecord> rows = repository.findAll().stream()
				.filter(row -> row.getIngredient().equalsIgnoreCase(ingredient))
				.sorted(Comparator.comparing(UsageRecord::getDate))
				.toList();
		
		if (rows.isEmpty()) {
			throw new IllegalArgumentException("No records for that ingredient.");
		}
		
		double[] quantities = rows.stream().mapToDouble(UsageRecord::getQuantity).toArray();
		
		SegmentTree segmentTree = new SegmentTree(quantities);
		
		int start = -1;
		int end = -1;
		
		for(int i = 0; i < rows.size(); i++) {
			LocalDate date = rows.get(i).getDate();
			
			if (!date.isBefore(from) && !date.isAfter(to)) {
				if (start == -1) start = i;
				end = i;
			}
		}
		
		double rangeTotal = start == -1 ? 0 : segmentTree.rangeSum(start, end);
		
		int percentileIndex = (int) Math.ceil(0.90 * quantities.length) - 1;
		percentileIndex = Math.max(0, percentileIndex);
		
		double percentile90 = Quickselect.selectKth(quantities, percentileIndex);
		
		return Map.of(
				"ingredient", ingredient,
				"from", from.toString(),
				"to", to.toString(),
				"rangeTotal", rangeTotal,
				"percentile90", percentile90,
				"records", rows
		);
	}
}
