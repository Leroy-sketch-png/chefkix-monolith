package com.chefkix.identity.service;

public interface StatisticsCounterOperations {

  void incrementCounter(String userId, String fieldName, int amount);
}