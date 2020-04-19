package exporter.text

data class Vars(
    val domain: String,
    val keyProps: Map<String, String>,
    val attribute: String
) {
    val keyPropString: String
        get() =
            keyProps
                .map { it.key + '=' + it.value }
                .joinToString(",")
}
