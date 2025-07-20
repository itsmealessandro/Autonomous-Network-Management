package com.network.analyzer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.network.analyzer.servicies.AnalyzeService;

/*
 * This component is used to get data from influxdb.
 * It has a scheduled getDataFromDB() method that runs every x seconds
 * */
@Component
public class QueryDatabase {

  final AnalyzeService databaseService;

  private final char[] TOKEN = "my-super-secret-token".toCharArray();
  // private final char[] TOKEN = System.getenv("INFLUX_TOKEN").toCharArray();
  private final String ORG = "network-monitoring";
  // System.getenv("INFLUX_PORT");
  private final String URL = "http://influxdb:8086";

  public QueryDatabase(AnalyzeService databaseService) {
    this.databaseService = databaseService;
  }

  @Scheduled(fixedRate = 5000) // 5 sec
  String getDataFromDB() {
    System.out.println("###################");
    System.out.println("scheduled method:");
    System.out.println(LocalDateTime.now());
    System.out.println("###################");

    InfluxDBClient influxDBClient = InfluxDBClientFactory.create(URL, TOKEN, ORG);
    QueryApi queryApi = influxDBClient.getQueryApi();

    String flux = "from(bucket: \"network-metrics\")\n" +
        "  |> range(start: 0)\n" + // include tutti i dati storici
        "  |> filter(fn: (r) => r._field == \"value\")\n" +
        "  |> sort(columns: [\"_time\"], desc: true)\n" + // ordina dal più recente
        "  |> limit(n: 5)\n" +
        "  |> sort(columns: [\"_time\"])"; // opzionale: riordina in ordine cronologico

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

    for (Map.Entry<String, List<Double>> entry : metricValues.entrySet()) {
      String metric = entry.getKey();
      List<Double> values = entry.getValue();

      // Prendi gli ultimi 5 valori
      int start = Math.max(0, values.size() - 5);
      List<Double> last5 = values.subList(start, values.size());

      // Stampa
      System.out.println("=== " + metric + " ===");
      last5.forEach(System.out::println);
      System.out.println();

      // Invoca i metodi del servizio in base alla metrica
      switch (metric) {
        case "bandwidth_usage":
          databaseService.analyzeBandwidth(last5);
          break;
        case "packet_loss":
          databaseService.analyzePacketLoss(last5);
          break;
        case "suspicious_activity":
          databaseService.analyzeSuspiciousActivity(last5);
          break;
        case "latency":
          databaseService.analyzeLatency(last5);
          break;
        case "traffic_flow":
          databaseService.analyzeTrafficFlow(last5);
          break;
        default:
          // Gestione sensori dinamici (es: Network/sensoreX/value)
          databaseService.analyzeNewSensor(metric, last5);
      }
    }

    return "";
  }

}
