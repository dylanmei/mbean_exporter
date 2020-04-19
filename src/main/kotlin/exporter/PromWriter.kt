package exporter

import exporter.text.*

import io.prometheus.client.Collector
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.Collector.MetricFamilySamples.Sample as Sample

import java.io.Closeable
import java.net.InetSocketAddress

import org.slf4j.LoggerFactory

class PromWriter(val host: String?, val port: Int) : Collector(), Collector.Describable, Writer {
    companion object {
        val log = LoggerFactory.getLogger(PromWriter::class.java)
        val beanCollectionsSeen = Counter.build()
          .name("bean_collections_seen_total")
          .help("Number of times bean collections have been seen.")
          .register()
    }

    val server: HTTPServer
    val mutableSamples = HashMap<String, MetricFamilySamples>()
    var writtenSamples: List<MetricFamilySamples>? = null

    init {
        register<PromWriter>()
        // TODO deal with host
        server = HTTPServer(InetSocketAddress(port), CollectorRegistry.defaultRegistry, true)
    }

    override fun write(bean: Bean) {
        val helpString = "${bean.domain}:${bean.query}"
        val beanConfig = bean.config
        beanConfig.attributes.forEach {
            val value = bean.attributes[it.name]
            if (value == null) return

            val vars = Vars(bean.domain, bean.keyProperties, it.name)
            val metric = bean.renderMetric(vars)

            val labelNames = beanConfig.labels?.keys ?: emptyList<String>()
            val labelValues = labelNames.map { bean.renderLabel(it, vars)}
            val metricType = when(it.type) {
                AttributeType.COUNTER -> Collector.Type.COUNTER
                AttributeType.GAUGE -> Collector.Type.GAUGE
            }

            writeSample(Sample(metric, labelNames.toList(), labelValues, value), metricType, helpString)
        }
    }

    fun writeSample(sample: Sample, type: Type, help: String) {
        var mfs = mutableSamples.get(sample.name)
        if (mfs == null) {
            log.debug("Adding sample '{}'", sample.name)

            mfs = MetricFamilySamples(sample.name, type, help, ArrayList<Sample>())
            mutableSamples.put(sample.name, mfs)
        }

        mfs.samples.add(sample);
    }

    override fun flush() {
        writtenSamples = mutableSamples.values.toList()
        mutableSamples.clear()
        beanCollectionsSeen.inc()
    }

    override fun close() {
        server.stop()
    }

    override fun collect(): List<Collector.MetricFamilySamples> {
        return writtenSamples?.let {
            it.toList()
        } ?: emptyList<Collector.MetricFamilySamples>()
    }

    override fun describe(): List<Collector.MetricFamilySamples> {
        return emptyList<Collector.MetricFamilySamples>()
    }
}
