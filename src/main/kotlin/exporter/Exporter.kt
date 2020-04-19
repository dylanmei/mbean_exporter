package exporter

import exporter.jmx.*

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select

import sun.misc.Signal
import sun.misc.SignalHandler

import java.io.File

import picocli.CommandLine
import org.slf4j.LoggerFactory

@CommandLine.Command(name = "mbean_exporter")
class Exporter : Runnable {
    @CommandLine.Option(
        names = ["--jmx.host"],
        description = ["JMX Host name to export metrics from"])
    var host: String = "localhost"

    @CommandLine.Option(
        names = ["--jmx.port"],
        description = ["JMX Host port to export metrics from"])
    var port: Int = 9010

    @CommandLine.Option(
        names = ["--http.host"],
        description = ["HTTP Host name to bind the output to"])
    var httpHost: String? = null

    @CommandLine.Option(
        names = ["--http.port"],
        description = ["HTTP Host port to bind the output to"])
    var httpPort: Int = 1234

    @CommandLine.Option(
        names = ["--jmx.timeout.ms"],
        description = ["Time to wait before cancelling JMX queries"])
    var maxTimeout: Long = 60000L

    @CommandLine.Option(
        names = ["--repeat.ms"],
        description = ["Duration between iterations; otherwise run once and exit"])
    var repeatDelay: Long = 0

    @CommandLine.Option(
        names = ["-f", "--config.path"],
        defaultValue = "config.yaml",
        description = ["Query configuration path"])
    lateinit var config: Config

    @CommandLine.Option(
        names = ["-o", "--output"],
        defaultValue = "stdout",
        description = ["Export query results to 'stdout' or 'http'"])
    lateinit var output: OutputOption

    override fun run() {
        val collector = Collector(ConnectionFactory(host, port))
        val writer = when (output) {
            OutputOption.STDOUT -> StdoutWriter()
            OutputOption.HTTP -> PromWriter(httpHost, httpPort)
        }

        runExporter(collector, writer)
        stopExporter(collector, writer)
    }

    fun runExporter(collector: Collector, writer: Writer) = runBlocking {
        val tickerChannel = ticker(delayMillis = repeatDelay, initialDelayMillis = 0)
        val cancelChannel = trapSignal("INT")

        val runForever = repeatDelay > 0L
        var continuing = true

        while (continuing) {
            continuing = select<Boolean> {
                cancelChannel.onReceive {
                    false
                }
                tickerChannel.onReceive {
                    runCollector(collector, writer)
                    runForever
                }
            }
        }
    }

    fun runCollector(collector: Collector, writer: Writer) = runBlocking {
        val beans = Channel<Bean>()
        val timeout = queryDomains(collector, config.domains, beans)

        async {
            for (bean in beans) {
                writer.write(bean)
            }
            writer.flush()
        }

        timeout.cancel()
    }

    fun CoroutineScope.queryDomains(collector: Collector, domains: List<DomainConfig>, beans: Channel<Bean>): Job {
        val query = async {
            domains.map { domain ->
                domain.beans.map { bean ->
                    collector.query(bean, Collector.Query(
                        domain = domain.name,
                        query = bean.query,
                        attributes = bean.attributes.names))
                }.forEach {
                    for (result in it) {
                        beans.send(result.sample())
                    }
                }
            }
        }

        query.invokeOnCompletion { beans.close() }

        val timeout = launch {
            delay(maxTimeout)
            log.warn("Cancelling remaining JMX queries: ${maxTimeout}ms time to wait has been exceeded.")
            query.cancel()
            beans.close()
        }

        return timeout
    }

    fun stopExporter(collector: Collector, writer: Writer) {
        collector.close()
        writer.close()
    }

    companion object {
        val log = LoggerFactory.getLogger(Exporter::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            var commandLine = CommandLine(Exporter())
            commandLine.registerConverter(Config::class.java, ConfigConverter())
            commandLine.setCaseInsensitiveEnumValuesAllowed(true)
            commandLine.execute(*args)
        }

        fun trapSignal(signal: String): ReceiveChannel<Unit> {
            val cancelChannel = Channel<Unit>()

            Signal.handle(Signal(signal), object: SignalHandler {
                override fun handle(sig: Signal) {
                    GlobalScope.launch {
                        cancelChannel.send(Unit)
                    }
                }
            })

            return cancelChannel
        }
    }
}
