#### Overview  

Current project includes a Proof of concept for a generic scalable policy engine.
Policy engine is based on drools with rule kjars discovery in a remote maven repository. 
Messages are delivered at the policy engine workers based on consistent hashing technique.

#### Policy engine Arquitecture:

![policyArchitecture](/images/policyArchitecture.png)

Current solution supports consistent hashing delivery of messages at policy manager workers:  

![consistenhashingpolicymanager](/images/consistenhashingpolicymanager.png)

#### Prerequisites:
1. Rabbitmq pub/sub framework with the https://github.com/rabbitmq/rabbitmq-consistent-hash-exchange enabled.  
You can access to http://localhost:15672 with username/password guest/guest.  
You should configure rabbitmq by ip at the application properties of policyengine  
```
docker-compose up -d broker
docker exec broker rabbitmq-plugins enable rabbitmq_management
docker exec broker rabbitmq-plugins enable rabbitmq_consistent_hash_exchange
```
2. A Nexus maven repository  
```
docker-compose up -d my-nexus //like this there is a connectivity problem between policy manager container and nexus
docker run -d -p 8081:8081 -p 8082:8082 -p 8083:8083 --name my-nexus sonatype/nexus3:3.0.0
```
You should create a new repository named  maven-group that includes the following sub repositories: central, releases & snapshots.  
Extra information can be found http://codeheaven.io/using-nexus-3-as-your-repository-part-1-maven-artifacts/  
You can access Nexus repository at http://localhost:8081  


#### Local mode execution it in standalone mode:
```
mvn clean instal 
java -jar target/consistent-hash-exchange-policy-engine-0.0.1-SNAPSHOT.jar 
```

#### Containerized mode:
In containerized  mode there is an extra prerequisites. This consist in enabling a load balancer in front of the workers. Traefik load balancer is selected for that. You can access traefik at http://localhost:8080/dashboard/    

##### Start Traefik load balancer
```
sudo service apache2 stop //Stop apache because traefik uses the port 80
docker-compose up -d reverse-proxy 
```

##### Create policyengine container(s)
```
docker  build -t consistentpolicyengine . // build policy engine image
docker-compose up -d consistentpolicyengine // Create only one worker
docker-compose up -d --scale consistentpolicyengine=2  //Create a cluster of policy engine containers
```

##### Some usefull commmands for testing are:  
```docker images //fetch all docker images  
docker image prune -a // remove all images which are not used by existing containers  
docker rm $(docker stop $(docker ps -a -q --filter ancestor=consistentpolicyengine --format="{{.ID}}")) // kill all policy engine workers
```  

#### References:
https://arxiv.org/pdf/1406.2294.pdf  
https://stattrek.com/chi-square-test/independence.aspx  
https://www.uuidgenerator.net/  

##### Useful and brief explication of how consistent hash function works  
![HashingRing](/images/hashring.jpg)
