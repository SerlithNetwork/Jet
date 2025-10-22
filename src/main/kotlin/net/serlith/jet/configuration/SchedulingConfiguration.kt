package net.serlith.jet.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@Configuration
class SchedulingConfiguration : SchedulingConfigurer {

    override fun configureTasks(registrar: ScheduledTaskRegistrar) {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.setThreadNamePrefix("Jet Spring Scheduling - ")
        scheduler.poolSize = 4
        scheduler.initialize()
        registrar.setScheduler(scheduler)
    }

}