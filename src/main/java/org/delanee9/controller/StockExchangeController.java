package org.delanee9.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.delanee9.service.StockExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = "/stock")
public class StockExchangeController {

  private final StockExchangeService stockExchangeService;

  @GetMapping
  public ResponseEntity<String> retrieveOutliers(@RequestParam(defaultValue = "2") int fileLimit) {
    log.info("Called /stock");
    stockExchangeService.processData(fileLimit);
    return ResponseEntity.ok("Data processing started...");
  }
}
