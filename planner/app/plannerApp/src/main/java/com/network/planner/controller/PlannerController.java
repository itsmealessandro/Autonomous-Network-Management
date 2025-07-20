package com.network.planner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analysis")
public class PlannerController {

  @PostMapping
  public ResponseEntity<String> receiveAnalysis(@RequestBody Map<String, String> analysisRecap) {
    System.out.println("[Planner] Ricevuta analisi dei sensori:");
    analysisRecap.forEach((sensor, status) -> System.out.println(" - " + sensor + ": " + status));

    // Puoi fare logica aggiuntiva qui: salvataggio DB, pianificazione azioni, ecc.

    return ResponseEntity.ok("Analisi ricevuta con successo.");
  }
}
