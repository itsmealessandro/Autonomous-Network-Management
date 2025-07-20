package com.network.analyzer.servicies;

import java.util.List;

public interface AnalyzeService {

  void analyzeBandwidth(List<Double> last5);

  void analyzePacketLoss(List<Double> last5);

  void analyzeSuspiciousActivity(List<Double> last5);

  void analyzeLatency(List<Double> last5);

  void analyzeTrafficFlow(List<Double> last5);

  void analyzeNewSensor(String metric, List<Double> last5);

}
