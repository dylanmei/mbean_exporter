mbean_exporter
--------------

A Prometheus exporter for JMX managed-beans. Similar to [prometheus/jmx_exporter](https://github.com/prometheus/jmx_exporter) but different.

---

Rather than doing this (ノಠ益ಠ)ノ彡┻━┻

```
rules:
- pattern: kafka.(\w+)<type=(.+), name=(.+), (.+)=(.+), (.+)=(.+)><>Value
  name: kafka_$1_$2_$3
  type: GAUGE
  labels:
    "$4": "$5"
    "$6": "$7"
```

You can be doing this ヾ(＠⌒ー⌒＠)ノ

```
domains: kafka.log
  beans:
  - query: "type=Log,name=Size,topic=*,partition=*"
    attributes:
    - Value: gauge
    metric: "kafka_log_partition_size"
    labels:
      topic: "${keyprop topic | lower}"
      partition: "${keyprop partition}"
  - query: "type=LogCleanerManager,name=time-since-last-run-ms"
    attributes:
    - Value: gauge
    metric: "kafka_${keyprop type | snake}_${keyprop name | snake}
  - query: "type=LogFlushStats,name=LogFlushRateAndTimeMs"
    attributes:
    - Mean: gauge
    - Count: counter
    metric: "kafka_log_flush_${attribute | replace Count total | replace Mean avg_time}"
```
