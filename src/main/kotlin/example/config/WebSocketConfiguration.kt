package example.config

import example.model.Event
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.server.resource.web.server.ServerBearerTokenAuthenticationConverter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.FluxSink
import reactor.core.publisher.FluxSink.OverflowStrategy
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.util.concurrent.Queues

@Configuration
class WebSocketConfiguration {

    @Bean
    fun streamScheduler(): Scheduler {
        return Schedulers.newParallel("ws-handler")
    }

    @Bean
    fun eventProcessor(): EmitterProcessor<Event> {
        return EmitterProcessor.create(Queues.ceilingNextPowerOfTwo(1000), false)
    }

    @Bean
    fun fluxSink(emitterProcessor: EmitterProcessor<Event?>): FluxSink<Event?> {
        return emitterProcessor.sink(OverflowStrategy.LATEST)
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }

    @Bean
    fun authenticationConverter(): ServerAuthenticationConverter {
        val authenticationConverter = ServerBearerTokenAuthenticationConverter()
        authenticationConverter.setAllowUriQueryParameter(true)
        return authenticationConverter
    }
}
