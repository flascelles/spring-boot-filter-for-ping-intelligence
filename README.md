# spring-boot filter for PingIntelligence for APIs
This is an example filter for a microservice built with spring boot. The filter collects metadata regarding the API traffic flowing through the  micoservice. This metadata is sent to [PingIntelligence for APIs](https://www.pingidentity.com/en/software/pingintelligence.html) by calling a sideband API. The component in the PingIntelligence for APIs which implements this sideband API is named API Security Enforcer (ASE).

## How to use

Include this filter in your Spring Boot project code.

The filter directs the sideband API calls to the ASE address provided by system variable `ase.baseurl`. Pass the ASE sideband listener address by setting this property when starting your microservice.
For example:
```
java -Dserver.port=8080 -Dase.baseurl=http://10.11.16.105:8443 -jar microservice.jar
```

## Additional Considerations

You can provide your implementation specific way to dereference the identity of the user behind an API call by overriding this method:
```
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
```
Adding your implementation-specific subject dereferencing will automatically attach user name information to each of your API calls. This generates rich identity correlation in the reports and dashboards provided by PingIntelligence for APIs

In some situations, you may want to not feed this metadata for all the calls going through this microservice, but only for some of it. You can add such rules at the beginning of the doFilter() method.
```
    @Override
    public void doFilter
            (ServletRequest request, ServletResponse response, FilterChain filterchain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest)request;
        if (shouldSkip(req)) {
            logger.info("skipping call " + req.getRequestURI());
            filterchain.doFilter(request, response);
            return;
        }
        ...
    }
    ...
    private boolean shouldSkip(HttpServletRequest req) {
        boolean isNotDemoApi = true;
        String uri = req.getRequestURI();
        if (uri != null && uri.contains("ehealth/patientinfo")) {
            isNotDemoApi = false;
        }
        return isNotDemoApi;
    }
```
