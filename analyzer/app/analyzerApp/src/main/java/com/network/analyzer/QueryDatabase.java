package com.network.analyzer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.network.analyzer.servicies.AnalyzeService;

@Component
public class QueryDatabase {

  final AnalyzeService databaseService;

  private final char[] TOKEN = System.getenv("INFLUX_TOKEN").toCharArray();
  // private final char[] TOKEN = "my-super-secret-token".toCharArray();
  private final String ORG = "network-monitoring";
  private final String URL = "http://influxdb:8086";

  private static final String PLANNER_PORT = System.getenv("PLANNER_PORT");
  private static final String PLANNER_URL = "http://planner:" + PLANNER_PORT + "/analysis";

  public QueryDatabase(AnalyzeService databaseService) {
    this.databaseService = databaseService;
  }

  @Scheduled(fixedRate = 5000) // ogni 5 secondi
  public void getDataFromDB() {
    System.out.println("###################");
    System.out.println("scheduled method:");
    System.out.println(LocalDateTime.now());
    System.out.println("###################");

    InfluxDBClient influxDBClient = InfluxDBClientFactory.create(URL, TOKEN, ORG);
    QueryApi queryApi = influxDBClient.getQueryApi();

    String flux = "from(bucket: \"network-metrics\")\n" +
        "  |> range(start: 0)\n" + // tutti i dati storici
        "  |> filter(fn: (r) => r._field == \"value\")\n" +
        "  |> sort(columns: [\"_time\"], desc: true)\n" + // dal più recente
        "  |> limit(n: 5)\n" +
        "  |> sort(columns: [\"_time\"])"; // riordina cronologicamente

    List<FluxTable> tables = queryApi.query(flux);

    Map<String, List<Double>> metricValues = new HashMap<>();

    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        String metric = record.getMeasurement();
        Object value = record.getValue();

        if (value instanceof Number) {
          metricValues.computeIfAbsent(metric, k -> new ArrayList<>()).add(((Number) value).doubleValue());
        }
      }
    }

    influxDBClient.close();

    // Mappa dove salvo i risultati di tutte le analisi
    Map<String, String> analysisResults = new HashMap<>();

    for (Map.Entry<String, List<Double>> entry : metricValues.entrySet()) {
      String metric = entry.getKey();
      List<Double> values = entry.getValue();

      // Prendo gli ultimi 5 valori
      int start = Math.max(0, values.size() - 5);
      List<Double> last5 = values.subList(start, values.size());

      // Stampa valori
      System.out.println("=== " + metric + " ===");
      last5.forEach(System.out::println);
      System.out.println();

      // Chiamo i metodi di analisi e salvo i risultati
      String result;
      switch (metric) {
        case "bandwidth_usage":
          result = databaseService.analyzeBandwidth(last5);
          break;
        case "packet_loss":
          result = databaseService.analyzePacketLoss(last5);
          break;
        case "suspicious_activity":
          result = databaseService.analyzeSuspiciousActivity(last5);
          break;
        case "latency":
          result = databaseService.analyzeLatency(last5);
          break;
        case "traffic_flow":
          result = databaseService.analyzeTrafficFlow(last5);
          break;
        default:
          result = databaseService.analyzeNewSensor(metric, last5);
      }

      analysisResults.put(metric, result);
    }

    // Alla fine invio tutto al planner con un'unica POST
    sendResultsToPlanner(analysisResults);
  }

  private void sendResultsToPlanner(Map<String, String> results) {
    try {
      System.out.println("[QueryDatabase] Invio POST a planner:/analysis con i risultati: " + results);

      RestTemplate restTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<Map<String, String>> request = new HttpEntity<>(results, headers);

      restTemplate.postForObject(PLANNER_URL, request, String.class);

    } catch (Exception e) {
      System.err.println("[QueryDatabase] Errore durante la POST al planner: " + e.getMessage());
    }
  }
}
