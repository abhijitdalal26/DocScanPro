package com.abhijit.docscanpro.utils

import com.google.mlkit.vision.text.Text

class MarkdownExporter {

    fun convertToMarkdown(ocrText: String, documentName: String): String {
        val lines = ocrText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val sb = StringBuilder()
        sb.appendLine("# $documentName")
        sb.appendLine()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val nextLine = lines.getOrNull(i + 1)
            when {
                isHeading(line) -> sb.appendLine("## $line")
                isBulletPoint(line) -> sb.appendLine("- ${cleanBullet(line)}")
                isNumberedItem(line) -> sb.appendLine(line)
                isTableStart(line, nextLine) -> {
                    val tableLines = mutableListOf<String>()
                    var j = i
                    while (j < lines.size && isTableRow(lines[j])) {
                        tableLines.add(lines[j])
                        j++
                    }
                    sb.append(buildMarkdownTable(tableLines))
                    i = j
                    continue
                }
                else -> {
                    sb.appendLine(line)
                    sb.appendLine()
                }
            }
            i++
        }
        return sb.toString().trimEnd()
    }

    fun convertFromVisionText(visionText: Text, documentName: String): String {
        val sb = StringBuilder()
        sb.appendLine("# $documentName")
        sb.appendLine()

        for (block in visionText.textBlocks) {
            val blockLines = block.lines
            if (blockLines.size == 1 && isHeading(blockLines[0].text)) {
                sb.appendLine("## ${blockLines[0].text}")
            } else {
                for (line in blockLines) {
                    val text = line.text.trim()
                    when {
                        text.isBlank() -> Unit
                        isBulletPoint(text) -> sb.appendLine("- ${cleanBullet(text)}")
                        isNumberedItem(text) -> sb.appendLine(text)
                        else -> sb.appendLine(text)
                    }
                }
                sb.appendLine()
            }
        }
        return sb.toString().trimEnd()
    }

    private fun isHeading(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.length in 3..70 &&
            trimmed.uppercase() == trimmed &&
            trimmed.any { it.isLetter() } &&
            !trimmed.endsWith(".")
    }

    private fun isBulletPoint(line: String): Boolean {
        return line.startsWith("•") || line.startsWith("–") ||
            line.startsWith("►") || line.startsWith("→") ||
            (line.startsWith("- ") && !isNumberedItem(line))
    }

    private fun isNumberedItem(line: String): Boolean {
        return Regex("""^\d+[.)]\s+\S""").containsMatchIn(line)
    }

    private fun isTableRow(line: String): Boolean {
        return line.contains("  ") && line.split(Regex("""\s{2,}""")).size >= 2
    }

    private fun isTableStart(line: String, nextLine: String?): Boolean {
        return isTableRow(line) && nextLine != null && isTableRow(nextLine)
    }

    private fun buildMarkdownTable(lines: List<String>): String {
        if (lines.isEmpty()) return ""
        val columns = lines.map { row ->
            row.trim().split(Regex("""\s{2,}""")).map { it.trim() }
        }
        val maxCols = columns.maxOf { it.size }
        val sb = StringBuilder()

        // Header row
        val header = columns[0].padEnd(maxCols)
        sb.appendLine("| ${header.joinToString(" | ")} |")
        sb.appendLine("| ${List(maxCols) { "---" }.joinToString(" | ")} |")

        // Data rows
        for (row in columns.drop(1)) {
            sb.appendLine("| ${row.padEnd(maxCols).joinToString(" | ")} |")
        }
        sb.appendLine()
        return sb.toString()
    }

    private fun cleanBullet(line: String): String =
        line.trimStart('•', '–', '►', '→', '-').trim()

    private fun List<String>.padEnd(size: Int): List<String> {
        return if (this.size >= size) this else this + List(size - this.size) { "" }
    }
}
