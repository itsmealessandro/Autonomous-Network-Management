package com.network.analyzer.servicies;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.analyzer.servicies.model.PossibleSymptoms;

import org.springframework.stereotype.Service;

@Service
public class AnalyzeServiceImplementation implements AnalyzeService {

  private static final String THRESHOLDS_FILE = "/simulated_env/thresholds.json";
  private final ObjectMapper objectMapper = new ObjectMapper();

  private String analyzeMetric(String metricName, List<Double> last5) {
    System.out.println("[AnalyzeService] " + metricName + " chiamato con valori: " + last5);

    try {
      JsonNode root = objectMapper.readTree(new File(THRESHOLDS_FILE));
      JsonNode metricNode = root.path(metricName);
      boolean hasMax = metricNode.has("MAX");
      boolean hasMin = metricNode.has("MIN");

      double maxThreshold = hasMax ? metricNode.path("MAX").asDouble() : Double.MAX_VALUE;
      double minThreshold = hasMin ? metricNode.path("MIN").asDouble() : Double.MIN_VALUE;

      long countAboveMax = hasMax ? last5.stream().filter(val -> val > maxThreshold).count() : 0;
      long countBelowMin = hasMin ? last5.stream().filter(val -> val < minThreshold).count() : 0;
      long totalBreaches = countAboveMax + countBelowMin;

      if (totalBreaches == 1) {
        double breachedValue = last5.stream()
            .filter(val -> (hasMax && val > maxThreshold) || (hasMin && val < minThreshold))
            .findFirst()
            .orElse(-1.0);

        System.out.println("[AnalyzeService] FALSO ALLARME su " + metricName +
            ": valore anomalo " + breachedValue +
            " fuori soglia" +
            (hasMin ? " [" + minThreshold : "") +
            " - " + (hasMax ? maxThreshold : "") + "]");
        return PossibleSymptoms.FALSE.name();
      } else if (countAboveMax > 1) {
        System.out.println("[AnalyzeService] ⚠️ ALLARME HIGH su " + metricName +
            ": " + countAboveMax + " valori sopra la soglia massima [" + maxThreshold + "]");
        return PossibleSymptoms.HIGH.name();
      } else if (countBelowMin > 1) {
        System.out.println("[AnalyzeService] ⚠️ ALLARME LOW su " + metricName +
            ": " + countBelowMin + " valori sotto la soglia minima [" + minThreshold + "]");
        return PossibleSymptoms.LOW.name();
      }

      // Nessuna violazione grave
      return PossibleSymptoms.FINE.name();

    } catch (IOException e) {
      e.printStackTrace();
      return PossibleSymptoms.FALSE.name(); // oppure lancia un'eccezione
    }
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
