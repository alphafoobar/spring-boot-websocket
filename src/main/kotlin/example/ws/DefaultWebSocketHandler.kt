package example.ws

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import example.model.Event
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.annotation.NonNull
import java.util.Objects

private val logger = LoggerFactory.getLogger(DefaultWebSocketHandler::class.java)

@Component
class DefaultWebSocketHandler @Autowired constructor(
    private val mapper: ObjectMapper,
    private val eventFlux: Flux<Event>
) : WebSocketHandler {

    @NonNull
    override fun handle(@NonNull webSocketSession: WebSocketSession): Mono<Void> {
        // Associate a reducer with the subscription so we don't suffer memory leaks when subscribers disconnect.
        val subscriber = Subscriber(mapper,  webSocketSession.handshakeInfo.uri.path, webSocketSession.id)
        return ReactiveSecurityContextHolder
            .getContext()
            .map { obj: SecurityContext -> obj.authentication }
            .doOnNext { authentication: Authentication -> subscriber.setAuthentication(authentication) }
            .then(
                Mono.zip(
                    subscribeInputStream(webSocketSession, subscriber),
                    subscribeOutputStream(webSocketSession, subscriber)
                ).then()
            )
    }

    private fun subscribeInputStream(webSocketSession: WebSocketSession, subscriber: Subscriber): Mono<Void> {
        return webSocketSession
            .receive()
            .map { obj: WebSocketMessage -> obj.payloadAsText }
            .doOnNext { payload: String -> subscriber.onNext(payload) }
            .doOnError { throwable: Throwable -> subscriber.onInputStreamError(throwable) }
            .doOnSubscribe { subscriber.onConnect() }
            .doFinally {
                subscriber.onComplete()
                webSocketSession.close()
            }
            .then()
    }

    private fun subscribeOutputStream(webSocketSession: WebSocketSession, subscriber: Subscriber): Mono<Void> {
        val messages = Flux
            .merge(subscriber.response(), eventFlux())
            .map { value: Event -> writeValueAsString(value) }
            .doOnError { throwable: Throwable -> subscriber.onOutputStreamError(throwable) }
            .filter { obj: String? -> Objects.nonNull(obj) }
            .map { payload: String? -> webSocketSession.textMessage((payload as String)) }
        return webSocketSession.send(messages)
    }

    private fun eventFlux(): Flux<Event> {
        return eventFlux
            .onBackpressureBuffer(1, BufferOverflowStrategy.DROP_OLDEST)
            .publish(1)
            .autoConnect()
    }

    private fun writeValueAsString(value: Event): String? {
        try {
            return mapper.writeValueAsString(value)
        } catch (exception: JsonProcessingException) {
            val message = exception.message
            logger.error("output-json-parse-error, message=\"{}\"", message, exception)
        }
        return null
    }
}
