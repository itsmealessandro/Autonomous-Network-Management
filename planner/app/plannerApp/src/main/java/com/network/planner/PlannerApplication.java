package com.network.planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // needed for pooling
@SpringBootApplication
public class PlannerApplication {

  public static void main(String[] args) {
    SpringApplication.run(PlannerApplication.class, args);
  }

}
