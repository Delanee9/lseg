package org.delanee9.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockExchangeData {

  @NotBlank(message = "Stock ID field can not be empty")
  @JsonProperty("Stock-ID")
  private String stockId;

  @NotBlank(message = "Timestamp field can not be empty")
  @Pattern(regexp = "^(3[01]|[12][0-9]|0[1-9])-(1[0-2]|0[1-9])-(\\d{4}$)", message = "Timestamp must be in the format dd-mm-yyyy")
  @JsonProperty("Timestamp")
  private String timestamp;

  @NotNull(message = "Stock Price field can not be empty")
  @PositiveOrZero(message = "Stock Price must be a positive number")
  @JsonProperty("actual stock price at that timestamp")
  private double stockPrice;

  @Override
  public String toString() {
    return stockId + ',' + timestamp + ',' + stockPrice;
  }
}
