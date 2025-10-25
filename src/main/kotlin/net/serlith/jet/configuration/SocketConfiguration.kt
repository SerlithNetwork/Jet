package net.serlith.jet.configuration

import com.corundumstudio.socketio.AuthorizationListener
import com.corundumstudio.socketio.AuthorizationResult
import net.serlith.jet.service.SessionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SocketConfiguration

@Autowired
constructor (
    private val sessionService: SessionService,
) {

    private final val logger = LoggerFactory.getLogger(SocketConfiguration::class.java)

    @Bean
    fun authorizationListener(): AuthorizationListener {
        this.logger.info("Configuring Socket.IO Server...")
        return AuthorizationListener { data ->
            val key = data.getSingleUrlParam("key")
            if (key == null) {
                this.logger.info("A live profiler session was attempted without providing a key")
                return@AuthorizationListener AuthorizationResult.FAILED_AUTHORIZATION
            }
            if (!this.sessionService.isProfilerLive(key)) {
                this.logger.info("Profiler for '$key' is not live to start a socket session")
                return@AuthorizationListener AuthorizationResult.FAILED_AUTHORIZATION
            }
            this.logger.info("Profiler for $key is live, starting socket session...")
            return@AuthorizationListener AuthorizationResult.SUCCESSFUL_AUTHORIZATION
        }
    }

}