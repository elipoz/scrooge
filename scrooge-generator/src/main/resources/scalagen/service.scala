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


  trait ScalaFutureIface {
    def close(): scala.concurrent.Future[Unit] = scala.concurrent.Promise[Unit]().future
{{#asyncFunctionsScala}}
    {{>function}}
{{/asyncFunctionsScala}}
  }

  import com.twitter.util.{Promise, Return, Throw}

  class ToScalaIface(scalaIface: ScalaFutureIface)(implicit executionContext: scala.concurrent.ExecutionContext) extends FutureIface {
    private def fromScalaFuture[A](future: scala.concurrent.Future[A]): Future[A] = {
      val promise = Promise[A]()
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
      case Return(r) => promise success r
      case Throw(t)  => promise failure t
    }
    promise.future
  }

  class ToScalaClient(client: FinagledClient) extends ScalaFutureIface {
    override def close(): scala.concurrent.Future[Unit] = {
      toScalaFuture(client.service.close())
    }
{{#functions}}
    {{#headerInfoScala}}{{>header}}{{/headerInfoScala}} = {
      toScalaFuture(client.{{funcName}}({{argNames}}))
    }
{{/functions}}
  }

  import com.twitter.finagle.builder.Server
  import java.net.SocketAddress

  class ToScalaServer(server: Server) {
    def close(): scala.concurrent.Future[Unit] = {
      toScalaFuture(server.close())
    }

    def localAddress: SocketAddress = server.localAddress
  }

  object ToScala {
    def apply(scalaIface: ScalaFutureIface)(implicit executionContext: scala.concurrent.ExecutionContext): FutureIface = {
      new ToScalaIface(scalaIface)
    }

    def apply(client: FinagledClient): ScalaFutureIface = {
      new ToScalaClient(client)
    }

    def apply(server: Server): ToScalaServer = {
      new ToScalaServer(server)
    }
  }
{{/withFinagle}}
}
