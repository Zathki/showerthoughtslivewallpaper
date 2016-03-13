package ca.thekidd.showerthoughtslivewallpaper.utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JSONUtils {
    public static JSONObject getJSON(String url) {
        URL website;
        try {
            website = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) website.openConnection();
            connection.setRequestProperty("charset", "utf-8");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream()));

            StringBuilder result = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                result.append(inputLine);

            in.close();
            return new JSONObject(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
