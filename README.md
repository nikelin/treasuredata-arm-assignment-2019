## Overview

Project consists of a three submodules - library, client and server application.

It uses some third-party dependencies in order to implement some basic features not related to the core application functionality
like CLI arguments parsing, logging handling and functional data structures implementations:

* Apache Commons Lang v3.9
* args4j v2.33
* Logback v1.1.7
* Slf4j API v1.7.21

Plugins used in the build configuration:

* **JaCoCo** - as a test coverage reports provider
* **Spotless** (Google Java Format) - as an automatic Java code formatting provider


## Build

```
./gradlew build
```

The command above will run all test suites in the project and generate two distributable artifacts.

Server application artifact can be located at:
```
$root/server-app/build/distributions/server-app-1.0.zip
```

Client  application artifact can be located at:
```
$root/client-app/build/distributions/client-app-1.0.zip
```


Also, in order to check the project test coverage you can use the **JaCoCo** test report, it can be generated with the following command:
```
./gradlew test jacocoTestReport
```

The resulting test report will be available for each sub-module at `$moduleName/build/reports/jacoco/test/html/index.html`.

## Run

Firstly, you will need to start a server application. It can be started from the distributable package generated as a result of the build process.

To start the server application, you will need to specify at least `port` and `directory` values via CLI.

**server-app**
```
$pathToServerDist/bin/server-app -d /opt/files -p 18078
```

Then, when the server is up and running you can start the **client-app**:
```
$pathToClientDist/bin/client-app -p 18078
```

## Commands available

In total, there are three commands implemented by the TD Server endpoint:

* **get {filename}** - fetches the requested file and prints it out to the console
* **index** - fetches and then prints to the console the list of all files in the root directory provided as a parameter on the server startup
* **stop** - stops the whole instance of the TD Server

# Networking specifics

In order to accept incoming connections and to process their input, the server application utilises non-blocking I/O based on a  Selector API.

Selector subscribes to the server and the client socket events and processes them in the events loop. A dedicated thread is  being allocated ot the events processing loop. Inside the events loop three main actions are being processed:

* Reading input data from the client sockets (when it is available)
* Accepting new connections when they become available
* Closing invalid connections

There are also three more additional threads which the server component requires to function properly:

* **FramesBuilderRunnable**
	* This worker is responsible for forming a data frames based on the all input data received from the client connections
	* Number of workers of this type at this point is hard coded and is set to **3** parallel workers, but can be configured in future
	* Keeps an internal state of the in-flight data frames
		* **Important**: In a case of a server implementation there might be a risk of a memory leak caused by this, so a feedback mechanism has to be implemented to notify this worker about expired connections
	* Used by both the client and the server application
	* Uses `readBuffers` as a source of work

* **PacketsProcessorRunnable**
	* This worker is responsible for processing user requests against the defined set of  route handlers, the resulting packets list is placed into the queue to be processed by the other worker
	* Number of workers of this type at this point is hard coded and is set to **3** parallel workers, but can be configured in future
	* Uses `inPacketsQueue` as a source of work

*  **PacketsWriterRunnable**
	* Responsible for sending back to the client all packets that were generated in response to the received request
	* Number of workers of this type at this point is hard coded and is set to **1** parallel workers, but can be configured in future
	* Uses `outPacketsQueue` as a source of work

## Protocol Specifics

Communication between the client and the server applications is done via TCP/IP connection using the custom protocol where each message has three fields:

* **int:token** (4 bytes) - optional user data which can be used in a routing logic implementation
* **int:length** (4 bytes) - sets the length of the body field
* **bytearray:body** (variable) - packet body

Both the client and the server applications share the same implementation of the communication protocols.

**Important**

In addition, `Packet#token` field is currently used in a broadcast packets implementation. Both client and server applications  agree to treat all packets with the token value of **1921883302** as a broadcast message.

To handle packets of this type the client connector expects an instance of `co.jp.treasuredata.armtd.client.commands.BroadcastPacketHandler`
to be provided on its initialisation. If no handler is provided (`= null`), client will just ignore packets of this type.

In the current implementation there is only one usecase of packets of this type - to notify clients in the event of a TD Server shutdown (when requested by one of the clients).

## Server application logic handlers

Each command supported by the TD Server endpoint is backed by its own custom command handler. Command handlers are resolved on the runtime from the configuration file - `application.properties`, which
is the default resolution strategy. Based on the requirements, the default resolution strategy can be overridden and replaced, for instance, by the classpath scanning approach.

At this point, the resolution strategy is hardcoded and cannot be overridden in a way other then the changes into the source code. It can be replaced through introduction of a service provider instead of a static instantiation logic.

It doesn't prevent user from adding a new handlers, though. The default handlers list is as below:
```
# Routes List

route.get-route-handler.class-name = co.jp.treasuredata.armtd.server.server.commands.impl.GetRouteHandler
route.quit-route-handler.class-name = co.jp.treasuredata.armtd.server.server.commands.impl.QuitCommandHandler
route.index-route-handler.class-name = co.jp.treasuredata.armtd.server.server.commands.impl.IndexRouteHandler
route.disconnect-route-handler.class-name = co.jp.treasuredata.armtd.server.server.commands.impl.DisconnectRouteHandler
```

It is possible for a route to return zero or more response actions and each response action, when executed, can produce zero or more response packets (see above **Protocol Specifics**).


## Client application logic handlers

Application client is designed on a strict request-response basis, meaning that each request requires at least a single response
to be produced by a server application.

Due to the assignment requirements, some requests may require more than a single response to be provided in response to a given client request.

The current request execution semantics do not include guaranteed request execution, so in a case of a possible connectivity problem
request will be failed and permission to re-attempt will be requested from the end-user.