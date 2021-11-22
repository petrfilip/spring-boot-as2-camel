package com.example.as2camel;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.helger.security.certificate.CertificateHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.bind.DatatypeConverter;
import org.apache.camel.component.as2.api.AS2ClientConnection;
import org.apache.camel.component.as2.api.AS2ClientManager;
import org.apache.camel.component.as2.api.AS2CompressionAlgorithm;
import org.apache.camel.component.as2.api.AS2EncryptionAlgorithm;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.http.HttpConnection;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

@Service
public class SenderTestAs2 {

  private static final Logger LOGGER = LoggerFactory.getLogger(SenderTestAs2.class);
  private static final String AS2_VERSION = "1.2";
  private static final String AS2_USER_AGENT = "Camel AS2 Client Endpoint";
  private static final String AS2_CLIENT_FQDN = "camel.apache.org";

  /**
   * certificates
   */
  @Value("${milos.certificate.signing}")
  private Resource certificateSigningResource;
  @Value("${milos.certificate.private}")
  private Resource certificatePrivateResource;
  @Value("${milos.certificate.encryption}")
  private Resource certificateEncryptionResource;

  @Value("${milos.message.content}")
  private Resource messageContent;

  /**
   * endpoint
   */
  @Value("${milos.endpoint.hostname}")
  private String targetHostName;
  @Value("${milos.endpoint.port}")
  private Integer targetPort;

  /**
   * proxy
   */
  @Value("${milos.proxy.hostname}")
  private String proxyHostName;
  @Value("${milos.proxy.port}")
  private Integer proxyPort;


  @PostConstruct
  public void main() {

    // validate certificates
    try {
      getEncryptionCertificateChain();
      getSigningPrivateKey();
      getSigningCertificateChain();
    } catch (Exception e) {
      throw new RuntimeException("Invalidate certificates", e);
    }

    // send message
    try {
      sendMessage(asString(messageContent));
    } catch (Exception e) {
      throw new RuntimeException("Unable to send message", e);
    }
  }


  private AS2ClientConnection prepareAs2Connection() throws IOException {
    LOGGER.info("AS2 MILOS INFO :: targetHostName: {} | targetPort: {} | proxyHostName: {} | proxyPort: {}", targetHostName, targetPort, proxyHostName, proxyPort);
    return new AS2ProxyClientConnection(AS2_VERSION, AS2_USER_AGENT, AS2_CLIENT_FQDN, targetHostName, targetPort, proxyHostName, proxyPort);
  }

  private HttpCoreContext sendMessage(String payload, AS2ClientConnection as2ClientConnection) throws Exception {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

    //HDS 4.1 Create AS2ClientManager from connection
    AS2ClientManager clientManager = new AS2ClientManager(as2ClientConnection);

    //HDS 4.2. Call send method on AS2ClientManager with parameters from as2Message
    String uri = "/PKP-AS2-ESB-UnicornAS2Receiver";
    String from = "";
    String subject = "";
    // String as2From = "N4G";
    String as2From = "UnicornTest";
    String as2To = "PRISMA-TEST-AS2-ID";
    AS2MessageStructure msgStructure = AS2MessageStructure.SIGNED_ENCRYPTED;
    ContentType ediMsgContentType = ContentType.create("application/edifact"); // "application/edi-x12";
    AS2SignatureAlgorithm signAlg = AS2SignatureAlgorithm.SHA256WITHRSA;
    String[] micAlg = {"SHA256"};
    AS2CompressionAlgorithm compressAlg = null;
    AS2EncryptionAlgorithm encryptAlg = AS2EncryptionAlgorithm.DES_EDE3_CBC;
    // String dispositionNotificationTo = "N4G";
    String dispositionNotificationTo = "UnicornTest";
    return clientManager
        .send(payload, uri, subject, from, as2From, as2To, msgStructure, ediMsgContentType, null, signAlg,
            getSigningCertificateChain(), getSigningPrivateKey(), compressAlg, dispositionNotificationTo, micAlg, encryptAlg, getEncryptionCertificateChain());
  }

  public String sendMessage(String content) throws Exception {
    LOGGER.info("START SENDING");

    HttpConnection conn = null;
    try {
      AS2ClientConnection as2ClientConnection = prepareAs2Connection();
      HttpCoreContext context = sendMessage(content, as2ClientConnection);
      conn = context.getConnection();
      HttpResponse response = context.getResponse();
      StatusLine status = response.getStatusLine();
      String responseContent = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), UTF_8)).lines().collect(Collectors.joining("\n"));
      return responseContent;
    } finally {
      if (conn != null) {
        conn.close();
      }
    }

  }

  private X509Certificate[] getEncryptionCertificateChain() throws CertificateException {
    X509Certificate[] certChain = new X509Certificate[1];
    String cert = asString(certificateEncryptionResource);
    certChain[0] = CertificateHelper.convertStringToCertficate(cert);
    return certChain;
  }

  private Certificate[] getSigningCertificateChain() throws CertificateException {
    Certificate[] certChain = new Certificate[1];
    // n4g.pem.cert
    String cert = asString(certificateSigningResource);

    certChain[0] = CertificateHelper.convertStringToCertficate(cert);
    return certChain;
  }

  private PrivateKey getSigningPrivateKey() {
    // AS2 TEST cert N4g - n4g-decrypted-pkcs8.key
    String key = asString(certificatePrivateResource);

    try {
      return getPrivateKey(key);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static PrivateKey getPrivateKey(String privateKeyString) throws NoSuchAlgorithmException, InvalidKeySpecException {
    String cleanPrivateKey = privateKeyString.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
    byte[] encoded = DatatypeConverter.parseBase64Binary(cleanPrivateKey);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(keySpec);
  }

  public static String asString(Resource resource) {
    try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
      return FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

