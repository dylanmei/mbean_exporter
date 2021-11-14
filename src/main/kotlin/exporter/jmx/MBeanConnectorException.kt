package exporter.jmx

import java.lang.RuntimeException

class MBeanConnectorException(message: String?, cause: Throwable?) :
    RuntimeException(message, cause)
