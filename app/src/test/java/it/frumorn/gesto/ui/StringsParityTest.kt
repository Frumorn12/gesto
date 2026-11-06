package it.frumorn.gesto.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element

/** Every translatable resource of the English strings has an Italian one, and the other way round. */
class StringsParityTest {
    private fun keys(path: String): Set<String> {
        val file = listOf(File(path), File("app/$path")).first { it.exists() }
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val nodes = root.childNodes
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it) as? Element }
            .filter { it.getAttribute("translatable") != "false" }
            .map { "${it.tagName}:${it.getAttribute("name")}" }
            .toSet()
    }

    @Test
    fun everyEnglishStringHasAnItalianOne() {
        val missing = keys("src/main/res/values/strings.xml") - keys("src/main/res/values-it/strings.xml")
        assertEquals("Missing from values-it: $missing", emptySet<String>(), missing)
    }

    @Test
    fun everyItalianStringHasAnEnglishOne() {
        val extra = keys("src/main/res/values-it/strings.xml") - keys("src/main/res/values/strings.xml")
        assertEquals("Missing from values: $extra", emptySet<String>(), extra)
    }
}
