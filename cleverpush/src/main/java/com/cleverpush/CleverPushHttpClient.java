package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.os.Handler;
import android.os.Looper;

import com.cleverpush.util.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

import org.json.JSONObject;

public class CleverPushHttpClient {
  public interface ResponseHandler {
    void onSuccess(String response);

    void onFailure(int statusCode, String response, Throwable throwable);
  }

  public static String BASE_URL = "https://api.cleverpush.com";
  private static final int TIMEOUT = 120_000;
  private static final int MAX_RETRIES = 3;
  private static final int INITIAL_RETRY_DELAY = 1000;
  private static final int MULTIPLY_FACTOR = 2;

  public static void postWithRetry(final String url, final JSONObject jsonBody, final ResponseHandler responseHandler) {
    postWithRetry(url, jsonBody, responseHandler, 0, INITIAL_RETRY_DELAY);
  }

  private static void postWithRetry(final String url, final JSONObject jsonBody, final ResponseHandler responseHandler, final int retryCount, final int retryDelay) {
    post(url, jsonBody, new ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        if (responseHandler != null) {
          responseHandler.onSuccess(response);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (retryCount < MAX_RETRIES) {
          int nextRetryDelay = retryDelay * MULTIPLY_FACTOR;
          new Handler(Looper.getMainLooper()).postDelayed(() -> postWithRetry(url, jsonBody, responseHandler, retryCount + 1, nextRetryDelay), retryDelay);
        } else {
          if (responseHandler != null) {
            responseHandler.onFailure(statusCode, response, throwable);
          }
        }
      }
    });
  }

  public static void getWithRetry(final String url, final ResponseHandler responseHandler) {
    getWithRetry(url, responseHandler, 0, INITIAL_RETRY_DELAY);
  }

  private static void getWithRetry(final String url, final ResponseHandler responseHandler, final int retryCount, final int retryDelay) {
    get(url, new ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        if (responseHandler != null) {
          responseHandler.onSuccess(response);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (retryCount < MAX_RETRIES) {
          int nextRetryDelay = retryDelay * MULTIPLY_FACTOR;
          new Handler(Looper.getMainLooper()).postDelayed(() -> getWithRetry(url, responseHandler, retryCount + 1, nextRetryDelay), retryDelay);
        } else {
          if (responseHandler != null) {
            responseHandler.onFailure(statusCode, response, throwable);
          }
        }
      }
    });
  }

  public static void post(final String url, final JSONObject jsonBody, final ResponseHandler responseHandler) {
    try {
      String authorizerToken = CleverPush.getInstance(CleverPush.context).getAuthorizerToken();
      if (authorizerToken != null && authorizerToken.length() > 0) {
        jsonBody.put("authorizationToken", authorizerToken);
      }
      new Thread(() -> makeRequest(url, "POST", jsonBody, responseHandler)).start();
    } catch (Exception e) {
      Logger.e(LOG_TAG, "CleverPushHttpClient: Error in post request", e);
    }
  }

  public static void get(String url, final ResponseHandler responseHandler) {
    String authorizerToken = CleverPush.getInstance(CleverPush.context).getAuthorizerToken();
    if (authorizerToken != null && authorizerToken.length() > 0) {
      if (url.contains("?")) {
        url += "&authorizationToken=" + authorizerToken;
      } else {
        url += "?authorizationToken=" + authorizerToken;
      }
    }
    String finalUrl = url;
    new Thread(() -> makeRequest(finalUrl, null, null, responseHandler)).start();
  }

  private static void makeRequest(String url, String method, JSONObject jsonBody, ResponseHandler responseHandler) {
    HttpURLConnection con = null;
    int httpResponse = -1;
    String json = null;

    Logger.d(LOG_TAG,
        "[HTTP] " + (method == null ? "GET" : method) + ": " + url + (jsonBody != null ? (" " + jsonBody.toString()) :
            ""));

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
          } catch (Exception ignored) {
          }
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

        byte[] sendBytes = strJsonBody.getBytes(StandardCharsets.UTF_8);
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
    } catch (Throwable throwable) {
      Logger.e(LOG_TAG, "CleverPushHttpClient: Error during HTTP request", throwable);
      if (responseHandler != null) {
        responseHandler.onFailure(httpResponse, null, throwable);
      }
    } finally {
      if (con != null) {
        con.disconnect();
      }
    }
  }
}
