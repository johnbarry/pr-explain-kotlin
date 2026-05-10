import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import parse.WalkthroughParseException
import parse.parseWalkthrough
import render.renderHtml
import validate.Severity
import validate.validate
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.system.exitProcess

class Walkthrough : CliktCommand(name = "walkthrough") {
    override fun run() = Unit
}

class RenderCommand : CliktCommand(
    name = "render",
    help = "Render a walkthrough YAML to a single self-contained HTML file",
) {
    val input: Path by option("--input", "-i", help = "Path to walkthrough.yaml")
        .path(mustExist = true, canBeDir = false)
        .required()
    val output: Path by option("--output", "-o", help = "Path to write the HTML file")
        .path(canBeDir = false)
        .required()

    override fun run() {
        val walkthrough = try {
            parseWalkthrough(input)
        } catch (e: WalkthroughParseException) {
            echo(e.message ?: "parse failed", err = true)
            exitProcess(1)
        }

        // Validation errors block rendering — emitting HTML for an invalid
        // document would just hide the bug. Warnings flow through to stderr
        // but don't stop the build.
        val result = validate(walkthrough)
        for (issue in result.issues) {
            echo(
                "${issue.severity} ${issue.path}: ${issue.message}",
                err = issue.severity == Severity.ERROR || issue.severity == Severity.WARNING,
            )
        }
        if (result.hasErrors) {
            echo("FAILED (${result.errors.size} error(s))", err = true)
            exitProcess(1)
        }

        val html = renderHtml(walkthrough)
        output.writeText(html)
        echo("Wrote $output (${html.length} bytes)")
    }
}

class ValidateCommand : CliktCommand(
    name = "validate",
    help = "Parse a walkthrough YAML and report any schema violations",
) {
    val input: Path by argument(help = "Path to walkthrough.yaml")
        .path(mustExist = true, canBeDir = false)

    override fun run() {
        val walkthrough = try {
            parseWalkthrough(input)
        } catch (e: WalkthroughParseException) {
            echo(e.message ?: "parse failed", err = true)
            exitProcess(1)
        }

        val result = validate(walkthrough)
        // Print warnings to stdout, errors to stderr — both prefixed with severity
        // and YAML-style path so the reader can grep right to the offending node.
        for (issue in result.issues) {
            val line = "${issue.severity} ${issue.path}: ${issue.message}"
            echo(line, err = issue.severity == Severity.ERROR)
        }

        if (result.hasErrors) {
            echo("FAILED (${result.errors.size} error(s), ${result.warnings.size} warning(s))", err = true)
            exitProcess(1)
        }
        echo(
            if (result.warnings.isEmpty()) "OK"
            else "OK with ${result.warnings.size} warning(s)",
        )
    }
}

fun main(args: Array<String>) {
    Walkthrough().subcommands(RenderCommand(), ValidateCommand()).main(args)
}
