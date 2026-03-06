package q3;

import java.util.Arrays;

public class StockMaxProfitKTransactions {

    // Returns maximum profit with at most k transactions
    public static int maxProfit(int k, int[] prices) {
        if (prices == null || prices.length == 0 || k == 0) {
            return 0;
        }

        int n = prices.length;

        // If k is large enough, treat it like unlimited transactions
        if (k >= n / 2) {
            int profit = 0;
            for (int i = 1; i < n; i++) {
                if (prices[i] > prices[i - 1]) {
                    profit += prices[i] - prices[i - 1];
                }
            }
            return profit;
        }

        // dp[t][d] = max profit using at most t transactions up to day d
        int[][] dp = new int[k + 1][n];

        for (int t = 1; t <= k; t++) {
            int maxDiff = -prices[0];

            for (int d = 1; d < n; d++) {
                dp[t][d] = Math.max(dp[t][d - 1], prices[d] + maxDiff);
                maxDiff = Math.max(maxDiff, dp[t - 1][d] - prices[d]);
            }
        }

        return dp[k][n - 1];
    }

    // Prints DP table for explanation/debugging
    public static void printDpTable(int k, int[] prices) {
        if (prices == null || prices.length == 0 || k == 0) {
            System.out.println("No DP table to show.");
            return;
        }

        int n = prices.length;
        int[][] dp = new int[k + 1][n];

        for (int t = 1; t <= k; t++) {
            int maxDiff = -prices[0];
            for (int d = 1; d < n; d++) {
                dp[t][d] = Math.max(dp[t][d - 1], prices[d] + maxDiff);
                maxDiff = Math.max(maxDiff, dp[t - 1][d] - prices[d]);
            }
        }

        System.out.println("\nDP Table:");
        for (int t = 0; t <= k; t++) {
            System.out.println("Transactions " + t + ": " + Arrays.toString(dp[t]));
        }
    }

    public static void runTest(int k, int[] prices, int expected) {
        int result = maxProfit(k, prices);

        System.out.println("k = " + k);
        System.out.println("Prices = " + Arrays.toString(prices));
        System.out.println("Expected Profit = " + expected);
        System.out.println("Computed Profit = " + result);
        System.out.println(result == expected ? "Status: PASS" : "Status: FAIL");
        System.out.println("--------------------------------------");
    }

    public static void main(String[] args) {
        // Example from brief
        runTest(2, new int[]{2000, 4000, 1000}, 2000);

        // More test cases
        runTest(2, new int[]{3, 2, 6, 5, 0, 3}, 7);
        runTest(2, new int[]{2, 4, 1}, 2);
        runTest(3, new int[]{10, 22, 5, 75, 65, 80}, 97);
        runTest(1, new int[]{7, 6, 4, 3, 1}, 0);

        // Optional DP view
        printDpTable(2, new int[]{3, 2, 6, 5, 0, 3});
    }
}