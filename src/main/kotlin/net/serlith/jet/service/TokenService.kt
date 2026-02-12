package net.serlith.jet.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit

@Service
class TokenService {

    private final val config = File("config")
    private final val tokensFile = File(this.config, "tokens.yml")
    private final val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private final var tokens: Set<Token> = emptySet()

    data class Token(val key: String, val owner: String)
    data class Tokens(val tokens: List<Token> = listOf(Token("default_token", "you")))

    @PostConstruct
    fun init() {
        this.config.mkdirs()
        if (!this.tokensFile.exists()) {
            this.createDefaultConfig()
        }
        this.loadConfig()
    }

    private final fun createDefaultConfig() {
        val default = Tokens()
        this.objectMapper.writeValue(this.tokensFile, default)
    }

    private final fun loadConfig() {
        val input: Tokens = this.objectMapper.readValue(this.tokensFile)
        this.tokens = input.tokens.toSet()
    }

    final fun isInstanceOpen(): Boolean {
        return this.tokens.isEmpty()
    }

    final fun isValid(token: String?): Boolean {
        if (this.isInstanceOpen()) return true
        return this.tokens.any { i -> i.key == token }
    }

    final fun getOwner(token: String?): String? = this.tokens.firstOrNull { i -> i.key == token }?.owner

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    private fun performUpdateConfig() {
        this.loadConfig()
    }

}