# Scrooge
[![Build Status](https://secure.travis-ci.org/twitter/scrooge.png)](http://travis-ci.org/twitter/scrooge)

Scrooge is a [thrift](http://thrift.apache.org/) code generator written in
Scala, which currently generates code for Scala and Java.

It's meant to be a replacement for the apache thrift code generator, and
generates conforming, compatible binary codecs by building on top of
libthrift.

Since Scala is API-compatible with Java, you can use the apache thrift code
generator to generate Java files and use them from within Scala, but the
generated code uses Java collections and mutable "bean" classes, causing some
annoying boilerplate conversions to be hand-written. This is an attempt to
bypass the problem by generating Scala code directly. It also uses Scala
syntax so the generated code is much more compact.

There is a fairly comprehensive set of unit tests, which actually generate
code, compile it, and execute it to verify expectations.

## Quick-start

There are a couple of classes needed by the generated code. These have been
moved out of scrooge into a separate jar to keep dependencies small.
Maven users need to add the following to the pom.xml file:

    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>scrooge-core_2.9.2</artifactId>
      <version>3.3.2</version>
    </dependency>

SBT users need this:

    val scroogeCore = "com.twitter" %% "scrooge-core" % "3.3.2"

## Full Documentation

<http://twitter.github.io/scrooge/>

## Changes in this fork

This is a fork from the original Twitter project. The two major things that are different 
here are:

1. Generation of case classes from the Thrift IDL (note that this limits the number of
struct members to 22)

2. Seamless conversion from Finagle futures to Scala futures. See example below (look
for MyTestService.ToScala(...))

Thrift IDL:

    namespace java com.test

    exception SomeException {
        1: string message
        2: i32 value
    }
 
    const i32 MyTestServicePort = 8001
 
    service MyTestService {
      list<byte> fetchBlob(1: i64 id, 2: i32 id2)
      string fetchString(1: string id, 2: optional string msg)
      oneway void runSomething()
      void exceptUnhandled()
      void exceptHandled() throws (1: SomeException ex)
    }
  
Service implementation:

    package com.test
 
    import scala.concurrent._
    import scala.concurrent.ExecutionContext.Implicits.global
 
    class MyTestServiceImpl extends MyTestService.ScalaFutureIface {
      override def fetchBlob(id: Long, id2: Int): Future[Seq[Byte]] = {
        val buff = List[Byte](id.toByte, id2.toByte, 2, 3, 4, 5, 99)
        Future(buff)
      }
 
      def asyncFetchString(id: String, msg: Option[String]): String = {
        java.lang.Thread.sleep(1000)
        "test: " + id + " " + msg.get
      }
 
      override def fetchString(id: String, msg: Option[String]): Future[String] = {
        //Future.value("test: " + id + " " + msg.get)
        future { asyncFetchString(id, msg) }
      }
 
      override def runSomething(): Future[Unit] = {
        // Do something here
        println("Running....")
        Future.successful()
      }
 
      def exceptUnhandled(): Future[Unit]  = {
        throw SomeException("unhandled exception on server", 1)
      }
 
      def exceptHandled(): Future[Unit] = {
        throw SomeException("handled exception on server", 2)
      }
 
      override def close(): Future[Unit] = {
        // Overriding this method is optional
        Future.successful()
      }
    }

Server side:

    package com.test
 
    import com.twitter.finagle.builder.ServerBuilder
    import com.twitter.finagle.thrift.ThriftServerFramedCodec
    import java.net.InetSocketAddress
    import org.apache.thrift.protocol.TBinaryProtocol
    import scala.concurrent.ExecutionContext.Implicits.global
 
    object ServerApp extends App {
      val myserviceImpl = new MyTestServiceImpl
      val myServer = MyTestService.ToScala(ServerBuilder()
        .name("MyTestServer")
        .bindTo(new InetSocketAddress(Constants.MyTestServicePort))
        .codec(ThriftServerFramedCodec())
        .maxConcurrentRequests(5)
        .build(new MyTestService.FinagledService(MyTestService.ToScala(myserviceImpl), new TBinaryProtocol.Factory())))
 
      println("press any key to stop")
      System.in.read
      println("shutting down...")
      myServer.close()
      myserviceImpl.close()
      println("end!")
    }
  
Client side:

    package com.test
 
    import org.apache.thrift.protocol.{TBinaryProtocol, TJSONProtocol}
    import com.twitter.finagle.builder.ClientBuilder
    import com.twitter.finagle.thrift.ThriftClientFramedCodec
    import scala.concurrent.{Await, Future}
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global
 
    object ClientApp extends App {
      val myService = ClientBuilder()
        .hosts("localhost:" + Constants.MyTestServicePort)
        .codec(ThriftClientFramedCodec())
        .hostConnectionLimit(1)
        //.tlsWithoutValidation()
        .build()
 
      val myClient = MyTestService.ToScala(new MyTestService.FinagledClient(myService, new TBinaryProtocol.Factory()))
      val response: Future[Seq[Byte]] = myClient.fetchBlob(7, 42)
      val result = Await.result(response, Duration("3 sec"))
      println("Got response!" + result.length)
      println(result.mkString(","))
      println("done")
 
      val response2: Future[String] = myClient.fetchString("abc", Some("xyz"))
      response2.onSuccess{case res => println("Got response - " + res)}
      println("after rpc sent")
 
      println("press any key to stop")
      System.in.read
      Await.ready(myClient.close(), Duration("1 sec"))
      println("end!")
    }
