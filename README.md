# WeatherWise

Weather forecast dashboard: Spring Boot + Maven backend, HTML/CSS/JS frontend, data from Open-Meteo (no API key).
Built for the DevOps lab (Weeks 1-6 done here, Weeks 7-12 extend the same project).

## Run locally
```bash
java -version        # needs 17+
mvn -v
mvn clean package    # compile + test + build JAR
java -jar target/weatherwise-1.0.0.jar
# open http://localhost:8080
```
Useful: `mvn clean compile`, `mvn test`, `mvn package`, `mvn spring-boot:run`

## API
- `GET /api/weather?city=Paris`
- `GET /api/places?q=par` (suggestions)
- `GET /actuator/health`, `GET /actuator/prometheus` (used in Week 12)

## Lab mapping
| Week | What to do with this project |
|---|---|
| 1 | Ubuntu VM, install JDK 17, Maven, Git |
| 2 | `git init`, add, commit, push to GitHub |
| 3 | `main` + `develop` + feature branches, merge, pull request |
| 4 | Maven lifecycle: `clean`, `compile`, `test`, `package` |
| 5 | Jenkins freestyle job: Git repo URL, build step `mvn clean package` |
| 6 | Pipeline job from `Jenkinsfile`, JUnit report, archive JAR, email |
| 7 | `docker build -t weatherwise .` then `docker run -p 8080:8080 weatherwise` (Dockerfile included) |
| 8 | Add `docker-compose.yml` (app + MySQL) |
| 9 | Add `k8s/deployment.yaml` + `service.yaml` |
| 10 | Add `ansible/setup.yml` (install Java, deploy JAR) |
| 11 | Add `terraform/main.tf` (EC2) |
| 12 | Deploy to EC2, add Prometheus + Grafana scraping `/actuator/prometheus` |

## Jenkins setup (Weeks 5-6)
1. Install Jenkins, plugins: Git, Pipeline, Maven Integration, Email Extension.
2. Manage Jenkins > Tools: add JDK named `JDK17` and Maven named `Maven3`.
3. Configure email (Manage Jenkins > System > Email notification, SMTP).
4. New Item > Pipeline > "Pipeline script from SCM" > your GitHub URL, branch `*/main`.
5. Edit the email address in `Jenkinsfile`, push, and run the build.
