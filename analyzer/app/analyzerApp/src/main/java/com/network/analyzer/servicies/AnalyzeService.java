package com.network.analyzer.servicies;

import java.util.List;

public interface AnalyzeService {

  String analyzeBandwidth(List<Double> last5);

  String analyzePacketLoss(List<Double> last5);

  String analyzeSuspiciousActivity(List<Double> last5);

  String analyzeLatency(List<Double> last5);

  String analyzeTrafficFlow(List<Double> last5);

  String analyzeNewSensor(String metric, List<Double> last5);

}
