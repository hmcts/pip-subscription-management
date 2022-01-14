# Publishing and Information - Subscription Management

## Purpose
The purpose of this service is to provide the ability to interact with and manage subscriptions.


The application exposes health endpoint (http://localhost:4550/health) and metrics endpoint
(http://localhost:4550/metrics).

## Plugins

The template contains the following plugins:

  * checkstyle

    https://docs.gradle.org/current/userguide/checkstyle_plugin.html

    Performs code style checks on Java source files using Checkstyle and generates reports from these checks.
    The checks are included in gradle's *check* task (you can run them by executing `./gradlew check` command).

  * pmd

    https://docs.gradle.org/current/userguide/pmd_plugin.html

    Performs static code analysis to finds common programming flaws. Included in gradle `check` task.


  * jacoco

    https://docs.gradle.org/current/userguide/jacoco_plugin.html

    Provides code coverage metrics for Java code via integration with JaCoCo.
    You can create the report by running the following command:

    ```bash
      ./gradlew jacocoTestReport
    ```

    The report will be created in build/reports subdirectory in your project directory.

  * io.spring.dependency-management

    https://github.com/spring-gradle-plugins/dependency-management-plugin

    Provides Maven-like dependency management. Allows you to declare dependency management
    using `dependency 'groupId:artifactId:version'`
    or `dependency group:'group', name:'name', version:version'`.

  * org.springframework.boot

    http://projects.spring.io/spring-boot/

    Reduces the amount of work needed to create a Spring application

  * org.owasp.dependencycheck

    https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html

    Provides monitoring of the project's dependent libraries and creating a report
    of known vulnerable components that are included in the build. To run it
    execute `gradle dependencyCheck` command.

  * com.github.ben-manes.versions

    https://github.com/ben-manes/gradle-versions-plugin

    Provides a task to determine which dependencies have updates. Usage:

    ```bash
      ./gradlew dependencyUpdates -Drevision=release
    ```

## Setup

Located in `./bin/init.sh`. Simply run and follow the explanation how to execute it.

## Notes

Since Spring Boot 2.1 bean overriding is disabled. If you want to enable it you will need to set `spring.main.allow-bean-definition-overriding` to `true`.

JUnit 5 is now enabled by default in the project. Please refrain from using JUnit4 and use the next generation

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

####Environment Variables
The application requires 4 environment variables ($DB_PASS, $DB_USER, $DB_NAME and $DB_PORT).
These values are required to connect to the postgres db. They can be found in the pip-ss-kv-stg key vault within azure.

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/spring-boot-template` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `4550` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4550/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

## Hystrix

[Hystrix](https://github.com/Netflix/Hystrix/wiki) is a library that helps you control the interactions
between your application and other services by adding latency tolerance and fault tolerance logic. It does this
by isolating points of access between the services, stopping cascading failures across them,
and providing fallback options. We recommend you to use Hystrix in your application if it calls any services.

### Hystrix circuit breaker

This template API has [Hystrix Circuit Breaker](https://github.com/Netflix/Hystrix/wiki/How-it-Works#circuit-breaker)
already enabled. It monitors and manages all the`@HystrixCommand` or `HystrixObservableCommand` annotated methods
inside `@Component` or `@Service` annotated classes.

### Other

Hystrix offers much more than Circuit Breaker pattern implementation or command monitoring.
Here are some other functionalities it provides:
 * [Separate, per-dependency thread pools](https://github.com/Netflix/Hystrix/wiki/How-it-Works#isolation)
 * [Semaphores](https://github.com/Netflix/Hystrix/wiki/How-it-Works#semaphores), which you can use to limit
 the number of concurrent calls to any given dependency
 * [Request caching](https://github.com/Netflix/Hystrix/wiki/How-it-Works#request-caching), allowing
 different code paths to execute Hystrix Commands without worrying about duplicating work

## API
This service exposes various RESTful api's that help complete its purpose. Below is a list of endpoints and their
operations.

POST `/subscription` - Creates a new unique subscription based on a valid JSON body conforming to the [Subscription model](#subscription-model). Returns 201
on success with a message of "Subscription successfully created with the
id: {subscription id}" or 400 on invalid payload

DELETE `/subscription/{subscriptionId}` - Deletes a subscription from the database that matches the subscription ID.
Returns a 200 on success with the message "Subscription: {subId} was deleted" or 404 if the supplied ID was not found.

GET - `/subscription/{subscriptionId}` - Returns a single subscription based on the subscription ID. Returns a 200
on success or a 404 if the subscription ID was not found.

GET - `/subscription/user/{userId}` - Returns a [User Subscription Model](#usersubscription-model) containing the
cases and courts a user is subscribed to. Returns a 200 on success.


## Models
Various models are used to build the objects needed to send and receive data, see models used in Subscription
Management below.

### Subscription Model
Subscription model representing the incoming data, please note that `ID`, `createdDate` and `courtName` are attributes
that exist in this model but are created automatically once the object has been received.

```json
{
  "userId": "ID of the user, linking to the user table",
  "searchType": "ENUM of searchType",
  "searchValue": "The Search value that is used against the search type",
  "channel": "ENUM of the channel the user is to receive subscriptions by",
  "caseNumber": "Case number of the case being subscribed to",
  "caseName": "Name of the case being subscribed to",
  "urn": "URN number being subscribed to"
}
```

### UserSubscription Model

```json
{
  "caseSubscriptions": [
    {
      "caseName": "Name of the case",
      "caseNumber": "Case number of the case being subscribed to",
      "urn": "URN number being subscribed to",
      "dateAdded": "LocalDateTime of when the subscription was created"
    }
  ],
  "courtSubscriptions": [
    {
      "courtName": "Name of the court being subscribed to",
      "dateAdded": "LocalDateTime of when the subscription was created"
    }
  ]
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

