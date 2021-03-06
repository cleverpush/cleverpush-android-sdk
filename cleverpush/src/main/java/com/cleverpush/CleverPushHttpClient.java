package com.cleverpush;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;

import org.json.JSONObject;

public class CleverPushHttpClient {
    public interface ResponseHandler {
        void onSuccess(String response);
        void onFailure(int statusCode, String response, Throwable throwable);
    }

    public static String BASE_URL = "https://api.cleverpush.com";
    private static final int TIMEOUT = 120000;

    public static void post(final String url, final JSONObject jsonBody, final ResponseHandler responseHandler) {
        new Thread(() -> makeRequest(url, "POST", jsonBody, responseHandler)).start();
    }

    public static void get(final String url, final ResponseHandler responseHandler) {
        new Thread(() -> makeRequest(url, null, null, responseHandler)).start();
    }

    private static void makeRequest(String url, String method, JSONObject jsonBody, ResponseHandler responseHandler) {
        HttpURLConnection con = null;
        int httpResponse = -1;
        String json = null;

        try {
            con = (HttpURLConnection) new URL(BASE_URL + url).openConnection();
            con.setUseCaches(false);
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(TIMEOUT);

            if (jsonBody != null) {
                con.setDoInput(true);
            }

            if (method != null) {
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                String language = null;
                if (jsonBody != null) {
                    try {
                        language = jsonBody.getString("language");
                    } catch (Exception ignored) {}
                }
                if (language == null) {
                    language = Locale.getDefault().getLanguage();
                }
                con.setRequestProperty("Accept-Language", language);

                con.setRequestProperty("User-Agent", "CleverPush Android SDK " + CleverPush.SDK_VERSION);
                con.setRequestMethod(method);
                con.setDoOutput(true);
            }

            if (jsonBody != null) {
                String strJsonBody = jsonBody.toString();

                byte[] sendBytes = strJsonBody.getBytes("UTF-8");
                con.setFixedLengthStreamingMode(sendBytes.length);

                OutputStream outputStream = con.getOutputStream();
                outputStream.write(sendBytes);
            }

            httpResponse = con.getResponseCode();

            InputStream inputStream;
            Scanner scanner;
            if (httpResponse == HttpURLConnection.HTTP_OK || httpResponse == HttpURLConnection.HTTP_CREATED) {
                inputStream = con.getInputStream();
                scanner = new Scanner(inputStream, "UTF-8");
                json = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                scanner.close();

                if (responseHandler != null) {
                    responseHandler.onSuccess(json);
                }
            } else {
                inputStream = con.getErrorStream();
                if (inputStream == null) {
                    inputStream = con.getInputStream();
                }

                if (inputStream != null) {
                    scanner = new Scanner(inputStream, "UTF-8");
                    json = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                    scanner.close();
                }

                if (responseHandler != null) {
                    responseHandler.onFailure(httpResponse, json, null);
                }
            }
        } catch (Throwable t) {
            if (responseHandler != null) {
                responseHandler.onFailure(httpResponse, null, t);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }
}
