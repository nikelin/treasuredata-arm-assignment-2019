
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

## Protocol Specifics

Communication between the client and the server applications is done via TCP/IP connection using the custom protocol where each message has three fields:
* **int:token** (4 bytes) - optional user data which can be used in a routing logic implementation
* **int:length** (4 bytes) - sets the length of the body field
* **bytearray:body** (variable) - packet body

## Application logic handlers 

Each command supported by the TD Server endpoint is backed by its own custom command handler. Command handlers are resolved on the runtime from the configuration file - `application.properties`. 

At this point, the resolution strategy is hardcoded and cannot be overridden in a way other then the changes into the source code. It can be replaced through introduction of a service provider instead of a static instantiation logic.

It doesn't prevent user from adding a new handlers, though. The default handlers list is as below:
```
# Routes List  
  
route.get-route-handler.class-name = co.jp.treasuredata.armtd.server.server.commands.impl.GetRouteHandler  
route.quit-route-handler = co.jp.treasuredata.armtd.server.server.commands.impl.QuitCommandHandler  
route.index-route-handler = co.jp.treasuredata.armtd.server.server.commands.impl.IndexRouteHandler  
route.disconnect-route-handler = co.jp.treasuredata.armtd.server.server.commands.impl.DisconnectRouteHandler
```

It is possible for a route to return zero or more response actions and each response action, when executed, can produce zero or more response packets (see above **Protocol Specifics**). 