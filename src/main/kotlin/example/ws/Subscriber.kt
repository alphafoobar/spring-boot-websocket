package example.ws

import com.fasterxml.jackson.databind.ObjectMapper
import example.model.Disconnect
import example.model.Error
import example.model.Event
import example.model.Welcome
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.FluxSink
import reactor.core.publisher.FluxSink.OverflowStrategy
import java.io.IOException
import java.util.*

private val logger = LoggerFactory.getLogger(Subscriber::class.java)

class Client(private val path: String, private val id: String, private val clientId: String) {
    override fun toString(): String {
        return "(path='$path', id='$id', clientId='$clientId')"
    }
}

internal class Subscriber(private val mapper: ObjectMapper, private val path: String, private val id: String) {

    private val responseFlux: EmitterProcessor<Event> = EmitterProcessor.create(4)
    private val responseSink: FluxSink<Event> = responseFlux.sink(OverflowStrategy.DROP)

    private var client: Client? = null

    fun setAuthentication(authentication: Authentication) {
        client = Client(path, id, authentication.name)
    }

    private fun decodeRecord(payload: String): Optional<Event> {
        try {
            return Optional.ofNullable(mapper.readValue(payload, Event::class.java))
        } catch (exception: IOException) {
            logger.error("decode-record, client={}, message=\"{}\"", client, exception.message)
            responseSink.next(Error("Unable to parse received message", 400))
        }
        return Optional.empty()
    }

    fun onNext(payload: String) {
        decodeRecord(payload).ifPresent { event: Event ->
            val name = event.javaClass.simpleName
            val message = getMessage(event)
            logger.info(
                "received-message, client={}, request=\"{}\", eventType={}, message=\"{}\"",
                client, event.requestId, name, message
            )
        }
    }

    private fun getMessage(event: Event): String {
        return if (event is Error) {
            event.reason
        } else ""
    }

    fun response(): EmitterProcessor<Event> {
        return responseFlux
    }

    fun onConnect() {
        logger.info("subscriber-connect, client={}", client)
        responseSink.next(Welcome())
    }

    fun onInputStreamError(throwable: Throwable) {
        logger.error("input-stream-error, client={}, message=\"{}\"", client, throwable.message)
    }

    fun onOutputStreamError(throwable: Throwable) {
        val message = throwable.message
        logger.error("output-stream-error, client={}, message=\"{}\"", client, message, throwable)
    }

    fun onComplete() {
        logger.info("subscriber-complete, client={}", client)
    }

    fun disconnect() {
        logger.info("sending-disconnect, client={}", client)
        responseSink.next(Disconnect())
    }
}
