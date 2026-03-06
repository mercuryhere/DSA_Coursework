package q4;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SmartGridAllocator {

    static class Source {
        String id;
        String type;
        int maxCapacityPerHour;
        int startHour;
        int endHour;
        double costPerKwh;
        boolean renewable;

        Source(String id, String type, int maxCapacityPerHour, int startHour, int endHour,
               double costPerKwh, boolean renewable) {
            this.id = id;
            this.type = type;
            this.maxCapacityPerHour = maxCapacityPerHour;
            this.startHour = startHour;
            this.endHour = endHour;
            this.costPerKwh = costPerKwh;
            this.renewable = renewable;
        }

        boolean isAvailable(int hour) {
            return hour >= startHour && hour <= endHour;
        }
    }

    static class SourceState {
        Source source;
        int remaining;

        SourceState(Source source) {
            this.source = source;
            this.remaining = source.maxCapacityPerHour;
        }
    }

    static class HourResult {
        int hour;
        int[] demand;
        int[] target;
        int[][] allocation; // allocation[district][sourceIndex]
        double totalCost;
        int totalDelivered;
        int totalDemand;
        int renewableDelivered;
        boolean dieselUsed;

        HourResult(int hour, int[] demand, int[] target, int sourceCount) {
            this.hour = hour;
            this.demand = demand;
            this.target = target;
            this.allocation = new int[demand.length][sourceCount];
        }
    }

    static final String[] DISTRICTS = {"District A", "District B", "District C"};

    // The brief only shows 06, 07 and then "...", so the remaining hours below are sample values
    // created for demonstration.
    static final int[] HOURS = {
        6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };

    static final int[][] DEMANDS = {
        {20, 15, 25}, // 06
        {22, 16, 28}, // 07
        {24, 18, 30}, // 08
        {26, 19, 31}, // 09
        {28, 20, 33}, // 10
        {30, 22, 35}, // 11
        {32, 24, 36}, // 12
        {31, 23, 35}, // 13
        {29, 22, 34}, // 14
        {28, 21, 33}, // 15
        {27, 20, 31}, // 16
        {30, 24, 36}, // 17
        {32, 26, 38}, // 18
        {34, 28, 40}, // 19
        {35, 29, 42}, // 20
        {33, 27, 39}, // 21
        {30, 25, 36}, // 22
        {28, 22, 34}  // 23
    };

    static final Source[] SOURCES = {
        new Source("S1", "Solar", 50, 6, 18, 1.0, true),
        new Source("S2", "Hydro", 40, 0, 24, 1.5, true),
        new Source("S3", "Diesel", 60, 17, 23, 3.0, false)
    };

    public static int minAllowed(int demand) {
        return (int) Math.ceil(demand * 0.90);
    }

    public static int maxAllowed(int demand) {
        return (int) Math.floor(demand * 1.10);
    }

    public static int sum(int[] arr) {
        int total = 0;
        for (int x : arr) total += x;
        return total;
    }

    private static List<SourceState> getAvailableSources(int hour) {
        List<SourceState> states = new ArrayList<>();
        for (Source s : SOURCES) {
            if (s.isAvailable(hour)) {
                states.add(new SourceState(s));
            }
        }
        states.sort(Comparator.comparingDouble(a -> a.source.costPerKwh));
        return states;
    }

    // Dynamic Programming:
    // Finds district targets so that:
    // 1) each district is within ±10%
    // 2) total assigned energy equals chosen hourTarget
    // 3) assignments stay as close as possible to actual demand
    public static int[] chooseDistrictTargetsDP(int[] demand, int hourTarget) {
        int n = demand.length;
        int[] minVals = new int[n];
        int[] maxVals = new int[n];

        for (int i = 0; i < n; i++) {
            minVals[i] = minAllowed(demand[i]);
            maxVals[i] = maxAllowed(demand[i]);
        }

        boolean[][] dp = new boolean[n + 1][hourTarget + 1];
        dp[0][0] = true;

        for (int i = 1; i <= n; i++) {
            for (int total = 0; total <= hourTarget; total++) {
                for (int give = minVals[i - 1]; give <= maxVals[i - 1]; give++) {
                    if (total >= give && dp[i - 1][total - give]) {
                        dp[i][total] = true;
                        break;
                    }
                }
            }
        }

        int chosenTotal = -1;
        for (int total = hourTarget; total >= 0; total--) {
            if (dp[n][total]) {
                chosenTotal = total;
                break;
            }
        }

        if (chosenTotal == -1) {
            return null;
        }

        int[] target = new int[n];
        int remaining = chosenTotal;

        for (int i = n; i >= 1; i--) {
            int bestGive = -1;
            int bestDistance = Integer.MAX_VALUE;

            for (int give = minVals[i - 1]; give <= maxVals[i - 1]; give++) {
                if (remaining >= give && dp[i - 1][remaining - give]) {
                    int distance = Math.abs(give - demand[i - 1]);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestGive = give;
                    }
                }
            }

            target[i - 1] = bestGive;
            remaining -= bestGive;
        }

        return target;
    }

    private static HourResult processHour(int hour, int[] demand) {
        List<SourceState> available = getAvailableSources(hour);

        int totalDemand = sum(demand);
        int totalAvailable = 0;
        for (SourceState s : available) {
            totalAvailable += s.remaining;
        }

        int minTotal = 0;
        int maxTotal = 0;
        for (int d : demand) {
            minTotal += minAllowed(d);
            maxTotal += maxAllowed(d);
        }

        int hourTarget;
        if (totalAvailable >= totalDemand) {
            hourTarget = totalDemand;
        } else if (totalAvailable >= minTotal) {
            hourTarget = Math.min(totalAvailable, maxTotal);
        } else {
            hourTarget = totalAvailable; // shortage case
        }

        int[] target = chooseDistrictTargetsDP(demand, hourTarget);

        if (target == null) {
            // fallback: assign minimum possible in order
            target = new int[demand.length];
            int left = totalAvailable;
            for (int i = 0; i < demand.length; i++) {
                target[i] = Math.min(demand[i], left);
                left -= target[i];
            }
        }

        HourResult result = new HourResult(hour, demand, target, SOURCES.length);
        result.totalDemand = totalDemand;

        // Greedy allocation: cheapest source first
        for (int district = 0; district < DISTRICTS.length; district++) {
            int needed = target[district];

            for (SourceState state : available) {
                if (needed == 0) break;

                int used = Math.min(needed, state.remaining);
                if (used > 0) {
                    int sourceIndex = getSourceIndex(state.source.id);
                    result.allocation[district][sourceIndex] += used;
                    state.remaining -= used;
                    needed -= used;

                    result.totalCost += used * state.source.costPerKwh;
                    result.totalDelivered += used;

                    if (state.source.renewable) {
                        result.renewableDelivered += used;
                    } else {
                        result.dieselUsed = true;
                    }
                }
            }
        }

        return result;
    }

    public static int getSourceIndex(String sourceId) {
        for (int i = 0; i < SOURCES.length; i++) {
            if (SOURCES[i].id.equals(sourceId)) {
                return i;
            }
        }
        return -1;
    }

    private static void printHourResult(HourResult result) {
        System.out.println("\n====================================================");
        System.out.println("Hour: " + result.hour + ":00");
        System.out.println("Total Demand = " + result.totalDemand + " kWh");
        System.out.println("Total Delivered = " + result.totalDelivered + " kWh");
        System.out.printf("Hour Cost = Rs. %.2f%n", result.totalCost);

        for (int i = 0; i < DISTRICTS.length; i++) {
            int delivered = 0;
            for (int s = 0; s < SOURCES.length; s++) {
                delivered += result.allocation[i][s];
            }

            double percent = result.demand[i] == 0 ? 0.0 : (delivered * 100.0 / result.demand[i]);

            System.out.println("\n" + DISTRICTS[i]);
            System.out.println("Demand = " + result.demand[i] + " kWh");
            System.out.println("Target = " + result.target[i] + " kWh");

            for (int s = 0; s < SOURCES.length; s++) {
                System.out.println("  " + SOURCES[s].type + ": " + result.allocation[i][s] + " kWh");
            }

            System.out.println("Delivered = " + delivered + " kWh");
            System.out.printf("Fulfilled = %.2f%%%n", percent);
        }

        if (result.dieselUsed) {
            System.out.println("\nDiesel was used in this hour because cheaper renewable capacity was not enough.");
        } else {
            System.out.println("\nNo diesel used in this hour.");
        }
    }
