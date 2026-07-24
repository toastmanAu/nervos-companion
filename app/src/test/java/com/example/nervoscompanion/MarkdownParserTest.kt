package com.example.nervoscompanion

import com.example.nervoscompanion.data.MarkdownParser
import com.example.nervoscompanion.data.RfcMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MarkdownParserTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun testFrontmatterParsing() {
    val mdContent = """
      ---
      Number: "0021"
      Category: Standards Track
      Status: Active
      Author: Cipher Wang <cipher@nervina.io>
      Created: 2019-01-20
      ---
      
      # CKB Address Format
      
      This is abstract info.
    """.trimIndent()

    val file = tempFolder.newFile("0021-test.md")
    file.writeText(mdContent)

    val metadata = MarkdownParser.parseFrontmatterAndTitle(file)

    assertEquals("0021", metadata.number)
    assertEquals("Standards Track", metadata.category)
    assertEquals("Active", metadata.status)
    assertEquals("Cipher Wang <cipher@nervina.io>", metadata.author)
    assertEquals("2019-01-20", metadata.created)
    assertEquals("CKB Address Format", metadata.title)
  }

  @Test
  fun testMarkdownToHtmlConversion() {
    val markdown = """
      # CKB Address Format
      
      This is a **bold** and *italic* text. Here is `inline code`.
      
      > This is a blockquote.
      
      * Item 1
      * Item 2
      
      | format type | description |
      |:-----------:|-------------|
      | 0x00 | full version |
      | 0x01 | short version |
      
      ```c
      payload = 0x00 | code_hash
      ```
    """.trimIndent()

    val metadata = RfcMetadata(
      number = "0021",
      category = "Standards",
      status = "Active",
      author = "Cipher",
      created = "2019"
    )

    val html = MarkdownParser.toHtml(markdown, metadata)

    // Verify metadata block is present
    assertTrue(html.contains("RFC Number"))
    assertTrue(html.contains("0021"))

    // Verify headers
    assertTrue(html.contains("<h1>CKB Address Format</h1>"))

    // Verify formatting
    assertTrue(html.contains("<strong>bold</strong>"))
    assertTrue(html.contains("<em>italic</em>"))
    assertTrue(html.contains("<code>inline code</code>"))

    // Verify blockquote
    assertTrue(html.contains("<blockquote>This is a blockquote.</blockquote>"))

    // Verify lists
    assertTrue(html.contains("<ul>"))
    assertTrue(html.contains("<li>Item 1</li>"))
    assertTrue(html.contains("<li>Item 2</li>"))
    assertTrue(html.contains("</ul>"))

    // Verify table
    assertTrue(html.contains("<table>"))
    assertTrue(html.contains("<th>format type</th>"))
    assertTrue(html.contains("<th>description</th>"))
    assertTrue(html.contains("<td>0x00</td>"))
    assertTrue(html.contains("<td>full version</td>"))
    assertTrue(html.contains("</table>"))

    // Verify code block
    assertTrue(html.contains("<pre><code class=\"language-c\">payload = 0x00 | code_hash"))
    assertTrue(html.contains("</code></pre>"))
  }
}
