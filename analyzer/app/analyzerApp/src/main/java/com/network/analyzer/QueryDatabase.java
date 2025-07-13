package com.network.analyzer;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import com.network.analyzer.servicies.DatabaseService;

@Controller
public class QueryDatabase {

  final DatabaseService databaseService;

  public QueryDatabase(DatabaseService databaseService) {
    this.databaseService = databaseService;
  }

  @Scheduled(fixedRate = 5000) // 5 sec
  String queryBandwithData() {
    System.out.println("###################");
    System.out.println("scheduled method:");
    System.out.println(LocalDateTime.now());
    System.out.println("###################");

    databaseService.getBandwithWithInterval(5);

    return "";
  }
}
