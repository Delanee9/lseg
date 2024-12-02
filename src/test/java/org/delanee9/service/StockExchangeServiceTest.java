package org.delanee9.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.delanee9.model.OutlierData;
import org.delanee9.model.StockExchangeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockExchangeServiceTest {

  @InjectMocks
  private StockExchangeService stockExchangeService;

  @BeforeEach
  void setUp() {
    System.setProperty("lseg.input.path", "src/test/resources/inputData");
    System.setProperty("lseg.output.path", "src/test/resources/outputData");
  }

  @Test
  void testSearchFiles_success() {
    Path csvPath = Path.of("src/test/resources/inputData/NYSE/VALID_DATA.csv");
    List<Path> result = stockExchangeService.searchFiles(csvPath, 10);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  void testSearchFiles_failure() {
    Path csvPath = Path.of("src/test/resources/inputData/NYSE/MISSING.csv");
    List<Path> result = stockExchangeService.searchFiles(csvPath, 2);
    assertNull(result);
  }

  @Test
  void testParseFile_success() {
    Path csvPath = Path.of("src/test/resources/inputData/NYSE/VALID_DATA.csv");
    List<StockExchangeData> result = stockExchangeService.parseFile(csvPath);
    assertFalse(result.isEmpty());
  }

  @Test
  void testParseFile_failure() {
    Path csvPath = Path.of("src/test/resources/inputData/NYSE/MISSING_DATA.csv");
    List<StockExchangeData> result = stockExchangeService.parseFile(csvPath);
    assertTrue(result.isEmpty());
  }

  @Test
  void testParseFile_shortFile_failure() {
    Path csvPath = Path.of("src/test/resources/inputData/NYSE/SHORT_FILE.csv");
    List<StockExchangeData> result = stockExchangeService.parseFile(csvPath);
    assertTrue(result.isEmpty());
  }

  @Test
  void testIdentifyOutliers_outliersNotFound_success() {
    List<StockExchangeData> mockData = List.of(
        new StockExchangeData("ID", "20-09-2023", 200.02),
        new StockExchangeData("ID", "21-09-2023", 207.93),
        new StockExchangeData("ID", "23-09-2023", 20227.93)
    );
    List<OutlierData> result = stockExchangeService.identifyOutliers(mockData);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testIdentifyOutliers_foundOutliers_success() {
    List<StockExchangeData> mockData = List.of(
        new StockExchangeData("ID1", "20-09-2023", 207.93),
        new StockExchangeData("ID2", "21-09-2023", 207.93),
        new StockExchangeData("ID3", "23-09-2023", 207.93)
    );
    List<OutlierData> result = stockExchangeService.identifyOutliers(mockData);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testSaveData_success() {
    List<OutlierData> mockOutliers = List.of(
        new OutlierData("Potato", "25-09-2023", 100.0, 90.0, 10.0, 5.0)
    );
    Path outputPath = Path.of("src/test/resources/outputData/test_output.csv");
    stockExchangeService.saveData(mockOutliers, outputPath);
  }
}
