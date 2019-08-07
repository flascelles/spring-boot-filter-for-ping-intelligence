package io.insecureapis.demo.toolbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 This is the communication channel into ASE from the Filter
 See PiFilter description

 @date Aug 7, 2019

 @author Francois Lascelles

 This is unsupported code provided as an example
 */
public class RestClient {
    Logger logger = LoggerFactory.getLogger(RestClient.class);
    private String aseBaseUrl;
    private RestTemplate rest;
    private HttpHeaders headers;
    private HttpStatus status;

    public RestClient(long seed) {
        String fromProp = System.getProperty("ase.baseurl");
        if (fromProp == null || fromProp.length() < 1) {
            aseBaseUrl = "http://localhost:9001";
            logger.warn("This is now targeting http://localhost:9001.\n" +
                        "To set your own ase target, set ase.baseurl \n" +
                        "property with base url for ase. For example \n" +
                        "-Dase.baseurl=http://10.101.30.62:8443");
        } else {
            aseBaseUrl = fromProp;
            logger.info("Targeting ASE " + fromProp);
        }
        this.rest = new RestTemplate();
        this.headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("X-CorrelationID", ""+seed);
    }

    public String get(String uri) {
        HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
        ResponseEntity<String> responseEntity = rest.exchange(aseBaseUrl + uri, HttpMethod.GET, requestEntity, String.class);
        this.setStatus(responseEntity.getStatusCode());
        return responseEntity.getBody();
    }

    public String post(String uri, String json) {
        HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
        ResponseEntity<String> responseEntity = rest.exchange(aseBaseUrl + uri, HttpMethod.POST, requestEntity, String.class);
        this.setStatus(responseEntity.getStatusCode());
        return responseEntity.getBody();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }
}
