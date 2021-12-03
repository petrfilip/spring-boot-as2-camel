# Spring Boot + Apache Camel AS2 example (with added proxy)

- RUN
```bash
java -jar 
    -Dmilos.endpoint.hostname='http://localhost:9081' 
    -Dmilos.endpoint.port='9081' 
    -Dmilos.proxy.hostname='localhost' 
    -Dmilos.proxy.port=9080 
    -Dmilos.certificate.signing='file:///path/to/signingCertificate' 
    -Dmilos.certificate.private='file:///path/to/privateKey' 
    -Dmilos.certificate.encryption='file:///path/to/encryptionCertificate' 
    -Dmilos.message.content='file:///path/to/content.xml' 
    ./as2-camel-0.0.1-SNAPSHOT.jar
```
- create JAR
```bash
mvn clean package spring-boot:repackage  -Dmaven.test.skip=true
```

- generate certificate
```bash
openssl req -newkey rsa:4096             
    -x509             
    -sha256             
    -days 3650             
    -nodes             
    -out example.crt             
    -keyout example.key
```


## tst
```
java -jar 
-Dmilos.endpoint.hostname='tsoas2.test.prisma-capacity.cloud'
 -Dmilos.endpoint.port=443 
 -Dmilos.proxy.hostname='proxyfg.net4gas.n4g.local' 
 -Dmilos.proxy.port=8080 
 -Dmilos.message.content='file:///home/klapal-ext/test.xml' 
 -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8001 as2-camel-0.0.1-SNAPSHOT.jar
```