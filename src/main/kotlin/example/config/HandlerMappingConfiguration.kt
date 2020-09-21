package example.config

import example.ws.DefaultWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler

@Configuration
class HandlerMappingConfiguration {
    @Bean
    fun webSocketMapping(webSocketHandler: DefaultWebSocketHandler?): HandlerMapping {
        val map: MutableMap<String, WebSocketHandler?> = HashMap()
        map["/"] = webSocketHandler
        map["/protected-ws"] = webSocketHandler

        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.order = 1
        handlerMapping.urlMap = map
        return handlerMapping
    }
}
