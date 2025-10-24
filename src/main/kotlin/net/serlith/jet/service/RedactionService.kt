package net.serlith.jet.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.io.File

@Service
class RedactionService {

    private final val file = File("sensitive.yml")
    private final val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private final lateinit var regex: List<Regex>

    data class SensitiveEntries(val entries: List<String> = listOf(
        "database",
        "bungeecord-addresses",
        "secret",
        "server-ip",
        "motd",
        "resource-pack",
        "rcon\\.password",
        "rcon\\.ip",
        "management-server-secret",
        "management-server-tls-keystore-password",
        "level-seed",
        "feature-seeds",
        "token",
        "sentry-dsn",
    ))

    @PostConstruct
    fun init() {
        if (!this.file.exists()) {
            this.createDefaultFile()
        }
        this.loadFile()
    }

    private final fun createDefaultFile() {
        val default = SensitiveEntries()
        this.mapper.writeValue(this.file, default)
    }

    private final fun loadFile() {
        val input: SensitiveEntries = this.mapper.readValue(this.file)
        this.regex = input.entries.map { Regex("(?im)^([ \\t]*$it[ \\t]*[:=])([ \\t]*.*)?$", RegexOption.MULTILINE) }
    }

    final fun sanitize(str: String): String {
        var content = str
        this.regex.forEach { r ->
            content = content.replace(r) {
                val key = it.groupValues[1]
                return@replace "$key *** REDACTED ***"
            }
        }
        return content
    }

}