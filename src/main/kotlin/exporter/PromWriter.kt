package exporter

import exporter.jmx.*
import exporter.text.Vars

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
    }

    val server: HTTPServer
    val mutableSamples = HashMap<String, MetricFamilySamples>()
    var writtenSamples: List<MetricFamilySamples>? = null

    init {
        register<PromWriter>()
        // TODO deal with host
        server = HTTPServer(InetSocketAddress(port), CollectorRegistry.defaultRegistry, true)
    }

    override fun write(beanConfig: BeanConfig, vars: Vars, type: AttributeType, value: Double) {
        val helpString = "${vars.domain} ${vars.keyPropString} ${vars.attribute}"
        val metricString = beanConfig.template
            .render(vars)
            .replace('.', '_')

        val metricType = when(type) {
            AttributeType.COUNTER -> Collector.Type.COUNTER
            AttributeType.GAUGE -> Collector.Type.GAUGE
            else -> Collector.Type.UNTYPED
        }

        val labelNames = beanConfig.labels?.keys ?: emptyList<String>()
        val labelValues = labelNames.map { beanConfig.renderLabel(it, vars) }

        writeSample(Sample(metricString, labelNames.toList(), labelValues, value), metricType, helpString)
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
