# DSA Coursework – Java Solutions

## Student Information
**Name:** Hritik Parajuli  
**Course:** Programming for Developers / DSA Coursework  
**Language Used:** Java  
**IDE Used:** Visual Studio Code  

---

## Project Overview
This repository contains my Java solutions for the DSA coursework. The project includes algorithm-based problems, graph problems, dynamic programming, greedy methods, and GUI-based applications using Java Swing.

The work is organized question-wise inside the `src` folder.

---

## Project Structure

```text
src/
  q1/
    MaxPointsOnLine.java
    WordBreakAllSentences.java
  q2/
    MaxPathSumBinaryTree.java
  q3/
    StockMaxProfitKTransactions.java
  q4/
    SmartGridAllocator.java
  q5/
    TouristOptimizerApp.java
    WeatherCollectorApp.java
  task6/
    SafestPathDijkstra.java
    EdmondsKarpMaxFlow.java
Implemented Questions
Q1
1(a) Max Points on a Line

Finds the maximum number of points that lie on the same straight line.

1(b) Word Break – All Sentences

Uses DFS with memoization to generate all valid sentences from a given string using dictionary words.

Q2
Maximum Path Sum in Binary Tree

Computes the maximum path sum in a binary tree using recursion and depth-first search.

Q3
Stock Buy and Sell with At Most K Transactions

Uses dynamic programming to calculate the maximum profit possible with at most k buy-sell transactions.

Q4
Smart Grid Energy Allocation

Implements a hybrid approach using greedy strategy and dynamic programming to distribute electricity across districts while considering:

source availability

cost per unit

renewable usage

diesel fallback

±10% demand flexibility

Q5
5(a) Tourist Spot Optimizer

A Java Swing GUI application that creates an itinerary based on:

available time

budget

interest tags

It compares a heuristic-based route with a brute-force solution and also shows a simple coordinate path view.

5(b) Weather Collector App

A Java Swing GUI application that collects weather data for five cities and compares:

sequential fetching

parallel fetching using multithreading

It displays:

temperature

humidity

pressure

latency comparison chart

execution log

Task 6
6(a) Safest Path using Dijkstra

Finds the safest route in a graph by transforming edge probabilities and applying Dijkstra’s algorithm.

6(b) Maximum Flow using Edmonds-Karp

Computes the maximum flow in a network using the Edmonds-Karp algorithm and also identifies the reachable side of the minimum cut.

Algorithms and Concepts Used

Brute Force

Greedy Algorithm

Dynamic Programming

Depth-First Search

Recursion

Memoization

Dijkstra’s Algorithm

Edmonds-Karp Algorithm

Multithreading

Java Swing GUI

How to Compile and Run

Open the project folder in terminal and use the following commands.

Compile
javac -d out src\q1\MaxPointsOnLine.java
javac -d out src\q1\WordBreakAllSentences.java
javac -d out src\q2\MaxPathSumBinaryTree.java
javac -d out src\q3\StockMaxProfitKTransactions.java
javac -d out src\q4\SmartGridAllocator.java
javac -d out src\q5\TouristOptimizerApp.java
javac -d out src\q5\WeatherCollectorApp.java
javac -d out src\task6\SafestPathDijkstra.java
javac -d out src\task6\EdmondsKarpMaxFlow.java
Run
java -cp out q1.MaxPointsOnLine
java -cp out q1.WordBreakAllSentences
java -cp out q2.MaxPathSumBinaryTree
java -cp out q3.StockMaxProfitKTransactions
java -cp out q4.SmartGridAllocator
java -cp out q5.TouristOptimizerApp
java -cp out q5.WeatherCollectorApp
java -cp out task6.SafestPathDijkstra
java -cp out task6.EdmondsKarpMaxFlow
Weather App Note

For WeatherCollectorApp.java, a valid OpenWeatherMap API key is needed for live API mode.

In the source code:

private static final String API_KEY = "PASTE_YOUR_OPENWEATHERMAP_API_KEY_HERE";

If no valid key is provided, the application can be kept in demo mode for GUI testing.

Output Summary

This project demonstrates:

correct implementation of core DSA concepts

clean Java package structure

console-based and GUI-based solutions

comparison of exact and heuristic approaches

comparison of sequential and parallel execution

Author

Hritik Parajuli


