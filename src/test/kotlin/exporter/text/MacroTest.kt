package exporter.text

import com.glispa.combo.MacroFactory
import com.glispa.combo.MacroRegistry
import com.glispa.combo.Template

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContain
import io.kotest.assertions.throwables.shouldThrow

class MacroTest : ShouldSpec() {
    init {
        val vars = Vars("bean.exporter", mapOf("country" to "Guatemala", "region" to "Fraijanes Plateau"), "Count")

        should("format domain var") {
            Template(VarMacroRegistry, "\${domain}").render(vars) shouldBe "bean.exporter"
        }

        should("format attribute var") {
            Template(VarMacroRegistry, "\${attribute}").render(vars) shouldBe "Count"
        }

        should("format keyprops var") {
            Template(VarMacroRegistry, "\${keyprops}").render(vars) shouldBe "country=Guatemala,region=Fraijanes Plateau"
        }

        should("format keyprop var") {
            Template(VarMacroRegistry, "\${keyprop country}").render(vars) shouldBe "Guatemala"
        }

        should("format lowercase var") {
            Template(VarMacroRegistry, "\${keyprop country | lower}").render(vars) shouldBe "guatemala"
        }

        should("format uppercase var") {
            Template(VarMacroRegistry, "\${keyprop country | upper}").render(vars) shouldBe "GUATEMALA"
        }

        should("format snakecase var") {
            Template(VarMacroRegistry, "\${keyprop region | snake}").render(vars) shouldBe "fraijanes_plateau"
        }

        should("format replace var") {
            Template(VarMacroRegistry, "\${attribute | replace Count total}").render(vars) shouldBe "total"
        }

        should("format remove var") {
            Template(VarMacroRegistry, "\${keyprop region | replace Plateau}").render(vars) shouldBe "Fraijanes "
        }
    }
}

