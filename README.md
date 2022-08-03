to deploy to Kubernetes:

   docker login
   -- enter username and password --

   gradle jib --image=registry.rancher.valensas.com/interns/rate-limit:latest

   copy the following yml: 
   ***
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
   ***
   pbpaste| kubectl apply -f -

