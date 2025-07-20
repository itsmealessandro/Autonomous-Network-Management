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

  private String analyzeMetric(String metricName, List<Double> last5) {
    System.out.println("[AnalyzeService] " + metricName + " called with values: " + last5);

    try {
      JsonNode root = objectMapper.readTree(new File(THRESHOLDS_FILE));
      double maxThreshold = root.path(metricName).path("MAX").asDouble();
      double minThreshold = root.path(metricName).path("MIN").asDouble();

      long countAboveMax = last5.stream().filter(val -> val > maxThreshold).count();
      long countBelowMin = last5.stream().filter(val -> val < minThreshold).count();
      long totalBreaches = countAboveMax + countBelowMin;

      if (totalBreaches == 1) {
        double breachedValue = last5.stream()
            .filter(val -> val > maxThreshold || val < minThreshold)
            .findFirst()
            .orElse(-1.0);

        System.out.println("[AnalyzeService] FALSO ALLARME su " + metricName +
            ": valore anomalo " + breachedValue +
            " fuori soglia [" + minThreshold + " - " + maxThreshold + "]");
        return "FALSO";
      } else if (totalBreaches > 1) {
        System.out.println("[AnalyzeService] ⚠️ ALLARME su " + metricName +
            ": " + totalBreaches + " valori fuori soglia [" + minThreshold + " - " + maxThreshold + "]");
        return "VERO";
      }

    } catch (IOException e) {
      System.err.println("[AnalyzeService] Errore nella lettura di " + THRESHOLDS_FILE + ": " + e.getMessage());
    }

    return "NESSUNO";
  }

  @Override
  public String analyzeBandwidth(List<Double> last5) {
    return analyzeMetric("bandwidth_usage", last5);
  }

  @Override
  public String analyzeLatency(List<Double> last5) {
    return analyzeMetric("latency", last5);
  }

  @Override
  public String analyzePacketLoss(List<Double> last5) {
    return analyzeMetric("packet_loss", last5);
  }

  @Override
  public String analyzeSuspiciousActivity(List<Double> last5) {
    return analyzeMetric("suspicious_activity", last5);
  }

  @Override
  public String analyzeTrafficFlow(List<Double> last5) {
    return analyzeMetric("traffic_flow", last5);
  }

  @Override
  public String analyzeNewSensor(String metric, List<Double> last5) {
    return analyzeMetric(metric, last5);
  }
}
