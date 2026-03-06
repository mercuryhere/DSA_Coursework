package task6;

import java.util.*;

// Finds the safest path where total path safety is the product of edge probabilities.
// We convert each edge probability p into weight = -log(p).
// Then the safest path becomes the shortest path problem.
public class SafestPathDijkstra {

    static class Edge {
        int to;
        double probability;

        Edge(int to, double probability) {
            this.to = to;
            this.probability = probability;
        }
    }

    static class State implements Comparable<State> {
        int node;
        double distance;

        State(int node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(State other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    private static final String[] cityNames = {
        "Kathmandu", "Pokhara", "Butwal", "Biratnagar", "Dhangadhi"
    };

    private static void addEdge(List<List<Edge>> graph, int from, int to, double probability) {
        graph.get(from).add(new Edge(to, probability));
        graph.get(to).add(new Edge(from, probability)); // undirected graph
    }

    private static void safestPath(List<List<Edge>> graph, int source, int destination) {
        int n = graph.size();
        double[] dist = new double[n];
        int[] parent = new int[n];
        boolean[] visited = new boolean[n];

        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(parent, -1);

        PriorityQueue<State> pq = new PriorityQueue<>();
        dist[source] = 0.0;
        pq.offer(new State(source, 0.0));

        while (!pq.isEmpty()) {
            State current = pq.poll();
            int u = current.node;

            if (visited[u]) continue;
            visited[u] = true;

            for (Edge edge : graph.get(u)) {
                int v = edge.to;
                double transformedWeight = -Math.log(edge.probability);

                if (!visited[v] && dist[u] + transformedWeight < dist[v]) {
                    dist[v] = dist[u] + transformedWeight;
                    parent[v] = u;
                    pq.offer(new State(v, dist[v]));
                }
            }
        }

        if (dist[destination] == Double.MAX_VALUE) {
            System.out.println("No path exists.");
            return;
        }

        List<Integer> path = new ArrayList<>();
        for (int at = destination; at != -1; at = parent[at]) {
            path.add(at);
        }
        Collections.reverse(path);

        double finalSafety = Math.exp(-dist[destination]);

        System.out.println("Safest path from " + cityNames[source] + " to " + cityNames[destination] + ":");
        for (int i = 0; i < path.size(); i++) {
            System.out.print(cityNames[path.get(i)]);
            if (i < path.size() - 1) {
                System.out.print(" -> ");
            }
        }
        System.out.println();

        System.out.printf("Path safety probability: %.6f%n", finalSafety);
    }

    public static void main(String[] args) {
        int n = 5;
        List<List<Edge>> graph = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        // Example network
        addEdge(graph, 0, 1, 0.95); // Kathmandu - Pokhara
        addEdge(graph, 0, 2, 0.90); // Kathmandu - Butwal
        addEdge(graph, 1, 2, 0.85); // Pokhara - Butwal
        addEdge(graph, 1, 3, 0.80); // Pokhara - Biratnagar
        addEdge(graph, 2, 3, 0.88); // Butwal - Biratnagar
        addEdge(graph, 2, 4, 0.92); // Butwal - Dhangadhi
        addEdge(graph, 3, 4, 0.75); // Biratnagar - Dhangadhi

        safestPath(graph, 0, 4); // Kathmandu to Dhangadhi
    }
}