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
domains:
- name: kafka.log
  beans:
  - pattern: "type=Log,name=Size,topic=*,partition=*"
    attributes:
    - Value: gauge
    metric: "kafka_log_partition_size"
    labels:
      topic: "${keyprop topic | lower}"
      partition: "${keyprop partition}"
- name: kafka.server
  beans:
  - pattern: "type=KafkaRequestHandlerPool,name=RequestHandlerAvgIdlePercent"
    attributes:
    - OneMinuteRate: gauge
    metric: "kafka_broker_${keyprop name | snake}"
  - query: "type=BrokerTopicMetrics,name=*"
    attributes:
    - Count: counter
    metric: "kafka_topics_${keyprop name | replace PerSec | replace Total | snake}_${attribute | replace Count total}"
```
