package exporter

import exporter.jmx.*

import javax.management.Attribute
import javax.management.ObjectName

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.assertions.throwables.shouldThrow

class ConfigParserTest : ShouldSpec() {
    init {
        should("parse empty config") {
            val config = ConfigParser().parse("""
            domains: []
            """.trimIndent())

            config.domains shouldHaveSize(0)
        }

        should("parse simple config") {
            val config = ConfigParser().parse("""
            domains:
            - name: foo
              beans:
              - query: type=bar
                attributes:
                - Value: counter
                metric: foo_bar_total
                labels:
                  hello: world
            """.trimIndent())

            config.domains shouldHaveSize(1)

            with (config.domains.first()) {
                name shouldBe("foo")
                beans shouldHaveSize(1)

                with (beans.first()) {
                    query shouldBe("type=bar")
                    metric shouldBe("foo_bar_total")

                    labels shouldNotBe(null)
                    labels!! shouldContainKey("hello")

                    attributes shouldHaveSize(1)
                    with (attributes.first()) {
                        name shouldBe("Value")
                        type shouldBe(AttributeType.COUNTER)
                    }
                }
            }
        }

        // TODO: Not implemented
        should("parse untyped attribute").config(enabled = false) {
            val config = ConfigParser().parse("""
            domains:
            - name: foo
              beans:
              - query: type=bar
                attributes:
                - Value
                metric: foo_bar_total
            """.trimMargin())

            config.domains shouldHaveSize(1)
            with (config.domains.first()) {
                beans shouldHaveSize(1)

                with (beans.first()) {
                    attributes shouldHaveSize(1)

                    with (attributes.first()) {
                        name shouldBe("Value")
                        type shouldBe(AttributeType.UNTYPED)
                    }
                }
            }
        }
    }
}
