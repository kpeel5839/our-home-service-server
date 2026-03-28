package com.ourhome.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class OurHomeServiceApplication

fun main(args: Array<String>) {
    runApplication<OurHomeServiceApplication>(*args)
}
