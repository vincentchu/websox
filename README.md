# websox

websox is a scala based websocket library based on Twitter's `Future` library. It incorporates a few features from finagle, but does not implement or use finagle's `Codec` and `Service` constructs.

The project is split into two pieces:

 - `websox-server` - Core library
 - `websox-examples` - A few examples

## Using websox

To use websox to create websocket applications, you must provide two things:

 - An implementation of the `Message[A]` trait
 - An implementation of a `WebsocketService[A]` trait
 

### The `Message[A]` trait

The websocket protocol provides a simple way of passing strings between a client and a server. The `Message[A]` trait is a simple converter between a `String` and a class `A`, based on `com.twitter.util.Bijection[A, String]`. Messages read from the websocket are passed through this converter, converting them to type `A` before being handed off to the websocket service; similarly, messages from the websocket service are converted from `A` to `String` before being sent down the websocket.

An extremely simple implementation is the `StringMessage`, which defines a simple identity mapping between incoming and outgoing messages:

    object StringMessage extends Message[String] {
      def apply(string: String)  = string
      def invert(string: String) = string
    }
    
### The `WebsocketService[A]` trait

Implementations of `WebsocketService[A]` implement application logic and act on messages of type `A` read from the socket. A `WebsocketService` also keeps track of connected clients. A partial implementation of this trait is `LocalWebsocketService[A]` which allows the user to implement only three methods that are unique to their application:

 - `onConnect` - Fired when a new websocket is connected
 - `onMessage` - Fired when a new message is read from the websocket
 - `onClose` - Fired when the websocket is closed, irregardless of who (client or server) initiated the close
  
### Lifecycle of a websocket

Briefly, the following is approximately what happens when the websocket library is used:

1. Client connects to server
2. A randomly generated `socketId` of type `SocketId` is created and registered
3. Client/Server negotiate successful handshake
4. `registerSocket` is called; socket registered with `WebsocketService`
5. `onConnect` is fired
6. Message received from client; `onMessage` is fired on `WebsocketService`
7. `writeMessage` is called on `WebsocketService`; message is written to the connected client
8. Either client or server intiates websocket close
9. `deregisterSocket` is fired on the `WebsocketService`
10. `onClose` is fired on `WebsocketService`
11. Channel is closed

## Putting it all together

Below is a simple sample application that uses the websox library and demonstrates most of the basic functions of the websocket service. The app is a modified echo app, echoing back whatever input it receives as an uppercase string. If the user sends "closeme", the server initiates a close of the session.

    object App {
      def main(args: Array[String]) {
        val service = new LocalWebsocketService[String] {
          def onConnect(socketId: SocketId): Future[Unit] = {
            println("** onConnect")
            Future.Unit
          }

          def onMessage(socketId: SocketId, msg: String): Future[Unit] = {
            println("** onMessage received " + msg + " from " + socketId)

            if (msg == "closeme") {
              // Initiate server-side websocket close
              close(socketId)
            } else {
              // Write a message to the client
              val resp = msg.toUpperCase
              writeMessage(socketId, resp)
            }
          }

          def onClose(socketId: SocketId): Future[Unit] = {
            println("** onClose from", socketId)
            Future.Unit
          }
        }

        val config = ServerConfig(
          StringMessage, // The type of messages your service can handle
          service,       // Your websocket service
          8080           // Port you wish to listen on
        )

        Server(config).bind() // .bind() actually binds and starts server
      }
    }     

## Acknowledgements

This project was a project I used to learn more about Netty and Finagle internals. It was inspired by Jeff Smick's [finagle-websockets](https://github.com/sprsquish/finagle-websocket) implementation.