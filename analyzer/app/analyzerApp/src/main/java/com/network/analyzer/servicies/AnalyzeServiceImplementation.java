package com.network.analyzer.servicies;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AnalyzeServiceImplementation implements AnalyzeService {

  private static final String THRESHOLDS_FILE = "/simulated_env/thresholds.json";
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void analyzeBandwidth(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeBandwidth called with values: " + last5);

    try {
      JsonNode root = objectMapper.readTree(new File(THRESHOLDS_FILE));
      double maxThreshold = root.path("bandwidth_usage").path("MAX").asDouble();

      long count = last5.stream().filter(val -> val > maxThreshold).count();

      if (count == 1) {
        double exceededValue = last5.stream().filter(val -> val > maxThreshold).findFirst().orElse(-1.0);
        System.out.println("[AnalyzeService] piccolo problema: valore " + exceededValue +
            " supera il massimo consentito di " + maxThreshold);
      }

    } catch (IOException e) {
      System.err.println("[AnalyzeService] Errore nella lettura di " + THRESHOLDS_FILE + ": " + e.getMessage());
    }
  }

  // Gli altri metodi restano invariati

  @Override
  public void analyzeLatency(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeLatency called with values: " + last5);
  }

  @Override
  public void analyzePacketLoss(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzePacketLoss called with values: " + last5);
  }

  @Override
  public void analyzeSuspiciousActivity(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeSuspiciousActivity called with values: " + last5);
  }

  @Override
  public void analyzeTrafficFlow(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeTrafficFlow called with values: " + last5);
  }

  @Override
  public void analyzeNewSensor(String metric, List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeNewSensor called with values: " + last5);
  }
}