public static void main(String[] args) {
    double grandCost = 0.0;
    int grandDemand = 0;
    int grandDelivered = 0;
    int grandRenewable = 0;

    List<String> dieselNotes = new ArrayList<>();

    for (int i = 0; i < HOURS.length; i++) {
        HourResult result = processHour(HOURS[i], DEMANDS[i]);
        printHourResult(result);

        grandCost += result.totalCost;
        grandDemand += result.totalDemand;
        grandDelivered += result.totalDelivered;
        grandRenewable += result.renewableDelivered;

        if (result.dieselUsed) {
            for (int d = 0; d < DISTRICTS.length; d++) {
                if (result.allocation[d][2] > 0) {
                    dieselNotes.add("Hour " + result.hour + ":00 - " + DISTRICTS[d]
                            + " used diesel (" + result.allocation[d][2] + " kWh) because solar + hydro were insufficient.");
                }
            }
        }
    }

    System.out.println("\n\n================ FINAL ANALYSIS ================");
    System.out.printf("Total Cost of Distribution = Rs. %.2f%n", grandCost);
    System.out.println("Total Demand = " + grandDemand + " kWh");
    System.out.println("Total Delivered = " + grandDelivered + " kWh");

    double renewablePercent = grandDelivered == 0 ? 0.0 : (grandRenewable * 100.0 / grandDelivered);
    System.out.printf("Renewable Share = %.2f%%%n", renewablePercent);

    if (dieselNotes.isEmpty()) {
        System.out.println("Diesel Usage: None");
    } else {
        System.out.println("Diesel Usage Details:");
        for (String note : dieselNotes) {
            System.out.println("- " + note);
        }
    }

    System.out.println("\nEfficiency / Trade-off Comment:");
    System.out.println("Greedy source selection keeps cost low by always using the cheapest available source first.");
    System.out.println("Dynamic programming is used to choose district targets within ±10% so the hourly assignment remains feasible.");
    System.out.println("This hybrid approach is simple and practical, but a more advanced optimizer could improve fairness and global optimality.");
} }