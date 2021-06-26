package exporter.text

import com.glispa.combo.Macro

import kotlin.text.StringBuilder

class SnakeCaseMacro() : Macro<Vars> {
    override fun apply(str: String, vars: Vars): String {
        val length = str.length
        if (length == 0) {
            return str
        }
        if (length == 1) {
            return str.lowercase()
        }

        val buffer = StringBuilder()

        var head = 0
        val tail = length - 1
        while (head < tail) {
            val curr = str[head]
            val next = str[head + 1]

            if (curr.isLetter()) {
                buffer.append(curr.lowercase())

                if (next.isDigit()) {
                    buffer.append('_')
                } else if (curr.isLowerCase() && next.isUpperCase()) {
                    // At camelCase word edge
                    buffer.append('_')
                } else if ((head < tail - 1) && curr.isUpperCase() && next.isUpperCase()) {
                    // Inside an acronym
                    val peek = str[head + 2]
                    if (peek.isLowerCase()) {
                        // Complete the acronym by
                        // appending in underscore
                        buffer.append('_')
                    }
                }
            } else if (curr.isDigit()){
                buffer.append(curr)
                if (next.isLetter()) {
                    buffer.append('_')
                }
            } else if (curr.isWhitespace() || curr == '-') {
                buffer.append('_')
            } else {
                buffer.append(curr)
            }

            head++
        }

        val curr = str[tail]
        if (!curr.isWhitespace()) {
            buffer.append(curr.lowercase())
        }

        return buffer.toString()
    }
}
