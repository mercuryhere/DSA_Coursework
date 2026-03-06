package q5;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class TouristOptimizerApp extends JFrame {

    private static final int VISIT_DURATION_MIN = 60;
    private static final double START_LAT = 27.7100;
    private static final double START_LON = 85.3170;

    private final List<Spot> spots = Arrays.asList(
            new Spot("Pashupatinath Temple", 27.7104, 85.3488, 100, "06:00", "18:00",
                    "culture", "religious"),
            new Spot("Swayambhunath Stupa", 27.7149, 85.2906, 200, "07:00", "17:00",
                    "culture", "heritage"),
            new Spot("Garden of Dreams", 27.7125, 85.3170, 150, "09:00", "21:00",
                    "nature", "relaxation"),
            new Spot("Chandragiri Hills", 27.6616, 85.2458, 700, "09:00", "17:00",
                    "nature", "adventure"),
            new Spot("Kathmandu Durbar Square", 27.7048, 85.3076, 100, "10:00", "17:00",
                    "culture", "heritage")
    );

    private JTextField hoursField;
    private JTextField budgetField;
    private JTextField tagsField;
    private JComboBox<String> startTimeBox;
    private JTextArea resultArea;
    private JLabel summaryLabel;
    private PathPanel pathPanel;

    public TouristOptimizerApp() {
        setTitle("Tourist Spot Optimizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new GridLayout(2, 5, 8, 8));

        inputPanel.add(new JLabel("Available Time (hours):"));
        inputPanel.add(new JLabel("Maximum Budget (Rs.):"));
        inputPanel.add(new JLabel("Interest Tags (comma-separated):"));
        inputPanel.add(new JLabel("Start Time:"));
        inputPanel.add(new JLabel(""));

        hoursField = new JTextField("6");
        budgetField = new JTextField("1000");
        tagsField = new JTextField("culture, heritage");
        startTimeBox = new JComboBox<>(new String[]{
                "08:00", "09:00", "10:00", "11:00"
        });

        JButton optimizeButton = new JButton("Generate Itinerary");
        optimizeButton.addActionListener(e -> runPlanner());

        inputPanel.add(hoursField);
        inputPanel.add(budgetField);
        inputPanel.add(tagsField);
        inputPanel.add(startTimeBox);
        inputPanel.add(optimizeButton);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(resultArea);

        pathPanel = new PathPanel();
        pathPanel.setPreferredSize(new Dimension(430, 500));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, pathPanel);
        splitPane.setResizeWeight(0.62);

        summaryLabel = new JLabel("Enter inputs and generate a route.");
        summaryLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        root.add(inputPanel, BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        root.add(summaryLabel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void runPlanner() {
        try {
            double availableHours = Double.parseDouble(hoursField.getText().trim());
            double maxBudget = Double.parseDouble(budgetField.getText().trim());

            if (availableHours <= 0 || maxBudget < 0) {
                JOptionPane.showMessageDialog(this, "Please enter valid time and budget values.");
                return;
            }

            Set<String> interests = parseTags(tagsField.getText());
            int startTime = parseTime((String) startTimeBox.getSelectedItem());

            VisitPlan heuristicPlan = buildHeuristicPlan(spots, availableHours, maxBudget, interests, startTime);
            VisitPlan bruteForcePlan = buildBruteForcePlan(spots, availableHours, maxBudget, interests, startTime);

            resultArea.setText(buildReport(heuristicPlan, bruteForcePlan, interests, availableHours, maxBudget, startTime));
            pathPanel.setData(spots, heuristicPlan.visits);

            summaryLabel.setText(
                    "Heuristic: " + heuristicPlan.visits.size() + " spots, Rs. " + (int) heuristicPlan.totalCost
                            + ", " + heuristicPlan.totalMinutes() + " min"
                            + " | Brute-force: " + bruteForcePlan.visits.size() + " spots, Rs. "
                            + (int) bruteForcePlan.totalCost + ", " + bruteForcePlan.totalMinutes() + " min"
            );

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Time and budget must be numeric.");
        }
    }

    private VisitPlan buildHeuristicPlan(List<Spot> dataset, double availableHours, double maxBudget,
                                         Set<String> interests, int startTime) {
        VisitPlan plan = new VisitPlan(startTime);
        Set<Spot> visited = new HashSet<>();

        int tripEnd = startTime + (int) Math.round(availableHours * 60);
        double currentLat = START_LAT;
        double currentLon = START_LON;
        int currentTime = startTime;

        while (true) {
            Visit bestVisit = null;
            double bestScore = -1e18;

            for (Spot spot : dataset) {
                if (visited.contains(spot)) {
                    continue;
                }

                Visit candidate = createVisit(currentLat, currentLon, currentTime, tripEnd,
                        plan.totalCost, maxBudget, interests, spot);

                if (candidate == null) {
                    continue;
                }

                double score = candidate.matchCount * 12.0;
                score -= spot.entryFee / 80.0;
                score -= candidate.travelMinutes / 18.0;
                score += (spot.closeTime - candidate.visitEnd) / 120.0;

                if (score > bestScore) {
                    bestScore = score;
                    bestVisit = candidate;
                }
            }

            if (bestVisit == null) {
                break;
            }

            visited.add(bestVisit.spot);
            plan.visits.add(bestVisit);
            plan.totalCost += bestVisit.spot.entryFee;
            plan.interestMatches += bestVisit.matchCount;

            currentLat = bestVisit.spot.latitude;
            currentLon = bestVisit.spot.longitude;
            currentTime = bestVisit.visitEnd;
        }

        return plan;
    }

    private VisitPlan buildBruteForcePlan(List<Spot> dataset, double availableHours, double maxBudget,
                                          Set<String> interests, int startTime) {
        VisitPlan[] best = new VisitPlan[]{new VisitPlan(startTime)};
        boolean[] used = new boolean[dataset.size()];

        int tripEnd = startTime + (int) Math.round(availableHours * 60);

        dfsBruteForce(dataset, used, START_LAT, START_LON, startTime, tripEnd, maxBudget,
                interests, new VisitPlan(startTime), best);

        return best[0];
    }

    private void dfsBruteForce(List<Spot> dataset, boolean[] used, double currentLat, double currentLon,
                               int currentTime, int tripEnd, double maxBudget, Set<String> interests,
                               VisitPlan currentPlan, VisitPlan[] best) {

        if (isBetter(currentPlan, best[0])) {
            best[0] = new VisitPlan(currentPlan);
        }

        for (int i = 0; i < dataset.size(); i++) {
            if (used[i]) {
                continue;
            }

            Spot spot = dataset.get(i);
            Visit candidate = createVisit(currentLat, currentLon, currentTime, tripEnd,
                    currentPlan.totalCost, maxBudget, interests, spot);

            if (candidate == null) {
                continue;
            }

            used[i] = true;
            currentPlan.visits.add(candidate);
            currentPlan.totalCost += spot.entryFee;
            currentPlan.interestMatches += candidate.matchCount;

            dfsBruteForce(dataset, used, spot.latitude, spot.longitude, candidate.visitEnd,
                    tripEnd, maxBudget, interests, currentPlan, best);

            currentPlan.interestMatches -= candidate.matchCount;
            currentPlan.totalCost -= spot.entryFee;
            currentPlan.visits.remove(currentPlan.visits.size() - 1);
            used[i] = false;
        }
    }

    private boolean isBetter(VisitPlan a, VisitPlan b) {
        if (a.visits.size() != b.visits.size()) {
            return a.visits.size() > b.visits.size();
        }
        if (a.interestMatches != b.interestMatches) {
            return a.interestMatches > b.interestMatches;
        }
        if (Double.compare(a.totalCost, b.totalCost) != 0) {
            return a.totalCost < b.totalCost;
        }
        return a.totalMinutes() < b.totalMinutes();
    }

    private Visit createVisit(double currentLat, double currentLon, int currentTime, int tripEnd,
                              double costAlreadySpent, double maxBudget, Set<String> interests, Spot spot) {

        int travel = estimateTravelMinutes(currentLat, currentLon, spot.latitude, spot.longitude);
        int arrival = currentTime + travel;
        int visitStart = Math.max(arrival, spot.openTime);
        int visitEnd = visitStart + VISIT_DURATION_MIN;

        if (visitEnd > spot.closeTime) {
            return null;
        }
        if (visitEnd > tripEnd) {
            return null;
        }
        if (costAlreadySpent + spot.entryFee > maxBudget) {
            return null;
        }

        int matches = countMatches(spot, interests);
        String reason = buildReason(spot, matches, travel, interests);

        return new Visit(spot, travel, arrival, visitStart, visitEnd, matches, reason);
    }

    private String buildReport(VisitPlan heuristic, VisitPlan bruteForce, Set<String> interests,
                               double availableHours, double maxBudget, int startTime) {
        StringBuilder sb = new StringBuilder();

        sb.append("TOURIST ITINERARY REPORT\n");
        sb.append("============================================================\n");
        sb.append("Start point: Kathmandu city center\n");
        sb.append("Trip start time: ").append(formatTime(startTime)).append("\n");
        sb.append("Available time: ").append(availableHours).append(" hours\n");
        sb.append("Budget: Rs. ").append((int) maxBudget).append("\n");
        sb.append("Interest tags: ").append(interests.isEmpty() ? "(none provided)" : interests).append("\n");
        sb.append("Assumption: each destination visit takes ").append(VISIT_DURATION_MIN).append(" minutes.\n");
        sb.append("Travel time is estimated from spot coordinates.\n\n");

        sb.append("1) HEURISTIC ITINERARY\n");
        sb.append("------------------------------------------------------------\n");
        appendPlan(sb, heuristic);

        sb.append("\n2) BRUTE-FORCE RESULT\n");
        sb.append("------------------------------------------------------------\n");
        appendPlan(sb, bruteForce);

        sb.append("\n3) COMPARISON\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Heuristic spots visited : ").append(heuristic.visits.size()).append("\n");
        sb.append("Brute-force spots visited: ").append(bruteForce.visits.size()).append("\n");
        sb.append("Heuristic total cost    : Rs. ").append((int) heuristic.totalCost).append("\n");
        sb.append("Brute-force total cost  : Rs. ").append((int) bruteForce.totalCost).append("\n");
        sb.append("Heuristic total time    : ").append(heuristic.totalMinutes()).append(" min\n");
        sb.append("Brute-force total time  : ").append(bruteForce.totalMinutes()).append(" min\n");

        if (heuristic.visits.size() == bruteForce.visits.size()) {
            sb.append("Observation: heuristic matched brute-force in number of spots.\n");
        } else {
            sb.append("Observation: heuristic is faster to compute, but may miss the absolute best combination.\n");
        }

        sb.append("Trade-off: brute-force is exact but expensive as dataset size grows; heuristic is near-optimal and more scalable.\n");

        return sb.toString();
    }

    private void appendPlan(StringBuilder sb, VisitPlan plan) {
        if (plan.visits.isEmpty()) {
            sb.append("No feasible itinerary found for the given inputs.\n");
            return;
        }

        for (int i = 0; i < plan.visits.size(); i++) {
            Visit v = plan.visits.get(i);
            sb.append(i + 1).append(". ").append(v.spot.name).append("\n");
            sb.append("   Travel Time : ").append(v.travelMinutes).append(" min\n");
            sb.append("   Visit Window: ").append(formatTime(v.visitStart))
                    .append(" - ").append(formatTime(v.visitEnd)).append("\n");
            sb.append("   Entry Fee   : Rs. ").append(v.spot.entryFee).append("\n");
            sb.append("   Tags        : ").append(v.spot.tags).append("\n");
            sb.append("   Why chosen  : ").append(v.reason).append("\n");
        }

        sb.append("Total Spots   : ").append(plan.visits.size()).append("\n");
        sb.append("Total Cost    : Rs. ").append((int) plan.totalCost).append("\n");
        sb.append("Total Time    : ").append(plan.totalMinutes()).append(" min\n");
        sb.append("Interest Match: ").append(plan.interestMatches).append("\n");
    }

    private static Set<String> parseTags(String text) {
        Set<String> tags = new HashSet<>();
        if (text == null || text.trim().isEmpty()) {
            return tags;
        }

        String[] parts = text.toLowerCase().split("[,;]");
        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) {
                tags.add(cleaned);
            }
        }
        return tags;
    }

    private static int countMatches(Spot spot, Set<String> interests) {
        if (interests.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String tag : interests) {
            if (spot.tags.contains(tag)) {
                count++;
            }
        }
        return count;
    }

    private static int parseTime(String time) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0].trim());
        int minute = Integer.parseInt(parts[1].trim());
        return hour * 60 + minute;
    }

    private static String formatTime(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return String.format("%02d:%02d", h, m);
    }

    private static int estimateTravelMinutes(double lat1, double lon1, double lat2, double lon2) {
        double avgLat = Math.toRadians((lat1 + lat2) / 2.0);
        double dLatKm = (lat2 - lat1) * 111.0;
        double dLonKm = (lon2 - lon1) * 111.0 * Math.cos(avgLat);
        double distanceKm = Math.sqrt(dLatKm * dLatKm + dLonKm * dLonKm);

        double averageSpeedKmh = 18.0;
        int minutes = (int) Math.round((distanceKm / averageSpeedKmh) * 60.0);

        return Math.max(10, minutes);
    }

    static class Spot {
        String name;
        double latitude;
        double longitude;
        int entryFee;
        int openTime;
        int closeTime;
        Set<String> tags;

        Spot(String name, double latitude, double longitude, int entryFee,
             String openTime, String closeTime, String... tags) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.entryFee = entryFee;
            this.openTime = parseTime(openTime);
            this.closeTime = parseTime(closeTime);
            this.tags = new HashSet<>();
            for (String tag : tags) {
                this.tags.add(tag.toLowerCase());
            }
        }
    }

    static class Visit {
        Spot spot;
        int travelMinutes;
        
        int visitStart;
        int visitEnd;
        int matchCount;
        String reason;

        Visit(Spot spot, int travelMinutes, int arrivalTime, int visitStart,
              int visitEnd, int matchCount, String reason) {
            this.spot = spot;
            this.travelMinutes = travelMinutes;
            
            this.visitStart = visitStart;
            this.visitEnd = visitEnd;
            this.matchCount = matchCount;
            this.reason = reason;
        }
    }

    static class VisitPlan {
        List<Visit> visits = new ArrayList<>();
        double totalCost;
        int interestMatches;
        int startTime;

        VisitPlan(int startTime) {
            this.startTime = startTime;
        }

        VisitPlan(VisitPlan other) {
            this.visits = new ArrayList<>(other.visits);
            this.totalCost = other.totalCost;
            this.interestMatches = other.interestMatches;
            this.startTime = other.startTime;
        }

        int totalMinutes() {
            if (visits.isEmpty()) {
                return 0;
            }
            return visits.get(visits.size() - 1).visitEnd - startTime;
        }
    }

    static class PathPanel extends JPanel {
        private List<Spot> allSpots = new ArrayList<>();
        private List<Visit> route = new ArrayList<>();

        PathPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createTitledBorder("Coordinate Path View"));
        }

        void setData(List<Spot> allSpots, List<Visit> route) {
            this.allSpots = new ArrayList<>(allSpots);
            this.route = new ArrayList<>(route);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (allSpots.isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int pad = 50;

            double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
            double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

            for (Spot s : allSpots) {
                minLat = Math.min(minLat, s.latitude);
                maxLat = Math.max(maxLat, s.latitude);
                minLon = Math.min(minLon, s.longitude);
                maxLon = Math.max(maxLon, s.longitude);
            }

            minLat = Math.min(minLat, START_LAT);
            maxLat = Math.max(maxLat, START_LAT);
            minLon = Math.min(minLon, START_LON);
            maxLon = Math.max(maxLon, START_LON);

            Point startPoint = mapPoint(START_LAT, START_LON, minLat, maxLat, minLon, maxLon, w, h, pad);

            g2.setColor(new Color(40, 40, 40));
            g2.fillOval(startPoint.x - 6, startPoint.y - 6, 12, 12);
            g2.drawString("Start", startPoint.x + 8, startPoint.y - 8);

            for (Spot s : allSpots) {
                Point p = mapPoint(s.latitude, s.longitude, minLat, maxLat, minLon, maxLon, w, h, pad);
                g2.setColor(new Color(110, 110, 110));
                g2.fillOval(p.x - 5, p.y - 5, 10, 10);
                g2.drawString(s.name, p.x + 8, p.y - 5);
            }

            if (!route.isEmpty()) {
                g2.setColor(new Color(220, 70, 70));
                g2.setStroke(new BasicStroke(2.5f));

                Point previous = startPoint;

                for (int i = 0; i < route.size(); i++) {
                    Visit visit = route.get(i);
                    Point current = mapPoint(visit.spot.latitude, visit.spot.longitude,
                            minLat, maxLat, minLon, maxLon, w, h, pad);

                    g2.drawLine(previous.x, previous.y, current.x, current.y);
                    g2.fillOval(current.x - 6, current.y - 6, 12, 12);
                    g2.drawString(String.valueOf(i + 1), current.x - 4, current.y - 10);

                    previous = current;
                }
            }
        }

        private Point mapPoint(double lat, double lon, double minLat, double maxLat,
                               double minLon, double maxLon, int width, int height, int pad) {

            double lonRange = Math.max(0.0001, maxLon - minLon);
            double latRange = Math.max(0.0001, maxLat - minLat);

            int x = pad + (int) ((lon - minLon) / lonRange * (width - 2 * pad));
            int y = height - pad - (int) ((lat - minLat) / latRange * (height - 2 * pad));

            return new Point(x, y);
        }
    }

    private static String buildReason(Spot spot, int matches, int travelMinutes, Set<String> interests) {
        StringBuilder sb = new StringBuilder();

        if (matches > 0) {
            sb.append("matched ").append(matches).append(" interest tag(s)");
        } else if (interests.isEmpty()) {
            sb.append("selected by cost/time feasibility");
        } else {
            sb.append("selected despite low tag match because it fit budget and time");
        }

        sb.append(", fee Rs. ").append(spot.entryFee);
        sb.append(", travel ").append(travelMinutes).append(" min");

        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TouristOptimizerApp app = new TouristOptimizerApp();
            app.setVisible(true);
        });
    }
}