package com.network.analyzer.servicies;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AnalyzeServiceImplementation implements AnalyzeService {

  @Override
  public void analyzeBandwidth(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeBandwidth called with values: " + last5);
  }

  @Override
  public void analyzeLatency(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeLatency called with values: " + last5);
  }

  @Override
  public void analyzePacketLoss(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzePacketLoss called with values: " + last5);
  }

  @Override
  public void analyzeSuspiciousActivity(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeSuspiciousActivity called with values: " + last5);
  }

  @Override
  public void analyzeTrafficFlow(List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeTrafficFlow called with values: " + last5);
  }

  @Override
  public void analyzeNewSensor(String metric, List<Double> last5) {
    System.out.println("[AnalyzeService] analyzeNewSensor called with values: " + last5);

  }

}
