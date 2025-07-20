package com.network.analyzer.servicies;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AnalyzeServiceImplementation implements AnalyzeService {

  private static final String THRESHOLDS_FILE = "/simulated_env/thresholds.json";
  private static final String PLANNER_URL = "http://planner:8080/analysis";
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, String> alarmRecap = new HashMap<>();

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

  private void finalizeAndSendRecap() {
    try {
      System.out.println("[AnalyzeService] Invio POST a planner:/analysis con i risultati: " + alarmRecap);

      RestTemplate restTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<Map<String, String>> request = new HttpEntity<>(alarmRecap, headers);

      restTemplate.postForObject(PLANNER_URL, request, String.class);

    } catch (Exception e) {
      System.err.println("[AnalyzeService] Errore durante la POST al planner: " + e.getMessage());
    } finally {
      alarmRecap.clear(); // pulizia per la prossima analisi
    }
  }

  @Override
  public void analyzeBandwidth(List<Double> last5) {
    String result = analyzeMetric("bandwidth_usage", last5);
    alarmRecap.put("bandwidth_usage", result);
  }

  @Override
  public void analyzeLatency(List<Double> last5) {
    String result = analyzeMetric("latency", last5);
    alarmRecap.put("latency", result);
  }

  @Override
  public void analyzePacketLoss(List<Double> last5) {
    String result = analyzeMetric("packet_loss", last5);
    alarmRecap.put("packet_loss", result);
  }

  @Override
  public void analyzeSuspiciousActivity(List<Double> last5) {
    String result = analyzeMetric("suspicious_activity", last5);
    alarmRecap.put("suspicious_activity", result);
  }

  @Override
  public void analyzeTrafficFlow(List<Double> last5) {
    String result = analyzeMetric("traffic_flow", last5);
    alarmRecap.put("traffic_flow", result);
  }

  @Override
  public void analyzeNewSensor(String metric, List<Double> last5) {
    String result = analyzeMetric(metric, last5);
    alarmRecap.put(metric, result);
    finalizeAndSendRecap(); // solo alla fine dell'ultima analisi
  }
}
