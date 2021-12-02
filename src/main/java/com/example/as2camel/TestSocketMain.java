package com.example.as2camel;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.IOUtils;

public class TestSocketMain {

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


    SSLContext sc = null;
    try {
      sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (GeneralSecurityException e) {
    }





    final String REQUEST = "GET / HTTP/1.1\r\n" +
        "Host: slavia-radonice.cz\r\n" +
        "Connection: close\r\n" +
        "\r\n";


    // Create Socket
    Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress("localhost", 8080));
    Socket socketProxy = new Socket(proxy);
    socketProxy.setSoTimeout(10000);
    InetSocketAddress address = InetSocketAddress.createUnresolved("slavia-radonice.cz", 443); // create a socket without resolving the target host to IP
    socketProxy.connect(address);

    //

    SSLSocketFactory sslSocketFactory = (SSLSocketFactory) sc.getSocketFactory();
    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socketProxy, address.getHostName(), address.getPort(), true);
    sslSocket.startHandshake();


    sslSocket.getOutputStream().write(REQUEST.getBytes(StandardCharsets.UTF_8));

    InputStream inputStream = sslSocket.getInputStream();
    byte[] bytes = IOUtils.toByteArray(inputStream);

    sslSocket.close();

    System.out.println(new String(bytes));

  }

}
