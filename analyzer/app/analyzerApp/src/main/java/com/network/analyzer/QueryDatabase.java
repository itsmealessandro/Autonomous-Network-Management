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
import com.network.analyzer.servicies.DatabaseService;

@Component
public class QueryDatabase {

  final DatabaseService databaseService;

  private final char[] TOKEN = "my-super-secret-token".toCharArray();
  // private final char[] TOKEN = System.getenv("INFLUX_TOKEN").toCharArray();
  private final String ORG = "network-monitoring";
  // private final String URL = "http://localhost:" +
  // System.getenv("INFLUX_PORT");
  private final String URL = "http://localhost:8086";

  public QueryDatabase(DatabaseService databaseService) {
    this.databaseService = databaseService;
  }

  @Scheduled(fixedRate = 5000) // 5 sec
  String getDataFromDB() {
    System.out.println("###################");
    System.out.println("scheduled method:");
    System.out.println(LocalDateTime.now());
    System.out.println("###################");
    databaseService.getBandwithWithInterval(5);

    InfluxDBClient influxDBClient = InfluxDBClientFactory.create(URL, TOKEN, ORG);

    QueryApi queryApi = influxDBClient.getQueryApi();

    String flux = "from(bucket: \"network-metrics\")\n" +
        "  |> range(start: -5s)\n" +
        "  |> filter(fn: (r) => r._field == \"value\")\n" +
        "  |> sort(columns: [\"_time\"])\n" +
        "  |> limit(n: 100)"; // aumenta questo numero se hai molte metriche

    List<FluxTable> tables = queryApi.query(flux);

    // Mappa per raccogliere i valori per ogni metrica
    Map<String, List<Object>> metricValues = new HashMap<>();

    for (FluxTable table : tables) {
      for (FluxRecord record : table.getRecords()) {
        String metric = record.getMeasurement();
        Object value = record.getValue();

        metricValues.computeIfAbsent(metric, k -> new ArrayList<>()).add(value);
      }
    }

    influxDBClient.close();

    // Stampa in formato leggibile: max 5 valori per metrica
    for (Map.Entry<String, List<Object>> entry : metricValues.entrySet()) {
      System.out.println("=== " + entry.getKey() + " ===");

      List<Object> values = entry.getValue();
      int start = Math.max(0, values.size() - 5); // Prendi gli ultimi 5 valori
      for (int i = start; i < values.size(); i++) {
        System.out.println(values.get(i));
      }
      System.out.println();
    }
    return "";
  }
}
