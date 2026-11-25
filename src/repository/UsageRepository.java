package repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import model.UsageRecord;

public class UsageRepository {
	private final Path csvPath;
	
	public UsageRepository(Path csvPath) {
		this.csvPath = csvPath;
	}
	
	public List<UsageRecord> findAll() throws IOException {
		List<UsageRecord> records = new ArrayList<>();
		
		try(BufferedReader reader = Files.newBufferedReader(csvPath)) {
			String header = reader.readLine();
			if(header == null) {
				throw new IOException("CSV file is empty.");
			}
			if (!header.trim().equalsIgnoreCase("date,ingredient,quantity")) {
				throw new IOException("CSV header must be: date,ingredient,quantity");
			}
			
			String line;
			int lineNumber = 1;
			
			while((line = reader.readLine()) != null) {
				lineNumber++;
				if (line.isBlank()) continue;
				
				String[] columns = line.split(",", -1);
				if (columns.length != 3) {
					throw new IOException("Invalid CSV row at line " + lineNumber);
				}
				
				try {
					records.add(new UsageRecord(
							LocalDate.parse(columns[0].trim()),
							columns[1].trim(),
							Double.parseDouble(columns[2].trim())
					));
				} catch (RuntimeException e) {
					throw new IOException("Invalid data at CSV line " + lineNumber, e);
				}
			}
		}
		return records;
	}

	public void append(UsageRecord record) throws IOException {
		if (record.getIngredient().contains(",")
				|| record.getIngredient().contains("\n")
				|| record.getIngredient().contains("\r")) {
			throw new IllegalArgumentException(
					"Ingredient cannot contain commas or line breaks.");
		}

		Path parent = csvPath.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		boolean needsHeader = !Files.exists(csvPath) || Files.size(csvPath) == 0;
		try (var writer = Files.newBufferedWriter(
				csvPath,
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND)) {
			if (needsHeader) {
				writer.write("date,ingredient,quantity");
				writer.newLine();
			}
			writer.write(record.getDate() + "," + record.getIngredient()
					+ "," + record.getQuantity());
			writer.newLine();
		}
	}

	public boolean updateFirst(UsageRecord original, UsageRecord updated) throws IOException {
		validateIngredient(updated);
		List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
		boolean replaced = false;
		for (int i = 1; i < lines.size(); i++) {
			String[] columns = lines.get(i).split(",", -1);
			if (columns.length != 3) continue;
			try {
				UsageRecord current = new UsageRecord(
						LocalDate.parse(columns[0].trim()), columns[1].trim(),
						Double.parseDouble(columns[2].trim()));
				if (!replaced && sameRecord(current, original)) {
					lines.set(i, toCsv(updated));
					replaced = true;
				}
			} catch (RuntimeException e) {
				throw new IOException("Invalid data at CSV line " + (i + 1), e);
			}
		}
		if (!replaced) return false;

		Path temporaryFile = Files.createTempFile(csvPath.toAbsolutePath().getParent(),
				csvPath.getFileName().toString(), ".tmp");
		try {
			Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
			try {
				Files.move(temporaryFile, csvPath, StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				Files.move(temporaryFile, csvPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporaryFile);
		}
		return true;
	}

	private boolean sameRecord(UsageRecord first, UsageRecord second) {
		return first.getDate().equals(second.getDate())
				&& first.getIngredient().equals(second.getIngredient())
				&& Double.compare(first.getQuantity(), second.getQuantity()) == 0;
	}

	private String toCsv(UsageRecord record) {
		return record.getDate() + "," + record.getIngredient() + "," + record.getQuantity();
	}

	private void validateIngredient(UsageRecord record) {
		if (record.getIngredient().contains(",") || record.getIngredient().contains("\n")
				|| record.getIngredient().contains("\r")) {
			throw new IllegalArgumentException(
					"Ingredient cannot contain commas or line breaks.");
		}
	}
}
