package q1;

import java.util.HashMap;
import java.util.Map;

public class MaxPointsOnLine {

    // Returns the maximum number of points that lie on the same straight line.
    // Time: O(n^2) average (hash maps), Space: O(n)
    public static int maxPoints(int[][] points) {
        if (points == null || points.length == 0) return 0;
        if (points.length <= 2) return points.length;

        int n = points.length;
        int best = 1;

        for (int i = 0; i < n; i++) {
            // Slope map for lines through point i
            Map<String, Integer> slopeCount = new HashMap<>();
            int duplicates = 0; // points identical to points[i]
            int localBest = 0;

            int x1 = points[i][0];
            int y1 = points[i][1];

            for (int j = i + 1; j < n; j++) {
                int x2 = points[j][0];
                int y2 = points[j][1];

                if (x1 == x2 && y1 == y2) {
                    duplicates++;
                    continue;
                }

                int dx = x2 - x1;
                int dy = y2 - y1;

                // Normalize slope by gcd and sign
                int g = gcd(Math.abs(dx), Math.abs(dy));
                dx /= g;
                dy /= g;

                // Ensure unique sign convention:
                // - keep dx positive, or if dx==0 (vertical), make dy=1
                // - if dy==0 (horizontal), make dx=1
                if (dx == 0) {
                    dy = 1; // vertical line
                } else if (dy == 0) {
                    dx = 1; // horizontal line
                } else {
                    // Make dx positive; if dx negative flip both
                    if (dx < 0) {
                        dx = -dx;
                        dy = -dy;
                    }
                }

                String key = dy + "/" + dx;
                int count = slopeCount.getOrDefault(key, 0) + 1;
                slopeCount.put(key, count);
                localBest = Math.max(localBest, count);
            }

            // localBest counts points besides i, so add 1 (point i) and duplicates
            best = Math.max(best, localBest + 1 + duplicates);
        }

        return best;
    }

    private static int gcd(int a, int b) {
        if (a == 0) return b == 0 ? 1 : b;
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    // Quick local test
    public static void main(String[] args) {
        int[][] pts1 = {{1, 1}, {2, 2}, {3, 3}};
        System.out.println("Expected 3, got: " + maxPoints(pts1));

        int[][] pts2 = {{1, 1}, {3, 2}, {5, 3}, {4, 1}, {2, 3}, {1, 4}};
        System.out.println("Expected 4, got: " + maxPoints(pts2));

        int[][] pts3 = {{0, 0}, {0, 0}, {1, 1}};
        System.out.println("Expected 3, got: " + maxPoints(pts3));

        int[][] pts4 = {{0, 0}, {0, 1}, {0, 2}};
        System.out.println("Expected 3, got: " + maxPoints(pts4));
    }
}