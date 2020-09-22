package example

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketSecurityTest(@Autowired val restTemplate: TestRestTemplate) {

    @Test
    fun `WebSocket unauthorized user is unauthorized`() {
        val entity = restTemplate.getForEntity<String>("/protected-ws", String::class.java)
        assertThat(entity.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    @WithMockUser(authorities = ["SCOPE_read:world"])
    fun `WebSocket authorized user, without required scope is forbidden`() {
        val entity = restTemplate.getForEntity<String>("/protected-ws", String::class.java)
        assertThat(entity.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    @WithMockUser(authorities = ["SCOPE_read:client-ws"])
    fun `WebSocket Authorized user, with required scope is OK`() {
        val entity = restTemplate.getForEntity<String>("/protected-ws", String::class.java)
        // You can't open a WebSocket connection with a REST client. But this shows the request is authorized.
        assertThat(entity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
