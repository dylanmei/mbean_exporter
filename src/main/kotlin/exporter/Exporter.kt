package exporter

import exporter.jmx.*
import exporter.config.*
import exporter.text.Vars

import io.prometheus.client.Counter
import io.prometheus.client.Gauge

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import kotlin.system.measureTimeMillis

import sun.misc.Signal
import sun.misc.SignalHandler

import picocli.CommandLine
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

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
        val collector = openCollector()
        val writer = when (output) {
            OutputOption.STDOUT -> StdoutWriter()
            OutputOption.HTTP -> PromWriter(httpHost, httpPort)
        }

        runExporter(collector, writer)
        collector.close()
        writer.close()
    }

    fun openCollector() = runBlocking {
        val connector = try {
            MBeanConnector(host, port)
        } catch (e: MBeanConnectorException) {
            log.error(e.localizedMessage)
            exitProcess(1)
        }

        MBeanCollector(connector)
    }

    fun runExporter(collector: MBeanCollector, writer: Writer) = runBlocking {
        val tickerChannel = ticker(delayMillis = repeatDelay, initialDelayMillis = 0)
        val cancelChannel = trapSignal("INT")

        val runForever = repeatDelay > 0L
        var continuing = true

        while (continuing) {
            continuing = select {
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
                    val attributesConfig = beanConfig.attributes
                    val attributeNames = attributesConfig.map { it.name }.toSet()
                    MBeanQuery(beanConfig, domainConfig.name, beanConfig.pattern, attributeNames)
                }
            }.flatten()

        val time = measureTimeMillis {
            try {
                queries.asFlow()
                    .map { query -> collector.collect(query) }
                    .collect { results ->
                        for (result in results) writeBean(writer, result)
                    }

                writer.flush()
                mbeanCollectionsSeen.inc()
                mbeanConnectionUp.set(1.0)
            }
            catch (e: MBeanConnectorException) {
                log.error(e.localizedMessage)
                mbeanConnectionUp.set(0.0)
            }
        }

        mbeanScrapeDuration.set(time.toDouble() / 1000)
        log.debug("Collection time {}ms", time)
    }

    fun writeBean(writer: Writer, bean: MBean) {
        for (attribute in bean.attributes) {
            when (attribute) {
                is Simple -> writeSimpleBean(writer, bean, attribute)
                is Composite -> writeCompositeBean(writer, bean, attribute)
            }
        }
    }

    fun writeSimpleBean(writer: Writer, bean: MBean, attribute: Simple) {
        val beanConfig = bean.query.context as BeanConfig
        val attributeConfig = beanConfig.attributes.find {
            it.name == attribute.name
        } ?: throw RuntimeException("Unknown attribute ${bean.domain}${beanConfig.pattern} ${attribute.name}")

        val vars = Vars(bean.domain, bean.keyProperties, attribute.name)
        writer.write(beanConfig, vars, attributeConfig.type, attribute.value)
    }

    fun writeCompositeBean(writer: Writer, bean: MBean, attribute: Composite) {
        val beanConfig = bean.query.context as BeanConfig
        val attributeConfig = beanConfig.attributes.find {
            it.name == attribute.name
        } ?: throw RuntimeException("Unknown attribute ${bean.domain}:${beanConfig.pattern} ${attribute.name}")

        attributeConfig.items.forEach { itemConfig ->
            attribute.items.find { item ->
                itemConfig.name == item.name
            }?.run {
                val vars = Vars(bean.domain, bean.keyProperties, attribute.name + "." + name)
                writer.write(beanConfig, vars, itemConfig.type, value)
            }
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(Exporter::class.java)!!
        val mbeanConnectionUp = Gauge.build()
            .name("mbean_up")
            .help("Whether the mbean connection is up (1) or down (0).")
            .register()!!
        val mbeanCollectionsSeen = Counter.build()
            .name("mbean_collections_seen_total")
            .help("Number of times mbean collections have been seen.")
            .register()!!
        val mbeanScrapeDuration = Gauge.build()
            .name("mbean_scrape_duration_seconds")
            .help("Time this MBean scrape took, in seconds.")
            .register()!!

        @JvmStatic
        fun main(args: Array<String>) {
            val commandLine = CommandLine(Exporter())
            commandLine.registerConverter(Config::class.java, ConfigConverter())
            commandLine.isCaseInsensitiveEnumValuesAllowed = true
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
