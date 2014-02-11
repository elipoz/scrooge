package {{package}}

import com.twitter.scrooge.{
  TFieldBlob, ThriftService, ThriftStruct, ThriftStructCodec, ThriftStructCodec3, ThriftUtil}
import java.nio.ByteBuffer
import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TTransport
import org.apache.thrift.TApplicationException
import org.apache.thrift.transport.TMemoryBuffer
import scala.collection.immutable.{Map => immutable$Map}
import scala.collection.mutable.{
  Builder,
  ArrayBuffer => mutable$ArrayBuffer, Buffer => mutable$Buffer,
  HashMap => mutable$HashMap, HashSet => mutable$HashSet}
import scala.collection.{Map, Set}

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"), date = "{{date}}")
trait {{ServiceName}}[+MM[_]] {{#genericParent}}extends {{genericParent}} {{/genericParent}}{
{{#genericFunctions}}
  {{>function}}
{{/genericFunctions}}
}

{{docstring}}
object {{ServiceName}} {
{{#internalStructs}}
{{#internalArgsStruct}}
  {{>struct}}
{{/internalArgsStruct}}
{{#internalResultStruct}}
  {{>struct}}
{{/internalResultStruct}}
{{/internalStructs}}

{{#withFinagle}}
  import com.twitter.util.Future

  trait FutureIface extends {{#futureIfaceParent}}{{futureIfaceParent}} with{{/futureIfaceParent}} {{ServiceName}}[Future] {
{{#asyncFunctions}}
    {{>function}}
{{/asyncFunctions}}
  }

  class FinagledClient(
      service: com.twitter.finagle.Service[com.twitter.finagle.thrift.ThriftClientRequest, Array[Byte]],
      protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
      serviceName: String = "",
      stats: com.twitter.finagle.stats.StatsReceiver = com.twitter.finagle.stats.NullStatsReceiver)
    extends {{ServiceName}}$FinagleClient(
      service,
      protocolFactory,
      serviceName,
      stats)
    with FutureIface

  class FinagledService(
      iface: FutureIface,
      protocolFactory: TProtocolFactory)
    extends {{ServiceName}}$FinagleService(
      iface,
      protocolFactory)


  trait ScalaFutureIface {{#scalaFutureIfaceParent}}extends {{scalaFutureIfaceParent}}{{/scalaFutureIfaceParent}} {
{{^scalaFutureIfaceParent}}
    def close(): scala.concurrent.Future[Unit] = scala.concurrent.Promise[Unit]().future
{{/scalaFutureIfaceParent}}
{{#asyncFunctionsScala}}
    {{>function}}
{{/asyncFunctionsScala}}
  }

  class ToScalaIface(scalaIface: ScalaFutureIface)(implicit executionContext: scala.concurrent.ExecutionContext)
  extends {{#toScalaIfaceParent}}{{toScalaIfaceParent}}(scalaIface) with {{/toScalaIfaceParent}}FutureIface {
    private def fromScalaFuture[A](future: scala.concurrent.Future[A]): Future[A] = {
      val promise = com.twitter.util.Promise[A]()
      future onSuccess{case s => promise setValue s}
      future onFailure{case t => promise setException t}
      promise
    }
{{#functions}}
    {{#headerInfo}}{{>header}}{{/headerInfo}} = {
      fromScalaFuture(scalaIface.{{funcName}}({{argNames}}))
    }
{{/functions}}
  }

  private def toScalaFuture[A](future: Future[A]): scala.concurrent.Future[A] = {
    val promise = scala.concurrent.Promise[A]()
    future respond {
      case com.twitter.util.Return(r) => promise success r
      case com.twitter.util.Throw(t)  => promise failure t
    }
    promise.future
  }

  class ToScalaClient(
            client: com.twitter.finagle.Service[com.twitter.finagle.thrift.ThriftClientRequest, Array[Byte]],
            clientName: String = "",
            protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
            stats: com.twitter.finagle.stats.StatsReceiver = com.twitter.finagle.stats.NullStatsReceiver)
  extends {{#toScalaClientParent}}{{toScalaClientParent}}(client) with {{/toScalaClientParent}}ScalaFutureIface {
    private val finagledClient = new FinagledClient(client, protocolFactory, clientName, stats)

    override def close(): scala.concurrent.Future[Unit] = {
      toScalaFuture(client.close())
    }
{{#functions}}
    {{#headerInfoScala}}{{>header}}{{/headerInfoScala}} = {
      toScalaFuture(finagledClient.{{funcName}}({{argNames}}))
    }
{{/functions}}
  }

  import com.twitter.finagle.builder.{ServerBuilder, ServerConfig}
  type ServerBuilderYYY = ServerBuilder[Array[Byte], Array[Byte], ServerConfig.Yes, ServerConfig.Yes, ServerConfig.Yes]

  class ToScalaServer(
            serverBuilder: ServerBuilderYYY,
            finagledService: FinagledService) {
    private val server: com.twitter.finagle.builder.Server = serverBuilder.build(finagledService)

    def this(
            serverBuilder: ServerBuilderYYY,
            serviceImpl: ScalaFutureIface,
            protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory)(implicit executionContext: scala.concurrent.ExecutionContext) =
        this(serverBuilder, new FinagledService(new ToScalaIface(serviceImpl), protocolFactory))

    def this(
            serverBuilder: ServerBuilderYYY,
            futureIface: FutureIface,
            protocolFactory: TProtocolFactory) =
        this(serverBuilder, new FinagledService(futureIface, protocolFactory))

    def close(): scala.concurrent.Future[Unit] = {
      toScalaFuture(server.close())
    }

    def localAddress: java.net.SocketAddress = server.localAddress
  }
{{/withFinagle}}
}
