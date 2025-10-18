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

    private val config = File("tokens.yml")
    private val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private var tokens: Set<Token> = emptySet()

    data class Token(val key: String, val owner: String)
    data class Tokens(val tokens: List<Token> = listOf(Token("default_token", "you")))

    @PostConstruct
    fun init() {
        if (!this.config.exists()) this.createDefaultConfig()
        this.loadConfig()
    }

    private fun createDefaultConfig() {
        val default = Tokens()
        this.objectMapper.writeValue(this.config, default)
    }

    private fun loadConfig() {
        val input: Tokens = this.objectMapper.readValue(this.config)
        this.tokens = input.tokens.toSet()
    }

    fun isValid(token: String?): Boolean = this.tokens.any { i -> i.key == token }

    fun getOwner(token: String?): String? = this.tokens.firstOrNull { i -> i.key == token }?.owner

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    private fun performUpdateConfig() {
        this.loadConfig()
    }

}