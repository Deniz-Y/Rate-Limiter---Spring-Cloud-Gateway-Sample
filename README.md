to deploy to Kubernetes:

   docker login registry.rancher.valensas.com
   -- enter username and password --

   gradle jib --image=registry.rancher.valensas.com/interns/rate-limit:latest

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
        image: registry.rancher.valensas.com/interns/rate-limit
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
   pbpaste| kubectl apply -f -


