package org.delanee9.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OutlierData {
    @JsonProperty("Stock-ID")
    private String stockId;
    @JsonProperty("Timestamp")
    private String timestamp;
    @JsonProperty("actual stock price at that timestamp")
    private double stockPrice;
    @JsonProperty("mean of 30 data points")
    private double meanPrice;
    @JsonProperty("actual stock price – mean")
    private double deviation;
    @JsonProperty("% deviation over and above the threshold")
    private double percentageDeviation;

    @Override
    public String toString() {
        return stockId + ',' + timestamp + "," + stockPrice + "," + meanPrice + "," + deviation + "," + percentageDeviation;
    }
}
