package io.omnipede

import org.kurento.client.KurentoClient
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@SpringBootApplication
@EnableWebSocket
class BootEngine

fun main(args: Array<String>) {
    runApplication<BootEngine>(*args)
}

@Configuration
class WebSocketConfiguration(
    private val magicMirrorHandler: MagicMirrorHandler
): WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(magicMirrorHandler, "/magicmirror")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }
}

@Configuration
class BeanConfiguration {

    @Bean
    fun kurentoClient(): KurentoClient {
        return KurentoClient.create()
    }
}
