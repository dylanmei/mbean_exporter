mbean_exporter
--------------

A Prometheus exporter for JMX managed-beans. Similar to [prometheus/jmx_exporter](https://github.com/prometheus/jmx_exporter) but different.

---

![Build and test](https://github.com/dylanmei/mbean_exporter/workflows/Build%20and%20test/badge.svg) [![Docker Pulls](https://img.shields.io/docker/pulls/dylanmei/mbean_exporter)](https://hub.docker.com/repository/docker/dylanmei/mbean_exporter)

About exporters, the [Prometheus documentation](https://prometheus.io/docs/instrumenting/writing_exporters/#maintainability-and-purity) states:

>The main decision you need to make when writing an exporter is how much work you’re willing to put in to get perfect metrics out of it.

Setting up `mbean_exporter` configurations requires a significant amount of configuration work, but its text-template pipelines offers a satisfying level of control over metric naming.

Rather than doing this (ノಠ益ಠ)ノ彡┻━┻
```
rules:
- pattern: kafka.(\w+)<type=(.+), name=(.+), (.+)=(.+)><>Count
  name: kafka_$1_$2_$3_total
  type: COUNTER
  labels:
    "$4": "$5"
- pattern: kafka.(\w+)<type=(.+), name=(.+), (.+)=(.*)><>(\d+)thPercentile
  name: kafka_$1_$2_$3
  type: GAUGE
  labels:
    "$4": "$5"
    quantile: "0.$6"
```

You can be doing this ヾ(＠⌒ー⌒＠)ノ
```
domains:
- name: kafka.network
  beans:
  - pattern: "type=RequestMetrics,name=RequestsPerSec,request=*,version=*"
    attributes:
    - Count: counter
    metric: "kafka_api_requests_total"
    labels:
      request: "${keyprop request}"
      version: "${keyprop version}"
  - pattern: "type=RequestMetrics,name=*TimeMs,request=*"
    attributes:
    - 50thPercentile: gauge
    - 95thPercentile: gauge
    - 99thPercentile: gauge
    - 999thPercentile: gauge
    metric: "kafka_api_${keyprop name | snake}"
    labels:
      request: "${keyprop request}"
      quantile: "0.${attribute | replace thPercentile}"
```
