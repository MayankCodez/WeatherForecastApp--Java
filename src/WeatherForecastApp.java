import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class WeatherForecastApp {
    private static final String WEATHER_API_KEY = "ce904d6585a59a7dc38a0f0cfe36e2cb";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherForecastApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Weather Forecast");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 550);
        frame.setLocationRelativeTo(null);

        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setLayout(new BorderLayout(0, 15));

        // Header label
        JLabel header = new JLabel("Weather Forecast", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 28));

        mainPanel.add(header, BorderLayout.NORTH);

        // Center panel for inputs and current weather
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JTextField cityField = new JTextField();
        cityField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        cityField.setToolTipText("Enter city name or leave empty for auto-detection");

        JButton getWeatherButton = new JButton("Get Weather");
        getWeatherButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        getWeatherButton.setBackground(new Color(33, 150, 243));
        getWeatherButton.setForeground(Color.WHITE);
        getWeatherButton.setFocusPainted(false);
        getWeatherButton.setBorder(new RoundedBorder(10));

        inputPanel.add(cityField, BorderLayout.CENTER);
        inputPanel.add(getWeatherButton, BorderLayout.EAST);

        centerPanel.add(inputPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Current weather info panel (clean grid)
        JPanel currentWeatherPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        currentWeatherPanel.setOpaque(false);

        JLabel locationLabel = createInfoLabel("Location: ");
        JLabel tempLabel = createInfoLabel("Temperature: ");
        JLabel conditionLabel = createInfoLabel("Condition: ");
        JLabel humidityLabel = createInfoLabel("Humidity: ");

        currentWeatherPanel.add(locationLabel);
        currentWeatherPanel.add(tempLabel);
        currentWeatherPanel.add(conditionLabel);
        currentWeatherPanel.add(humidityLabel);

        centerPanel.add(currentWeatherPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        // Forecast panel with cards
        JPanel forecastPanel = new JPanel();
        forecastPanel.setLayout(new GridLayout(1, 5, 15, 0));
        forecastPanel.setOpaque(false);

        JLabel[] forecastCards = new JLabel[5];
        for (int i = 0; i < 5; i++) {
            JLabel card = new JLabel("", SwingConstants.CENTER);
            card.setVerticalTextPosition(SwingConstants.BOTTOM);
            card.setHorizontalTextPosition(SwingConstants.CENTER);
            card.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            card.setBorder(new RoundedBorder(15));
            card.setOpaque(true);
            card.setBackground(new Color(224, 242, 254));
            card.setPreferredSize(new Dimension(110, 150));
            forecastCards[i] = card;
            forecastPanel.add(card);
        }

        centerPanel.add(forecastPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Dark Mode toggle menu
        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem darkModeToggle = new JCheckBoxMenuItem("Dark Mode");
        viewMenu.add(darkModeToggle);
        menuBar.add(viewMenu);
        frame.setJMenuBar(menuBar);

        frame.add(mainPanel);
        frame.setVisible(true);

        // Button action listener
        getWeatherButton.addActionListener(e -> {
            String city = cityField.getText().trim();
            if (city.isEmpty()) {
                city = detectCityByIP();
            }
            locationLabel.setText("Location: " + city);

            JSONObject weatherData = getWeatherData(city);
            if (weatherData != null) {
                JSONObject main = weatherData.getJSONObject("main");
                String condition = weatherData.getJSONArray("weather").getJSONObject(0).getString("description");
                tempLabel.setText(String.format("Temperature: %.1f °C", main.getDouble("temp")));
                conditionLabel.setText("Condition: " + capitalize(condition));
                humidityLabel.setText("Humidity: " + main.getInt("humidity") + "%");
            } else {
                JOptionPane.showMessageDialog(frame, "Could not retrieve weather data.", "Error", JOptionPane.ERROR_MESSAGE);
            }

            JSONObject forecastData = getFiveDayForecast(city);
            if (forecastData != null) {
                updateForecastCards(forecastData, forecastCards);
            }
        });

        // Dark mode toggle
        darkModeToggle.addActionListener(e -> {
            boolean dark = darkModeToggle.isSelected();
            Color bg = dark ? new Color(30, 30, 30) : Color.WHITE;
            Color fg = dark ? Color.WHITE : Color.DARK_GRAY;
            Color cardBg = dark ? new Color(60, 63, 65) : new Color(224, 242, 254);

            mainPanel.setBackground(bg);
            centerPanel.setBackground(bg);
            currentWeatherPanel.setBackground(bg);
            forecastPanel.setBackground(bg);
            for (JLabel card : forecastCards) {
                card.setBackground(cardBg);
                card.setForeground(fg);
                card.setBorder(new RoundedBorder(15, dark ? new Color(100, 100, 100) : new Color(200, 200, 200)));
            }

            locationLabel.setForeground(fg);
            tempLabel.setForeground(fg);
            conditionLabel.setForeground(fg);
            humidityLabel.setForeground(fg);
            header.setForeground(dark ? Color.CYAN : new Color(33, 150, 243));
            cityField.setBackground(dark ? new Color(80, 80, 80) : Color.WHITE);
            cityField.setForeground(fg);
            getWeatherButton.setBackground(dark ? new Color(70, 130, 180) : new Color(33, 150, 243));
            getWeatherButton.setForeground(Color.WHITE);
        });
    }

    private static JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        label.setForeground(Color.DARK_GRAY);
        return label;
    }

    private static String detectCityByIP() {
        try {
            URL url = new URL("http://ip-api.com/json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) content.append(inputLine);
            in.close();
            JSONObject response = new JSONObject(content.toString());
            return response.getString("city");
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    private static JSONObject getWeatherData(String city) {
        try {
            String urlString = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&appid=%s",
                    URLEncoder.encode(city, "UTF-8"), WEATHER_API_KEY);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) content.append(inputLine);
            in.close();
            return new JSONObject(content.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject getFiveDayForecast(String city) {
        try {
            String urlString = String.format(
                    "https://api.openweathermap.org/data/2.5/forecast?q=%s&units=metric&appid=%s",
                    URLEncoder.encode(city, "UTF-8"), WEATHER_API_KEY);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) content.append(inputLine);
            in.close();
            return new JSONObject(content.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void updateForecastCards(JSONObject forecastData, JLabel[] cards) {
        try {
            JSONArray list = forecastData.getJSONArray("list");
            Map<String, Double> tempSum = new LinkedHashMap<>();
            Map<String, Integer> count = new LinkedHashMap<>();
            Map<String, String> conditions = new LinkedHashMap<>();

            for (int i = 0; i < list.length(); i++) {
                JSONObject entry = list.getJSONObject(i);
                String dt_txt = entry.getString("dt_txt");
                String day = dt_txt.split(" ")[0];

                double temp = entry.getJSONObject("main").getDouble("temp");
                String condition = entry.getJSONArray("weather").getJSONObject(0).getString("main");

                tempSum.put(day, tempSum.getOrDefault(day, 0.0) + temp);
                count.put(day, count.getOrDefault(day, 0) + 1);
                if (!conditions.containsKey(day)) {
                    conditions.put(day, condition);
                }
            }

            int i = 0;
            for (String day : tempSum.keySet()) {
                if (i >= 5) break;
                double avgTemp = tempSum.get(day) / count.get(day);
                String cond = conditions.get(day);
                String dateDisplay = day.substring(5); // MM-DD
                cards[i].setText("<html><b>" + dateDisplay + "</b><br>" +
                        String.format("%.1f", avgTemp) + " °C<br>" +
                        cond + "</html>");
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    // Custom rounded border for buttons and cards
    static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color borderColor;

        public RoundedBorder(int radius) {
            this(radius, Color.LIGHT_GRAY);
        }

        public RoundedBorder(int radius, Color borderColor) {
            this.radius = radius;
            this.borderColor = borderColor;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(borderColor);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = radius / 2;
            return insets;
        }
    }
}
