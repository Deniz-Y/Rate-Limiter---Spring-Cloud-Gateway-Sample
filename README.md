# API RATE LIMITER WITH BUCKET4J
## Introduction
This project is an api rate limiter that use spring cloud gateway, bucket4j, and hazelcast to limit the users based on their ip addresses for protecting the application from DoS attacks, and the massive usage of some of the users.

## Configuration
- spring.cloud.gateway.routes.id = name of the route
  - we named "get_route" and "post_route" because of our endpoints, you can name according to your endpoints
- spring.cloud.gateway.routes.uri : url of the page you want to direct
  - we used "httpbin.org" for unit tests and "wiremock port" for load tests
- spring.cloud.gateway.routes.predicates : name of the endpoint you will send request
  - we used "/get" and "/post", you can use whatever you want
- spring.cloud.gateway.cache.type : where to store the data
  - we used hazelcast, redis is another alternative

- rate-limiter.costs.path : the path that you wish to limit
- rate-limiter.costs.cost : the cost of the path above
- rate-limiters.limits.capacity : the number of tokens the bucket has
- rate-limiters.limits.periods : the wait time before refresh the bucket again
- rate-limiters.limits.name : the name of the limiter

- management.endpoints.web.exposure.include : to determine the enabled endpoints that will be monitored by the actuator endpoint
  - we used "*" to enable all of the endpoints
- management.metrics.tags.application : the application name tag that will be applied to all meters to monitor metrics with prometheus and grafana
  - we named as "rate-limiter"

- rate-limit.enable : the condition for to create the class RateLimiterPreFilter to switch the rate limiter on and off

## RateLimiterPreFilter

This class is a component for the limiting requests.

```
@ConditionalOnProperty(prefix = "rate-limit", name = ["enabled"], havingValue = "True")
```
This is the annotation for creating the class if rate-limit.enabled is True in the configuration file.

```
val BucketList = rateLimiterConfig.limiters.map {
            val configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(it.capacity, Refill.intervally(it.capacity, it.period)))
                .build()
```
This is the code for configuring bucket properties according to application.yml file.
Refill.intervally is an alternative to default greedy refill option that refresh all the tokens at the same time.

```
if(exchange.request.headers.containsKey("X-Forwarded-For")){
                val hazelcastBucket: Bucket = proxyManager.builder().build(exchange.request.headers.getValue("X-Forwarded-For").toString()+  it.name, configuration)
                hazelcastBucket

            }else{

                val hazelcastBucket: Bucket = proxyManager.builder().build(exchange.request.remoteAddress?.address.toString() +  it.name, configuration)
                hazelcastBucket
            }
```
This is the code for starting the bucket with a key and the configuration above in the hazelcast.
Key is the value of X-Forwarded-For header if exists, the value of address in the remote address of the request otherwise.
We used all the information that is found in the value of X-Forwarded-For header, the first ip which is the main ip of the client in the value of the X-Forwarded-For header, can be used for more accurate results. 

```
val cost = rateLimiterConfig.costs.filter { it.path == exchange.request.path.value() }.sumOf { it.cost }
```
This is the code for calculating the total cost of a request sent to certain endpoint.

```
val results = BucketList.map { it.tryConsumeAndReturnRemaining(cost) }
```
This is the code that returns a list of boolean for the sufficiency of the number of tokens, and the number of the remaining tokens.

```
return if (results.all { it.isConsumed }) {
            exchange.response.headers.add("Remaining", results.minOf { it.remainingTokens }.toString())
            metricService.incrementRequestAction("","",RateLimiterAction.Block)
            chain.filter(exchange)
        } else {
            val waitTimeInSecond = results.maxOf { it.nanosToWaitForRefill } / 10.0.pow(9.0)
            MetricClass.processRequest()
            exchange.response.headers.add("Retry-After", waitTimeInSecond.toString())
            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
            exchange.response.writeWith(Mono.empty())
```
If all the limiters has the sufficient capacity for the request:
  - Add the number of remaining tokens to the headers
  - Add the metrics of the request to the metric service
  - Execute the filter chain

Otherwise:
  - Call the processRequest() function of metric class
  - Add the remaining time to refresh the bucket as a header
  - Change the http status code of the response to "HTTP 429 : Too many requests"
  - Print the response

## Unit Tests
### Test 1:
This is for the test the rate limiter for its accuracy.
* The capacity of the bucket is 10.
* A get request consumes 3 tokens.
* The counter starts with the value 10 and decrease 3 in each step
* In each step: 
  * A get request is sent
  * If the counter is grater then 3 the expected http status code of the response is "HTTP 200 : OK", otherwise "HTTP 429 : Too many requests"
  * Response.statusCode is checked for being the same as expected
  * If the status code is OK:
    * Check for the value of the remaining header is not null and equals the counter - 3 as expected
  * Otherwise, save the remaining time thanks to the header "Retry-After".
* After counter has reached to 0:
  * Sleep until the saved remaining time is passed
  * Send a get request and check the response is with the "HTTP OK" status code.

### Test 2:
This is for the test whether the rate limiter is based-on the ip of the client.
* A unique ip address is assigned to each clients 
* Each client can send 3 get requests
* HTTP 200 is expected for the first 3 requests and HTTP 429 is expected for the fourth request of a client
* Check the result is the same as expected

## Deploy to Kubernetes:

   `docker login registry.rancher.valensas.com`
   -- enter username and password --

   `gradle jib --image=registry.rancher.valensas.com/interns/rate-limit:latest`

   copy the following yml: 

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rate-limiting
  namespace: rate-limiting
spec:
  selector:
    matchLabels:
      app: rate-limiting
  template:
    metadata:
      labels:
        app: rate-limiting
    spec:
      containers:
      - name: rate-limiting
        image: registry.rancher.valensas.com/interns/rate-limit:latest
        imagePullPolicy: Always
        ports:
          - containerPort: 8080
            name: http
        env:
        - name: SPRING_CLOUD_GATEWAY_ROUTES_0_URI
          value: http://wiremock:9999
        - name: SPRING_CLOUD_GATEWAY_ROUTES_0_ID
          value: get_route
        - name: SPRING_CLOUD_GATEWAY_ROUTES_0_PREDICATES_0
          value: Path=/get
        - name: SPRING_CLOUD_GATEWAY_ROUTES_1_URI
          value: http://wiremock:9999
        - name: SPRING_CLOUD_GATEWAY_ROUTES_1_ID
          value: post_route
        - name: SPRING_CLOUD_GATEWAY_ROUTES_1_PREDICATES_0
          value: Path=/post
        - name: RATE-LIMIT_ENABLED
          value: "True"
---
apiVersion: v1
kind: Service
metadata:
  name: rate-limiting
spec:
  selector:
    app: rate-limiting
  ports:
  - port: 8080
    targetPort: 8080
    name: http
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: rate-limiting
spec:
  rules:
  - host: rate-limiter.rancher.valensas.com
    http:
      paths:
      - pathType: Prefix
        path: "/"
        backend:
          service:
            name: rate-limiting
            port:
              number: 8080

```
   `pbpaste| kubectl apply -f -`


