package net.serlith.jet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class JetApplication

fun main(args: Array<String>) {
    runApplication<JetApplication>(*args)
}
