package exporter

import exporter.jmx.*

import io.prometheus.client.Counter
import io.prometheus.client.Gauge

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import kotlin.system.measureTimeMillis

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
        val collector = MBeanCollector(ConnectionFactory(host, port))
        val writer = when (output) {
            OutputOption.STDOUT -> StdoutWriter()
            OutputOption.HTTP -> PromWriter(httpHost, httpPort)
        }

        runExporter(collector, writer)
        stopExporter(collector, writer)
    }

    fun runExporter(collector: MBeanCollector, writer: Writer) = runBlocking {
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

    fun runCollector(collector: MBeanCollector, writer: Writer) = runBlocking {
        val queries = config.domains
            .map { domainConfig ->
                domainConfig.beans.map { beanConfig ->
                    MBeanQuery(
                        beanConfig,
                        domainConfig.name,
                        beanConfig.query,
                        beanConfig.attributes.names)
                    }
                }.flatten()

        val time = measureTimeMillis {
            queries.asFlow()
                .map { query -> collector.collect(query) }
                .collect { results ->
                    for (result in results)
                        writer.write(result.sample())
                }

            writer.flush()
        }

        mbeanCollectionsSeen.inc()
        mbeanScrapeDuration.set(time.toDouble() / 1000)
        log.debug("Collection time ${time}ms")
    }

    fun stopExporter(collector: MBeanCollector, writer: Writer) {
        collector.close()
        writer.close()
    }

    companion object {
        val log = LoggerFactory.getLogger(Exporter::class.java)
        val mbeanCollectionsSeen = Counter.build()
          .name("mbean_collections_seen_total")
          .help("Number of times mbean collections have been seen.")
          .register()
        val mbeanScrapeDuration = Gauge.build()
          .name("mbean_scrape_duration_seconds")
          .help("Time this MBean scrape took, in seconds.")
          .register()

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
