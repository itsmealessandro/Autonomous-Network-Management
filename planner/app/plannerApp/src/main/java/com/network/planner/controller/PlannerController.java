package com.network.planner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.network.planner.servicies.PlanActionsService;

import java.util.Map;

@RestController
@RequestMapping("/analysis")
public class PlannerController {

  private final PlanActionsService planActionsService;

  public PlannerController(PlanActionsService planActionsService) {
    this.planActionsService = planActionsService;

  }

  @PostMapping
  public ResponseEntity<String> receiveAnalysis(@RequestBody Map<String, String> analysisRecap) {
    System.out.println("[Planner] Ricevuta analisi dei sensori:");
    analysisRecap.forEach((sensor, status) -> System.out.println(" - " + sensor + ": " + status));

    planActionsService.checkSymptoms(analysisRecap);

    return ResponseEntity.ok("Analisi ricevuta con successo.");
  }
}
