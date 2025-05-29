import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import org.json.JSONObject;

public class WeatherForecastApp {
    private static final String WEATHER_API_KEY = "ce904d6585a59a7dc38a0f0cfe36e2cb";

    private static Color lightBg = Color.decode("#f2f6fc");
    private static Color lightPanelBg = Color.WHITE;
    private static Color lightText = Color.decode("#2e3c5d");
    private static Color lightButtonBg = new Color(58, 134, 255);
    private static Color lightButtonFg = Color.WHITE;

    private static Color darkBg = Color.decode("#1e1e2f");
    private static Color darkPanelBg = Color.decode("#2c2c3f");
    private static Color darkText = Color.decode("#cbd5e1");
    private static Color darkButtonBg = new Color(80, 110, 230);
    private static Color darkButtonFg = Color.WHITE;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherForecastApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("☀️ Weather Forecast App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 420);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(lightBg);

        JLabel heading = new JLabel("Weather Forecast", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 28));
        heading.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        heading.setForeground(lightText);
        mainPanel.add(heading, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(lightPanelBg);
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 16);

        JLabel cityPrompt = new JLabel("Enter City (or leave blank to auto-detect):");
        cityPrompt.setFont(labelFont);
        cityPrompt.setForeground(lightText);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        contentPanel.add(cityPrompt, gbc);

        JTextField cityField = new JTextField();
        cityField.setFont(labelFont);
        gbc.gridy = 1;
        contentPanel.add(cityField, gbc);

        JButton fetchButton = new JButton("Get Weather");
        fetchButton.setFont(labelFont);
        fetchButton.setBackground(lightButtonBg);
        fetchButton.setForeground(lightButtonFg);
        fetchButton.setFocusPainted(false);
        gbc.gridy = 2;
        contentPanel.add(fetchButton, gbc);

        JLabel locationLabel = new JLabel("Location: Detecting...");
        JLabel tempLabel = new JLabel("Temperature: ");
        JLabel conditionLabel = new JLabel("Condition: ");
        JLabel humidityLabel = new JLabel("Humidity: ");
        JLabel windLabel = new JLabel("Wind Speed: ");

        JLabel[] infoLabels = {locationLabel, tempLabel, conditionLabel, humidityLabel, windLabel};
        int row = 3;
        for (JLabel label : infoLabels) {
            label.setFont(labelFont);
            label.setForeground(lightText);
            gbc.gridy = row++;
            contentPanel.add(label, gbc);
        }

        // Menu Bar with View -> Dark Mode checkbox
        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem darkModeMenuItem = new JCheckBoxMenuItem("Dark Mode");
        viewMenu.add(darkModeMenuItem);
        menuBar.add(viewMenu);
        frame.setJMenuBar(menuBar);

        // Action listener for fetchButton
        fetchButton.addActionListener(e -> {
            String city = cityField.getText().trim();
            if (city.isEmpty()) {
                city = detectCityByIP();
            }
            locationLabel.setText("Location: " + city);
            JSONObject weatherData = getWeatherData(city);
            if (weatherData != null) {
                JSONObject main = weatherData.getJSONObject("main");
                JSONObject wind = weatherData.getJSONObject("wind");
                String condition = weatherData.getJSONArray("weather").getJSONObject(0).getString("description");
                tempLabel.setText("Temperature: " + main.getDouble("temp") + " \u00B0C");
                conditionLabel.setText("Condition: " + capitalize(condition));
                humidityLabel.setText("Humidity: " + main.getInt("humidity") + "%");
                windLabel.setText("Wind Speed: " + wind.getDouble("speed") + " m/s");
            } else {
                JOptionPane.showMessageDialog(frame, "Could not retrieve weather data.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Dark mode toggle from menu item
        darkModeMenuItem.addActionListener(e -> {
            boolean darkMode = darkModeMenuItem.isSelected();
            if (darkMode) {
                mainPanel.setBackground(darkBg);
                contentPanel.setBackground(darkPanelBg);
                heading.setForeground(darkText);
                cityPrompt.setForeground(darkText);
                for (JLabel label : infoLabels) {
                    label.setForeground(darkText);
                }
                cityField.setBackground(darkPanelBg);
                cityField.setForeground(darkText);
                fetchButton.setBackground(darkButtonBg);
                fetchButton.setForeground(darkButtonFg);
                // Menu item colors
                viewMenu.setForeground(darkText);
                darkModeMenuItem.setBackground(darkPanelBg);
                darkModeMenuItem.setForeground(darkText);
            } else {
                mainPanel.setBackground(lightBg);
                contentPanel.setBackground(lightPanelBg);
                heading.setForeground(lightText);
                cityPrompt.setForeground(lightText);
                for (JLabel label : infoLabels) {
                    label.setForeground(lightText);
                }
                cityField.setBackground(Color.WHITE);
                cityField.setForeground(Color.BLACK);
                fetchButton.setBackground(lightButtonBg);
                fetchButton.setForeground(lightButtonFg);
                // Menu item colors
                viewMenu.setForeground(Color.BLACK);
                darkModeMenuItem.setBackground(null);
                darkModeMenuItem.setForeground(null);
            }
        });

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }

    private static String detectCityByIP() {
        try {
            URL url = new URL("http://ip-api.com/json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
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
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            return new JSONObject(content.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
