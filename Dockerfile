FROM gcr.io/distroless/java:11

ADD build/libs/mbean_exporter-all.jar /mbean_exporter.jar
WORKDIR /
