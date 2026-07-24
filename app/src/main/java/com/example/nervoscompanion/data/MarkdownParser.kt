package com.example.nervoscompanion.data

import java.io.File

data class RfcMetadata(
  val number: String = "",
  val category: String = "",
  val status: String = "",
  val author: String = "",
  val created: String = "",
  val title: String = ""
)

object MarkdownParser {

  fun parseFrontmatterAndTitle(file: File): RfcMetadata {
    var number = ""
    var category = ""
    var status = ""
    var author = ""
    var created = ""
    var title = ""

    try {
      file.bufferedReader().use { reader ->
        var line = reader.readLine()?.trim() ?: ""
        var inFrontmatter = false
        var linesRead = 0
        
        if (line == "---") {
          inFrontmatter = true
          while (linesRead < 25) {
            line = reader.readLine()?.trim() ?: break
            linesRead++
            if (line == "---") {
              break
            }
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
              val key = parts[0].trim().lowercase()
              val value = parts[1].replace("\"", "").trim()
              when (key) {
                "number" -> number = value
                "category" -> category = value
                "status" -> status = value
                "author" -> author = value
                "created" -> created = value
              }
            }
          }
        }
        
        // Find title: first non-empty line starting with #
        var count = 0
        while (count < 50) {
          line = reader.readLine()?.trim() ?: break
          count++
          if (line.startsWith("#")) {
            title = line.removePrefix("#").trim()
            break
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    
    // If number is empty, try to parse it from the filename or folder name
    if (number.isEmpty()) {
      val name = file.parentFile?.name ?: file.name
      val numMatch = "^\\d+".toRegex().find(name)
      if (numMatch != null) {
        number = numMatch.value
      }
    }
    if (title.isEmpty()) {
      title = file.parentFile?.name?.replace("-", " ")?.replaceFirstChar { it.uppercase() } ?: file.nameWithoutExtension
    }

    return RfcMetadata(number, category, status, author, created, title)
  }

  fun toHtml(markdown: String, metadata: RfcMetadata): String {
    val lines = markdown.lines()
    val html = StringBuilder()
    var inCodeBlock = false
    var inList = false
    var inOrderedList = false
    var inTable = false
    var codeLang = ""

    html.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>")
    html.append("""
      body {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        line-height: 1.6;
        color: #E2E8F0;
        background-color: #0B0F13;
        padding: 16px;
        margin: 0;
      }
      h1, h2, h3, h4, h5, h6 {
        color: #F8FAFC;
        margin-top: 24px;
        margin-bottom: 16px;
        font-weight: 700;
        letter-spacing: -0.025em;
      }
      h1 { border-bottom: 1px solid #1F2E3A; padding-bottom: 8px; font-size: 1.6em; color: #38EF7D; }
      h2 { border-bottom: 1px solid #1F2E3A; padding-bottom: 6px; font-size: 1.3em; color: #00F2FE; }
      h3 { font-size: 1.15em; }
      h4 { font-size: 1.05em; }
      p { margin-top: 0; margin-bottom: 16px; color: #94A3B8; font-size: 1.0em; }
      a { color: #38EF7D; text-decoration: none; font-weight: 500; }
      a:hover { text-decoration: underline; color: #00F2FE; }
      code {
        font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
        font-size: 0.85em;
        background-color: #1E293B;
        padding: 2px 6px;
        border-radius: 4px;
        color: #38EF7D;
      }
      pre {
        background-color: #05080C;
        padding: 16px;
        border-radius: 8px;
        overflow-x: auto;
        border: 1px solid #1F2E3A;
        margin-bottom: 16px;
      }
      pre code {
        background-color: transparent;
        padding: 0;
        border-radius: 0;
        color: #E2E8F0;
      }
      table {
        border-collapse: collapse;
        width: 100%;
        margin-bottom: 24px;
        font-size: 0.9em;
      }
      th, td {
        border: 1px solid #1F2E3A;
        padding: 10px 14px;
        text-align: left;
      }
      th {
        background-color: #112521;
        color: #38EF7D;
        font-weight: bold;
      }
      tr:nth-child(even) {
        background-color: #0F161E;
      }
      tr:nth-child(odd) {
        background-color: #0B0F13;
      }
      blockquote {
        margin: 0 0 20px 0;
        padding: 12px 20px;
        border-left: 4px solid #11998E;
        background-color: #0F161E;
        border-radius: 0 8px 8px 0;
        color: #94A3B8;
      }
      ul, ol {
        margin-top: 0;
        margin-bottom: 20px;
        padding-left: 24px;
        color: #94A3B8;
      }
      li {
        margin-bottom: 8px;
      }
      img {
        max-width: 100%;
        height: auto;
        border-radius: 8px;
        margin: 20px 0;
        background-color: #0F161E;
        border: 1px solid #1F2E3A;
        padding: 8px;
      }
      .meta-box {
        background: linear-gradient(135deg, #112521 0%, #0F161E 100%);
        border: 1px solid #11998E;
        border-radius: 12px;
        padding: 16px 20px;
        margin-bottom: 30px;
        font-size: 0.85em;
      }
      .meta-row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 6px;
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        padding-bottom: 4px;
      }
      .meta-row:last-child {
        border-bottom: none;
        margin-bottom: 0;
        padding-bottom: 0;
      }
      .meta-label {
        color: #94A3B8;
        font-weight: 500;
      }
      .meta-value {
        color: #F8FAFC;
        font-weight: 600;
        text-align: right;
      }
      .status-active { color: #38EF7D; }
      .status-draft { color: #FFA726; }
      .status-deprecated { color: #EF5350; }
    """.trimIndent())
    html.append("</style></head><body>")

    val statusClass = when (metadata.status.lowercase()) {
      "active" -> "status-active"
      "draft" -> "status-draft"
      "deprecated" -> "status-deprecated"
      else -> ""
    }
    html.append("<div class=\"meta-box\">")
    html.append("<div class=\"meta-row\"><span class=\"meta-label\">RFC Number</span><span class=\"meta-value\">${metadata.number}</span></div>")
    html.append("<div class=\"meta-row\"><span class=\"meta-label\">Category</span><span class=\"meta-value\">${metadata.category}</span></div>")
    html.append("<div class=\"meta-row\"><span class=\"meta-label\">Status</span><span class=\"meta-value $statusClass\">${metadata.status}</span></div>")
    html.append("<div class=\"meta-row\"><span class=\"meta-label\">Author</span><span class=\"meta-value\">${metadata.author}</span></div>")
    html.append("<div class=\"meta-row\"><span class=\"meta-label\">Created</span><span class=\"meta-value\">${metadata.created}</span></div>")
    html.append("</div>")

    // Find the first line after frontmatter
    var startLineIndex = 0
    if (lines.isNotEmpty() && lines[0] == "---") {
      var endIdx = -1
      for (idx in 1 until lines.size) {
        if (lines[idx] == "---") {
          endIdx = idx
          break
        }
      }
      if (endIdx != -1) {
        startLineIndex = endIdx + 1
      }
    }

    var i = startLineIndex
    while (i < lines.size) {
      val rawLine = lines[i]
      val line = rawLine.trim()

      // Code blocks
      if (line.startsWith("```")) {
        if (inCodeBlock) {
          html.append("</code></pre>\n")
          inCodeBlock = false
        } else {
          codeLang = line.substring(3).trim()
          html.append("<pre><code class=\"language-$codeLang\">")
          inCodeBlock = true
        }
        i++
        continue
      }

      if (inCodeBlock) {
        val escaped = rawLine
          .replace("&", "&amp;")
          .replace("<", "&lt;")
          .replace(">", "&gt;")
        html.append(escaped).append("\n")
        i++
        continue
      }

      // Tables
      if (line.startsWith("|")) {
        if (inList) { html.append("</ul>\n"); inList = false }
        if (inOrderedList) { html.append("</ol>\n"); inOrderedList = false }

        if (!inTable) {
          html.append("<table>\n")
          inTable = true
          val cols = parseTableCols(line)
          html.append("<thead>\n<tr>\n")
          for (c in cols) {
            html.append("<th>").append(renderInline(c)).append("</th>\n")
          }
          html.append("</tr>\n</thead>\n<tbody>\n")

          if (i + 1 < lines.size && lines[i + 1].trim().startsWith("|") && lines[i + 1].contains("---")) {
            i++
          }
        } else {
          val cols = parseTableCols(line)
          if (!line.contains("---")) {
            html.append("<tr>\n")
            for (c in cols) {
              html.append("<td>").append(renderInline(c)).append("</td>\n")
            }
            html.append("</tr>\n")
          }
        }
        i++
        continue
      } else {
        if (inTable) {
          html.append("</tbody>\n</table>\n")
          inTable = false
        }
      }

      // Blockquotes
      if (line.startsWith(">")) {
        if (inList) { html.append("</ul>\n"); inList = false }
        if (inOrderedList) { html.append("</ol>\n"); inOrderedList = false }
        val content = line.substring(1).trim()
        html.append("<blockquote>").append(renderInline(content)).append("</blockquote>\n")
        i++
        continue
      }

      // Unordered Lists
      if (line.startsWith("* ") || line.startsWith("- ") || line.startsWith("+ ")) {
        if (inOrderedList) { html.append("</ol>\n"); inOrderedList = false }
        if (!inList) {
          html.append("<ul>\n")
          inList = true
        }
        val content = line.substring(2).trim()
        html.append("<li>").append(renderInline(content)).append("</li>\n")
        i++
        continue
      }

      // Ordered Lists
      val orderedMatch = "^(\\d+)\\.\\s+(.*)$".toRegex().find(line)
      if (orderedMatch != null) {
        if (inList) { html.append("</ul>\n"); inList = false }
        if (!inOrderedList) {
          html.append("<ol>\n")
          inOrderedList = true
        }
        val content = orderedMatch.groupValues[2].trim()
        html.append("<li>").append(renderInline(content)).append("</li>\n")
        i++
        continue
      }

      if (line.isEmpty()) {
        if (inList) { html.append("</ul>\n"); inList = false }
        if (inOrderedList) { html.append("</ol>\n"); inOrderedList = false }
        i++
        continue
      }

      // Headers
      if (line.startsWith("#")) {
        if (inList) { html.append("</ul>\n"); inList = false }
        if (inOrderedList) { html.append("</ol>\n"); inOrderedList = false }

        val level = line.takeWhile { it == '#' }.length
        val titleContent = line.drop(level).trim()
        html.append("<h$level>").append(renderInline(titleContent)).append("</h$level>\n")
        i++
        continue
      }

      // Paragraph
      if (inList) { html.append("</ul>\n"); inList = false }
      if (inOrderedList) { html.append("</ol>\n"); inOrderedList = false }

      html.append("<p>").append(renderInline(line)).append("</p>\n")
      i++
    }

    if (inCodeBlock) html.append("</code></pre>\n")
    if (inTable) html.append("</tbody></table>\n")
    if (inList) html.append("</ul>\n")
    if (inOrderedList) html.append("</ol>\n")

    html.append("</body></html>")
    return html.toString()
  }

  private fun parseTableCols(line: String): List<String> {
    val clean = line.trim().removePrefix("|").removeSuffix("|")
    return clean.split("|").map { it.trim() }
  }

  private fun renderInline(text: String): String {
    var out = text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")

    // Images: ![caption](src)
    out = """!\[([^\]]*)\]\(([^)]+)\)""".toRegex().replace(out) { match ->
      val alt = match.groupValues[1]
      val src = match.groupValues[2]
      "<img src=\"$src\" alt=\"$alt\" />"
    }

    // Links: [text](url)
    out = """\[([^\]]+)\]\(([^)]+)\)""".toRegex().replace(out) { match ->
      val linkText = match.groupValues[1]
      val href = match.groupValues[2]
      "<a href=\"$href\">$linkText</a>"
    }

    // Bold: **text**
    out = """\*\*([^*]+)\*\*""".toRegex().replace(out) { match ->
      "<strong>${match.groupValues[1]}</strong>"
    }

    // Italic: *text* or _text_
    out = """\*([^*]+)\*""".toRegex().replace(out) { match ->
      "<em>${match.groupValues[1]}</em>"
    }
    out = """_([^_]+)_""".toRegex().replace(out) { match ->
      "<em>${match.groupValues[1]}</em>"
    }

    // Inline code: `code`
    out = """`([^`]+)`""".toRegex().replace(out) { match ->
      "<code>${match.groupValues[1]}</code>"
    }

    return out
  }
}
