Here’s the step-by-step process, including every single code snippet required, to create a Spring Boot app and run it as a Kubernetes job.

---

### 1. Spring Boot Application

#### File: `pom.xml`
Add dependencies to your Spring Boot project:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

---

#### File: `src/main/java/com/example/demo/DemoApplication.java`
Create the main application with a simple task:
```java
package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("Running the Spring Boot Kubernetes Job!");
        // Add your logic here
    }
}
```

---

### 2. Dockerize the Spring Boot Application

#### File: `Dockerfile`
Create the Dockerfile for the Spring Boot app:
```dockerfile
FROM openjdk:21-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=target/demo-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

#### Commands to Build Docker Image
1. Package the application:
   ```bash
   mvn clean package
   ```
2. Build the Docker image:
   ```bash
   docker build -t spring-boot-job:latest .
   ```

---

### 3. Kubernetes Job Configuration

#### File: `k8s-job.yaml`
Define the Kubernetes job:
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: spring-boot-job
spec:
  parallelism: 1
  completions: 1
  backoffLimit: 3
  template:
    spec:
      containers:
      - name: spring-boot-container
        image: spring-boot-job:latest
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1"
        imagePullPolicy: Never
      restartPolicy: Never
```

---

### 4. Deploy and Run the Kubernetes Job

#### Step 1: Enable Kubernetes in Docker Desktop
Ensure Kubernetes is enabled in Docker Desktop.

#### Step 2: Load the Docker Image into Kubernetes
If you’re using Kind (Kubernetes in Docker), load the Docker image:
```bash
kind load docker-image spring-boot-job:latest
```
If not, skip this step.

#### Step 3: Apply the Job YAML File
Run the following command to create the job in Kubernetes:
```bash
kubectl apply -f k8s-job.yaml
```

#### Step 4: Check the Job Status
Monitor the job:
```bash
kubectl get jobs
kubectl logs job/spring-boot-job
```

---

### 5. Clean Up Resources

Once the job completes, you can clean it up:
```bash
kubectl delete job spring-boot-job
```

---

### Full Directory Structure
Here’s how your project should look:
```
spring-boot-job/
├── Dockerfile
├── k8s-job.yaml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/demo/
    │   │       └── DemoApplication.java
    │   └── resources/
```

---

With this complete setup, you can create, run, and monitor your Spring Boot application as a Kubernetes job. Let me know if you need any help with this!
