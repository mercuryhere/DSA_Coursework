package task6;

import java.util.*;

public class EdmondsKarpMaxFlow {

    private static final String[] NAMES = {"KTM", "JA", "JB", "PH", "BS"};

    public static boolean bfs(int[][] residual, int source, int sink, int[] parent) {
        int n = residual.length;
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(source);
        visited[source] = true;
        parent[source] = -1;

        while (!queue.isEmpty()) {
            int u = queue.poll();

            for (int v = 0; v < n; v++) {
                if (!visited[v] && residual[u][v] > 0) {
                    visited[v] = true;
                    parent[v] = u;
                    queue.offer(v);

                    if (v == sink) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int edmondsKarp(int[][] capacity, int source, int sink) {
        int n = capacity.length;
        int[][] residual = new int[n][n];

        for (int i = 0; i < n; i++) {
            System.arraycopy(capacity[i], 0, residual[i], 0, n);
        }

        int[] parent = new int[n];
        int maxFlow = 0;

        while (bfs(residual, source, sink, parent)) {
            int pathFlow = Integer.MAX_VALUE;

            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                pathFlow = Math.min(pathFlow, residual[u][v]);
            }

            List<Integer> path = new ArrayList<>();
            for (int v = sink; v != -1; v = parent[v]) {
                path.add(v);
            }
            Collections.reverse(path);

            System.out.print("Augmenting path: ");
            for (int i = 0; i < path.size(); i++) {
                System.out.print(NAMES[path.get(i)]);
                if (i < path.size() - 1) System.out.print(" -> ");
            }
            System.out.println(" | Flow added = " + pathFlow);

            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                residual[u][v] -= pathFlow;
                residual[v][u] += pathFlow;
            }

            maxFlow += pathFlow;
        }

        printMinCut(residual, source);
        return maxFlow;
    }

    public static void printMinCut(int[][] residual, int source) {
        int n = residual.length;
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(source);
        visited[source] = true;

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v = 0; v < n; v++) {
                if (!visited[v] && residual[u][v] > 0) {
                    visited[v] = true;
                    queue.offer(v);
                }
            }
        }

        System.out.print("Minimum cut (reachable from source): { ");
        for (int i = 0; i < n; i++) {
            if (visited[i]) System.out.print(NAMES[i] + " ");
        }
        System.out.println("}");
    }

    public static void main(String[] args) {
        int KTM = 0, JA = 1, JB = 2, PH = 3, BS = 4;
        int[][] capacity = new int[5][5];

        // Exact capacity data from the brief
        capacity[KTM][JA] = 10;
        capacity[KTM][JB] = 15;
        capacity[JA][KTM] = 10;
        capacity[JA][PH] = 8;
        capacity[JA][BS] = 5;
        capacity[JB][KTM] = 15;
        capacity[JB][JA] = 4;
        capacity[JB][BS] = 12;
        capacity[PH][JA] = 8;
        capacity[PH][BS] = 6;
        capacity[BS][JA] = 5;
        capacity[BS][JB] = 12;
        capacity[BS][PH] = 6;

        int maxFlow = edmondsKarp(capacity, KTM, BS);
        System.out.println("Maximum flow from KTM to BS = " + maxFlow);
    }
}