package parse

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.Serializable
import model.Walkthrough
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * kaml configured to:
 *  - translate snake_case YAML keys (audience_hint, structural_changes, line_hint, …)
 *    to camelCase Kotlin properties via [YamlNamingStrategy.SnakeCase].
 *  - tolerate unknown keys (forward-compatible: if the generator prompt grows new
 *    fields, an older renderer should still load the document).
 */
private val yaml: Yaml = Yaml(
    configuration = YamlConfiguration(
        yamlNamingStrategy = YamlNamingStrategy.SnakeCase,
        strictMode = false,
    ),
)

class WalkthroughParseException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

/**
 * Parse a walkthrough YAML file into a [Walkthrough].
 *
 * Accepts both shapes the generator produces:
 *   1. a top-level `walkthrough:` wrapper around the object, and
 *   2. the [Walkthrough] object directly.
 *
 * Tries the wrapped form first because that's what the model is prompted to emit.
 */
fun parseWalkthrough(path: Path): Walkthrough {
    val source = Files.readString(path)
    return try {
        parseWalkthrough(source)
    } catch (e: WalkthroughParseException) {
        throw e
    } catch (e: Exception) {
        throw WalkthroughParseException(
            "failed to read walkthrough YAML at ${path.absolutePathString()}: ${e.message}",
            e,
        )
    }
}

internal fun parseWalkthrough(source: String): Walkthrough {
    val firstError: Throwable
    try {
        return yaml.decodeFromString(WalkthroughDocument.serializer(), source).walkthrough
    } catch (e: Exception) {
        firstError = e
    }
    return try {
        yaml.decodeFromString(Walkthrough.serializer(), source)
    } catch (second: Exception) {
        // Surface the wrapped-form error since that's the canonical shape; the
        // unwrapped attempt is a fallback for hand-authored fixtures.
        throw WalkthroughParseException(
            "failed to parse walkthrough YAML: ${firstError.message}",
            firstError,
        )
    }
}

@Serializable
internal data class WalkthroughDocument(val walkthrough: Walkthrough)
