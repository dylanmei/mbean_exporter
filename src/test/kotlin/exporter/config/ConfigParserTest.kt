package exporter.config

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ConfigParserTest {
    @Test
    fun `should parse empty config`() {
        val config = ConfigParser().parse(
            """
            domains: []
            """.trimIndent()
        )

        config.domains shouldHaveSize(0)
    }

    @Test
    fun `should parse simple config`() {
        val config = ConfigParser().parse(
            """
        domains:
        - name: foo
          beans:
          - pattern: type=bar
            attributes:
            - Value: counter
            metric: foo_bar_total
            labels:
              hello: world
            """.trimIndent()
        )

        config.domains shouldHaveSize(1)

        with(config.domains.first()) {
            name shouldBe("foo")
            beans shouldHaveSize(1)

            with(beans.first()) {
                pattern shouldBe("type=bar")
                metric shouldBe("foo_bar_total")

                labels shouldNotBe(null)
                labels!! shouldContainKey("hello")

                attributes shouldHaveSize(1)
                with(attributes.first()) {
                    name shouldBe("Value")
                    type shouldBe(AttributeType.COUNTER)
                }
            }
        }
    }

    @Test
    fun `should parse composite attributes`() {
        val config = ConfigParser().parse(
            """
        domains:
        - name: foo
          beans:
          - pattern: type=bar
            attributes:
            - Value.item1: gauge
            - Value.item2: counter
            metric: foo_bar_total
        """.trimMargin()
        )

        config.domains shouldHaveSize(1)
        with(config.domains.first()) {
            beans shouldHaveSize(1)

            with(beans.first()) {
                attributes shouldHaveSize(1)

                with(attributes.first()) {
                    name shouldBe("Value")
                    type shouldBe(AttributeType.COMPOSITE)

                    items shouldHaveSize(2)
                    with(items.first()) {
                        name shouldBe("item1")
                        type shouldBe(AttributeType.GAUGE)
                    }
                    with(items.last()) {
                        name shouldBe("item2")
                        type shouldBe(AttributeType.COUNTER)
                    }
                }
            }
        }
    }

    @Test
    @Disabled
    fun `should parse unknown attribute`() {
        val config = ConfigParser().parse(
            """
        domains:
        - name: foo
          beans:
          - pattern: type=bar
            attributes:
            - Value
            metric: foo_bar_total
        """.trimMargin()
        )

        config.domains shouldHaveSize(1)
        with(config.domains.first()) {
            beans shouldHaveSize(1)

            with(beans.first()) {
                attributes shouldHaveSize(1)

                with(attributes.first()) {
                    name shouldBe("Value")
                    type shouldBe(AttributeType.UNKNOWN)
                }
            }
        }
    }
}
