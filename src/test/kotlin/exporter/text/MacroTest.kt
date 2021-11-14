package exporter.text

import com.glispa.combo.Template
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MacroTest {
    val vars = Vars(
        "bean.exporter",
        mapOf("country" to "Guatemala", "region" to "Fraijanes Plateau"),
        "Count"
    )

    @Test
    fun `should format domain var`() {
        Template(VarMacroRegistry, "\${domain}")
            .render(vars)
            .shouldBe("bean.exporter")
    }

    @Test
    fun `should format attribute var`() {
        Template(VarMacroRegistry, "\${attribute}")
            .render(vars)
            .shouldBe("Count")
    }

    @Test
    fun `should format keyprops var`() {
        Template(VarMacroRegistry, "\${keyprops}")
            .render(vars)
            .shouldBe("country=Guatemala,region=Fraijanes Plateau")
    }

    @Test
    fun `should format keyprop var`() {
        Template(VarMacroRegistry, "\${keyprop country}")
            .render(vars)
            .shouldBe("Guatemala")
    }

    @Test
    fun `should format lowercase var`() {
        Template(VarMacroRegistry, "\${keyprop country | lower}")
            .render(vars)
            .shouldBe("guatemala")
    }

    @Test
    fun `should format uppercase var`() {
        Template(VarMacroRegistry, "\${keyprop country | upper}")
            .render(vars)
            .shouldBe("GUATEMALA")
    }

    @Test
    fun `should format snakecase var`() {
        Template(VarMacroRegistry, "\${keyprop region | snake}")
            .render(vars)
            .shouldBe("fraijanes_plateau")
    }

    @Test
    fun `should format replace var`() {
        Template(VarMacroRegistry, "\${attribute | replace Count total}")
            .render(vars)
            .shouldBe("total")
    }

    @Test
    fun `should format remove var`() {
        Template(VarMacroRegistry, "\${keyprop region | replace Plateau}")
            .render(vars)
            .shouldBe("Fraijanes ")
    }

    @Test
    fun `should format leftof var`() {
        Template(VarMacroRegistry, "\${keyprop region | leftof}")
            .render(vars)
            .shouldBe("Fraijanes")
    }

    @Test
    fun `should format rightof var`() {
        Template(VarMacroRegistry, "\${keyprop region | rightof}")
            .render(vars)
            .shouldBe("Plateau")
    }
}
