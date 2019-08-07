package io.insecureapis.demo.toolbox;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 This is a filter for a microservice built with spring boot.
 It collects metadata of api traffic flowing through the
 micoservice it is running alongside and feeds it to an
 external API Security Enforcer (ASE). The ASE is a component
 of Ping Identity's Ping Intelligence for APIs which build
 machine learning models of your APIs to look for anomalies.

 You can point your own ASE deployment by running your service
 with the ase.baseurl property. For example
 -Dase.baseurl=http://10.101.30.62:8443

 @date Aug 7, 2019

 @author Francois Lascelles

 This is unsupported code provided as an example
 */
@Component
public class PiFilter implements Filter {
    Logger logger = LoggerFactory.getLogger(PiFilter.class);

    // these are based on out of box ase target URI
    static final String ASE_REQ_URI = "/ase/request";
    static final String ASE_RES_URI = "/ase/response";
    // these are for recursive introspection at dev time use above values when really connecting to ase
    //static final String ASE_REQ_URI = "/hello";
    //static final String ASE_RES_URI = "/hello";


    @Override
    public void destroy() {
        //
    }

    @Override
    public void doFilter
            (ServletRequest request, ServletResponse response, FilterChain filterchain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest)request;
        long ts = System.currentTimeMillis();
        req.setAttribute("ts", ts);
        if (req.getRequestURI().equalsIgnoreCase("/hello")) {
            // bypass hack to prevent infinite loop when testing on self
            logger.info("bypass");
            return;
        }

        String sidebandCallPayload = "{\n" +
                "  \"source_ip\": \""+req.getRemoteAddr()+"\",\n" +
                "  \"source_port\": "+req.getRemotePort()+",\n" +
                "  \"method\" : \""+req.getMethod()+"\",\n" +
                "  \"url\" : \""+req.getRequestURI()+"\",\n" +
                "  \"http_version\": \"1.1\",\n" +
                "  \"headers\" : [\n";
        Enumeration<String> headernames = req.getHeaderNames();
        boolean touched = false;
        while (headernames.hasMoreElements()) {
            String name = headernames.nextElement();
            if (!name.equalsIgnoreCase("user-agent")) {
                String val = req.getHeader(name);
                sidebandCallPayload += "    {\"" + name + "\" : \"" + val + "\"},\n";
                touched = true;
            }
        }
        // remove trailing comma at end of array
        if (touched) sidebandCallPayload = sidebandCallPayload.substring(0,sidebandCallPayload.length()-2);
        String user = PiFilter.subjectFromReq(req);
        sidebandCallPayload += "\n  ],\n" +
                "  \"user_info\": [{\"username\" : \""+user+"\"} ]\n" +
                "}";


        logger.info("sideband call #1" + sidebandCallPayload);

        RestClient httpClient = new RestClient(ts);
        String piresponse = httpClient.post(ASE_REQ_URI, sidebandCallPayload);
        HttpStatus status = httpClient.getStatus();
        logger.info("PI status = " + status.toString());
        logger.info("PI response = " + piresponse);

        HttpServletResponse res = (HttpServletResponse)response;
        // this is to exit if PI4API says to block this request
        if (!status.toString().startsWith("200")) {
            logger.warn("This call is being blocked based on input from PingIntelligence for APIs " + piresponse);
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterchain.doFilter(request, response);

        sidebandCallPayload = "{\n" +
                "  \"response_code\":\""+res.getStatus()+"\",\n" +
                "  \"response_status\":\"OK\",\n" +
                "  \"http_version\":\"1.1\",\n" +
                "  \"headers\":[";

        if (res.getHeaderNames().size() > 0) {
            Iterator<String> resheaders = res.getHeaderNames().iterator();
            while (resheaders.hasNext()) {
                String name = resheaders.next();
                String val = res.getHeader(name);
                sidebandCallPayload += "    {\"" + name + "\" : \"" + val + "\"},\n";
            }
            // remove trailing comma at end of array
            sidebandCallPayload = sidebandCallPayload.substring(0, sidebandCallPayload.length() - 2);
        }
        sidebandCallPayload += "\n  ]\n}";

        logger.info("sideband call #2" + sidebandCallPayload);
        piresponse = httpClient.post(ASE_RES_URI, sidebandCallPayload);
        //piresponse = httpClient.post("/ase/response", sidebandCallPayload);
        status = httpClient.getStatus();
        logger.info("PI status = " + status.toString());
        logger.info("PI response = " + piresponse);
    }

    private static String subjectFromReq(HttpServletRequest req) {
        // insert implementation-specific way to dereference a user from a request using for example the token
        String authorizationHeader = req.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.length() > 0) {
            // deferencing users from tokens is very implementation specific
            // plug in your method here or just ignore user identities at this point
            // return TokenEntry.userFromToken(authorizationHeader);
        }
        return "anonymous";
    }

    @Override
    public void init(FilterConfig filterconfig) throws ServletException {

    }
}
