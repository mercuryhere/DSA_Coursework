package q5;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class WeatherCollectorApp extends JFrame {

    // Replace this with your real OpenWeatherMap API key for final submission.
    private static final String API_KEY = "PASTE_YOUR_OPENWEATHERMAP_API_KEY_HERE";

    // If API key is not set, the app falls back to demo data so the GUI can still be tested.
    private static final boolean DEMO_MODE = "PASTE_YOUR_OPENWEATHERMAP_API_KEY_HERE".equals(API_KEY);

    private static final String[] CITIES = {
            "Kathmandu", "Pokhara", "Biratnagar", "Nepalgunj", "Dhangadhi"
    };

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private DefaultTableModel tableModel;
    private JButton fetchButton;
    private JTextArea logArea;
    private JLabel statusLabel;
    private LatencyChartPanel chartPanel;

    public WeatherCollectorApp() {
        setTitle("Multi-threaded Weather Data Collector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
    JPanel root = new JPanel(new BorderLayout(10, 10));
    root.setBorder(new EmptyBorder(10, 10, 10, 10));

    JPanel topPanel = new JPanel(new BorderLayout(10, 10));

    JLabel titleLabel = new JLabel("Weather Collector - Sequential vs Parallel");
    titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

    fetchButton = new JButton("Fetch Weather");
    fetchButton.addActionListener(e -> startFetchWorkflow());

    topPanel.add(titleLabel, BorderLayout.WEST);
    topPanel.add(fetchButton, BorderLayout.EAST);

    String[] columns = {"City", "Temperature (°C)", "Humidity (%)", "Pressure (hPa)", "Mode", "Status"};
    tableModel = new DefaultTableModel(columns, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    JTable table = new JTable(tableModel);
    table.setRowHeight(26);
    JScrollPane tableScroll = new JScrollPane(table);

    chartPanel = new LatencyChartPanel();
    chartPanel.setPreferredSize(new Dimension(380, 260));

    logArea = new JTextArea();
    logArea.setEditable(false);
    logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    JScrollPane logScroll = new JScrollPane(logArea);
    logScroll.setPreferredSize(new Dimension(380, 260));

    JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 10));
    bottomPanel.add(chartPanel);
    bottomPanel.add(logScroll);
    bottomPanel.setPreferredSize(new Dimension(1000, 260));

    statusLabel = new JLabel("Ready.");
    statusLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

    JPanel southPanel = new JPanel(new BorderLayout(0, 8));
    southPanel.add(bottomPanel, BorderLayout.CENTER);
    southPanel.add(statusLabel, BorderLayout.SOUTH);

    root.add(topPanel, BorderLayout.NORTH);
    root.add(tableScroll, BorderLayout.CENTER);
    root.add(southPanel, BorderLayout.SOUTH);

    setContentPane(root);
}
    

    private void startFetchWorkflow() {
        fetchButton.setEnabled(false);
        clearTable();
        appendLog("Starting weather collection...");
        appendLog("Mode: " + (DEMO_MODE ? "DEMO (offline test mode)" : "LIVE API mode"));
        statusLabel.setText("Fetching weather data...");
        chartPanel.setLatencies(0, 0);

        new Thread(this::runComparisonWorkflow, "weather-controller").start();
    }

    private void runComparisonWorkflow() {
        try {
            long sequentialStart = System.nanoTime();
            List<WeatherResult> sequentialResults = fetchSequentialWeather();
            long sequentialMs = (System.nanoTime() - sequentialStart) / 1_000_000;

            appendLog("");
            appendLog("Sequential fetch completed in " + sequentialMs + " ms");
            for (WeatherResult result : sequentialResults) {
                appendLog(formatLogLine(result, "Sequential"));
            }

            prepareParallelTable();

            long parallelStart = System.nanoTime();
            List<WeatherResult> parallelResults = fetchParallelWeather();
            long parallelMs = (System.nanoTime() - parallelStart) / 1_000_000;

            appendLog("");
            appendLog("Parallel fetch completed in " + parallelMs + " ms");
            for (WeatherResult result : parallelResults) {
                appendLog(formatLogLine(result, "Parallel"));
            }

            SwingUtilities.invokeLater(() -> {
                chartPanel.setLatencies(sequentialMs, parallelMs);
                statusLabel.setText("Completed. Sequential = " + sequentialMs + " ms, Parallel = " + parallelMs + " ms");
                fetchButton.setEnabled(true);
            });

        } catch (InterruptedException ex) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Error: " + ex.getMessage());
                fetchButton.setEnabled(true);
            });
            appendLog("Fatal error: " + ex.getMessage());
        }
    }

    private List<WeatherResult> fetchSequentialWeather() {
        List<WeatherResult> results = new ArrayList<>();

        for (String city : CITIES) {
            WeatherResult result = fetchWeatherForCity(city);
            result.mode = "Sequential";
            results.add(result);
        }

        return results;
    }

    private List<WeatherResult> fetchParallelWeather() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CITIES.length);
        CountDownLatch latch = new CountDownLatch(CITIES.length);
        Map<String, WeatherResult> resultMap = new ConcurrentHashMap<>();

        for (String city : CITIES) {
            executor.submit(() -> {
                try {
                    WeatherResult result = fetchWeatherForCity(city);
                    result.mode = "Parallel";
                    resultMap.put(city, result);

                    SwingUtilities.invokeLater(() -> updateTableRow(result));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        List<WeatherResult> orderedResults = new ArrayList<>();
        for (String city : CITIES) {
            orderedResults.add(resultMap.get(city));
        }
        return orderedResults;
    }

    private WeatherResult fetchWeatherForCity(String city) {
        try {
            if (DEMO_MODE) {
                return generateDemoWeather(city);
            }

            String encodedCity = URLEncoder.encode(city + ",NP", StandardCharsets.UTF_8);
            String url = "https://api.openweathermap.org/data/2.5/weather?q="
                    + encodedCity
                    + "&appid="
                    + API_KEY
                    + "&units=metric";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String message = extractString(response.body(), "message");
                if (message == null || message.isBlank()) {
                    message = "HTTP " + response.statusCode();
                }
                return WeatherResult.error(city, message);
            }

            String body = response.body();

            double temperature = extractDouble(body, "temp");
            int humidity = (int) Math.round(extractDouble(body, "humidity"));
            int pressure = (int) Math.round(extractDouble(body, "pressure"));

            return WeatherResult.success(city, temperature, humidity, pressure);

        } catch (java.io.IOException | IllegalArgumentException ex) {
                return WeatherResult.error(city, ex.getMessage());
}               catch (InterruptedException ex) {
             Thread.currentThread().interrupt();
             return WeatherResult.error(city, "Interrupted while fetching weather");
}
}
        
    

    private WeatherResult generateDemoWeather(String city) {
        try {
            Thread.sleep(500 + Math.abs(city.hashCode()) % 700);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Random random = new Random(city.hashCode());
        double temperature = 16 + random.nextDouble() * 14;
        int humidity = 45 + random.nextInt(40);
        int pressure = 990 + random.nextInt(30);

        return WeatherResult.success(city, temperature, humidity, pressure);
    }

    private static double extractDouble(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        throw new IllegalArgumentException("Missing field: " + key);
    }

    private static String extractString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private void prepareParallelTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (String city : CITIES) {
                tableModel.addRow(new Object[]{city, "-", "-", "-", "Parallel", "Waiting..."});
            }
        });
    }

    private void clearTable() {
        tableModel.setRowCount(0);
    }

    private void updateTableRow(WeatherResult result) {
        int row = findCityRow(result.city);
        if (row == -1) {
            tableModel.addRow(new Object[]{
                    result.city,
                    formatTemperature(result),
                    formatHumidity(result),
                    formatPressure(result),
                    result.mode,
                    result.status
            });
        } else {
            tableModel.setValueAt(formatTemperature(result), row, 1);
            tableModel.setValueAt(formatHumidity(result), row, 2);
            tableModel.setValueAt(formatPressure(result), row, 3);
            tableModel.setValueAt(result.mode, row, 4);
            tableModel.setValueAt(result.status, row, 5);
        }
    }

    private int findCityRow(String city) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object value = tableModel.getValueAt(i, 0);
            if (city.equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private String formatTemperature(WeatherResult result) {
        return result.success ? String.format("%.2f", result.temperature) : "-";
    }

    private String formatHumidity(WeatherResult result) {
        return result.success ? String.valueOf(result.humidity) : "-";
    }

    private String formatPressure(WeatherResult result) {
        return result.success ? String.valueOf(result.pressure) : "-";
    }

    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private String formatLogLine(WeatherResult result, String mode) {
        if (result.success) {
            return String.format("%-10s [%s] Temp=%.2f°C, Humidity=%d%%, Pressure=%d hPa",
                    result.city, mode, result.temperature, result.humidity, result.pressure);
        }
        return String.format("%-10s [%s] ERROR: %s", result.city, mode, result.status);
    }

    private static class WeatherResult {
        String city;
        double temperature;
        int humidity;
        int pressure;
        boolean success;
        String status;
        String mode;

        static WeatherResult success(String city, double temperature, int humidity, int pressure) {
            WeatherResult result = new WeatherResult();
            result.city = city;
            result.temperature = temperature;
            result.humidity = humidity;
            result.pressure = pressure;
            result.success = true;
            result.status = "OK";
            return result;
        }

        static WeatherResult error(String city, String message) {
            WeatherResult result = new WeatherResult();
            result.city = city;
            result.success = false;
            result.status = message == null ? "Unknown error" : message;
            return result;
        }
    }

    private static class LatencyChartPanel extends JPanel {
        private long sequentialMs;
        private long parallelMs;

        LatencyChartPanel() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createTitledBorder("Latency Comparison (ms)"));
        }

        void setLatencies(long sequentialMs, long parallelMs) {
            this.sequentialMs = sequentialMs;
            this.parallelMs = parallelMs;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int left = 60;
            int right = 30;
            int top = 40;
            int bottom = 60;

            g2.setColor(Color.DARK_GRAY);
            g2.drawLine(left, height - bottom, width - right, height - bottom);
            g2.drawLine(left, height - bottom, left, top);

            long maxValue = Math.max(1, Math.max(sequentialMs, parallelMs));
            int chartHeight = height - top - bottom;
            int barWidth = 120;

            int seqX = left + 60;
            int parX = seqX + 180;

            int seqBarHeight = (int) ((sequentialMs * 1.0 / maxValue) * (chartHeight - 20));
            int parBarHeight = (int) ((parallelMs * 1.0 / maxValue) * (chartHeight - 20));

            g2.setColor(new Color(90, 140, 220));
            g2.fillRect(seqX, height - bottom - seqBarHeight, barWidth, seqBarHeight);

            g2.setColor(new Color(90, 190, 120));
            g2.fillRect(parX, height - bottom - parBarHeight, barWidth, parBarHeight);

            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.5f));

            g2.drawString("Sequential", seqX + 20, height - 25);
            g2.drawString("Parallel", parX + 28, height - 25);

            g2.drawString(sequentialMs + " ms", seqX + 28, height - bottom - seqBarHeight - 8);
            g2.drawString(parallelMs + " ms", parX + 35, height - bottom - parBarHeight - 8);

            g2.drawString("Time", 18, top - 10);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WeatherCollectorApp app = new WeatherCollectorApp();
            app.setVisible(true);
        });
    }
}