package org.delanee9.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delanee9.model.OutlierData;
import org.delanee9.model.StockExchangeData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockExchangeService {

  private static final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
  private static final Validator validator = validatorFactory.getValidator();

  @Value("${lseg.input.path}")
  private String inputDataFolder;

  @Value("${lseg.output.path}")
  private String outputDataFolder;

  public void processData(int fileLimit) {
    log.info("Processing CSV data in the {} folder", inputDataFolder);
    List<Path> directories = readDirectories();
    List<Path> csvFiles = directories.parallelStream()
        .flatMap(path -> searchFiles(path, fileLimit).stream()).toList();
    List<StockExchangeData> stockExchangeData;
    List<OutlierData> outlierData;

    for (Path path : csvFiles) {
      stockExchangeData = parseFile(path);
      outlierData = identifyOutliers(stockExchangeData);
      saveData(outlierData, path);
    }
    log.info("Processing complete. Check output folder {}", outputDataFolder);
  }

  /**
   * Return a list of folder paths to later check for data.
   *
   * @return list of directories to search
   */
  public List<Path> readDirectories() {
    log.info("Reading subdirectories in: {}", inputDataFolder);
    Path path = Paths.get(inputDataFolder);
    try (Stream<Path> stream = Files.list(path)) {
      return stream.filter(Files::isDirectory).toList();
    } catch (IOException e) {
      log.error("Error reading directory structure - {}", e.getMessage());
      return null;
    }
  }

  /**
   * Search for csv files to process within a folder with a max depth of 1.
   *
   * @param path      directory to be searched
   * @param fileLimit max number of files to be searched in the folder
   * @return list of csv paths
   */
  public List<Path> searchFiles(Path path, int fileLimit) {
    log.info("Searching for csv files in {}", path);
    try (Stream<Path> stream = Files.walk(path, 1)) {
      return stream.filter(Files::isRegularFile)
          .filter(file -> file.toString().endsWith(".csv"))
          .limit(fileLimit).toList();
    } catch (IOException e) {
      log.error("Error reading files in directory: {} - {}", path, e.getMessage());
      return null;
    }
  }

  /**
   * Return 30 consecutive data points starting from a random timestamp within the file.
   *
   * @param path CSV path to be read
   * @return 30 rows of consecutive data
   */
  public List<StockExchangeData> parseFile(Path path) {
    log.info("Parsing 30 rows of data from file {}", path);

    //count total number of lines in the csv and return a random start point
    int numLines = 0;
    int startPoint = 0;
    try {
      numLines = (int) Files.lines(path).count();
      if (numLines >= 30) {
        startPoint = new Random().nextInt(numLines - 30);
      }
      log.info("random number is : {}", startPoint);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    CsvSchema csvSchema = CsvSchema.builder()
        .addColumn("Stock-ID", CsvSchema.ColumnType.STRING)
        .addColumn("Timestamp", CsvSchema.ColumnType.STRING)
        .addColumn("actual stock price at that timestamp", CsvSchema.ColumnType.NUMBER)
        .setUseHeader(false)
        .build();

    ObjectReader objectReader = new CsvMapper().reader(StockExchangeData.class).with(csvSchema);

    try (MappingIterator<StockExchangeData> dataIterator = objectReader.readValues(path.toFile())) {

      List<StockExchangeData> result = new ArrayList<>();
      Set<ConstraintViolation<StockExchangeData>> violations;

      //Ensure the file is not empty or has too few lines
      if (numLines >= 30) {
        while (dataIterator.hasNext()) {
          //Skip all data apart from the 30 rows to be checked. less data validation required
          if (dataIterator.getCurrentLocation().getLineNr() == startPoint) {
            StockExchangeData row = dataIterator.next();
            //start processing 30 rows of data from the random start point
            for (int i = 0; i < 30; i++) {
              violations = validator.validate(row);
              if (violations.isEmpty()) {
                result.add(row);
              } else {
                String errorMsg = violations.stream()
                    .map(violation -> " " + violation.getMessage() + ";")
                    .collect(Collectors.joining("", "Row " + row + " has errors:", ""));
                log.error("Data validation error : {} - {}", path, errorMsg);
              }
              row = dataIterator.next();
            }
            break;
          }
          dataIterator.next();
        }
      }

      return result;
    } catch (IOException e) {
      log.info("Error parsing file: {} - {}", path, e.getMessage());
      return null;
    }
  }

  /**
   * Process 30 rows of data and find the outliers by checking mean, standard deviation, and
   * percentage deviation
   *
   * @param stockExchangeData 30 rows of data
   * @return all detected outliers
   */
  public List<OutlierData> identifyOutliers(List<StockExchangeData> stockExchangeData) {
    log.info("Identifying outlier data for: {}", stockExchangeData);
    double mean = stockExchangeData.stream().mapToDouble(StockExchangeData::getStockPrice).average()
        .orElse(0);
    double standardDeviation = Math.sqrt(
        stockExchangeData.stream().mapToDouble(data -> Math.pow(data.getStockPrice() - mean, 2))
            .average().orElse(0));

    return stockExchangeData.stream()
        .filter(data -> Math.abs(data.getStockPrice() - mean) > 2 * standardDeviation)
        .map(data -> new OutlierData(data.getStockId(), data.getTimestamp(), data.getStockPrice(),
            mean, standardDeviation,
            (Math.abs(data.getStockPrice() - mean) / (2 * standardDeviation)) * 100))
        .collect(Collectors.toList());
  }

  /**
   * Save data to csv in the output folder
   *
   * @param data Data to be saved to csv
   * @param path CSV path
   */
  public void saveData(List<OutlierData> data, Path path) {
    log.info("Saving outlier data from file: {}", path);
    //headers to be added to the csv
    CsvSchema csvSchema = CsvSchema.builder()
        .addColumn("Stock-ID", CsvSchema.ColumnType.STRING)
        .addColumn("Timestamp", CsvSchema.ColumnType.STRING)
        .addColumn("actual stock price at that timestamp", CsvSchema.ColumnType.NUMBER)
        .addColumn("mean of 30 data points", CsvSchema.ColumnType.NUMBER)
        .addColumn("actual stock price – mean", CsvSchema.ColumnType.NUMBER)
        .addColumn("% deviation over and above the threshold", CsvSchema.ColumnType.NUMBER)
        .setUseHeader(true)
        .build();

    try {
      new CsvMapper().writer(csvSchema)
          .writeValue(new File(outputDataFolder + "/outliers_" + path.toFile().getName()), data);
    } catch (IOException e) {
      log.error("Error saving data to new CSV: {} - {}",
          outputDataFolder + "/outliers_" + path.toFile().getName(), e.getMessage());
    }
  }
}
