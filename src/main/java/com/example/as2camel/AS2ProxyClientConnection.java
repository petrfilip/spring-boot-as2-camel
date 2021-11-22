/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.as2camel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import org.apache.camel.component.as2.api.AS2ClientConnection;
import org.apache.camel.component.as2.api.io.AS2BHttpClientConnection;
import org.apache.camel.component.as2.api.protocol.RequestAS2;
import org.apache.camel.component.as2.api.protocol.RequestMDN;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestDate;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.Args;

public class AS2ProxyClientConnection extends AS2ClientConnection {

  private HttpHost targetHost;
  private final HttpProcessor httpProcessor;
  private final DefaultBHttpClientConnection httpConnection;
  private String as2Version;
  private String userAgent;
  private String clientFqdn;

  public AS2ProxyClientConnection(String as2Version, String userAgent, String clientFqdn,
    String targetHostName, Integer targetPortNumber,
    String proxyHostName, Integer proxyPortNumber) throws IOException {

    super(as2Version, userAgent, clientFqdn, proxyHostName, proxyPortNumber);

    this.as2Version = Args.notNull(as2Version, "as2Version");
    this.userAgent = Args.notNull(userAgent, "userAgent");
    this.clientFqdn = Args.notNull(clientFqdn, "clientFqdn");
    this.targetHost = new HttpHost(Args.notNull(proxyHostName, "proxyHostName"), Args.notNull(proxyPortNumber, "proxyPortNumber"));

    // Build Processor
    httpProcessor = HttpProcessorBuilder.create()
      .add(new RequestAS2(as2Version, clientFqdn))
      .add(new RequestMDN())
      .add(new RequestTargetHost())
      .add(new RequestUserAgent(this.userAgent))
      .add(new RequestDate())
      .add(new RequestContent(true))
      .add(new RequestConnControl())
      .add(new RequestExpectContinue(true)).build();

    // Create Socket
    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostName, proxyPortNumber));
    Socket socketProxy = new Socket(proxy);
    InetSocketAddress address = InetSocketAddress.createUnresolved(targetHostName, targetPortNumber); // create a socket without resolving the target host to IP
    socketProxy.connect(address);

    // Create Connection
    httpConnection = new AS2BHttpClientConnection(8 * 1024);
    httpConnection.bind(socketProxy);
  }


  public String getAs2Version() {
    return as2Version;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getClientFqdn() {
    return clientFqdn;
  }

  public HttpResponse send(HttpRequest request, HttpCoreContext httpContext) throws HttpException, IOException {

    httpContext.setTargetHost(targetHost);

    // Execute Request
    HttpRequestExecutor httpExecutor = new HttpRequestExecutor();
    httpExecutor.preProcess(request, httpProcessor, httpContext);
    HttpResponse response = httpExecutor.execute(request, httpConnection, httpContext);
    httpExecutor.postProcess(response, httpProcessor, httpContext);

    return response;
  }

}
