package com.example.as2camel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class TestMain {

  public static void main(String[] args) throws IOException {



    TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }
          public void checkClientTrusted(
              java.security.cert.X509Certificate[] certs, String authType) {
          }
          public void checkServerTrusted(
              java.security.cert.X509Certificate[] certs, String authType) {
          }
        }
    };


    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (GeneralSecurityException e) {
    }














    final String REQUEST = "GET / HTTP/1.1\r\n" +
        "Host: baido.com\r\n" +
        "Connection: close\r\n" +
        "\r\n";


    Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress("localhost", 8080));

    URL url = new URL("https://www.slavia-radonice.cz/");
    HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
    conn.setDoOutput(true);
    conn.getOutputStream().write(REQUEST.getBytes(StandardCharsets.UTF_8));
    conn.getOutputStream().flush();

    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String inputLine;
    StringBuilder s = new StringBuilder();
    while ((inputLine = in.readLine()) != null) {
      s.append(inputLine);
    }
    in.close();

    System.out.println(s);

  }

}
