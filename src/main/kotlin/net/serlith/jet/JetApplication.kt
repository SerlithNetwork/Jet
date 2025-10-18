package net.serlith.jet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JetApplication

fun main(args: Array<String>) {
    runApplication<JetApplication>(*args)
}
