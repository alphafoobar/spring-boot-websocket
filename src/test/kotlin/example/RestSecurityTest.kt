package example

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestSecurityTest(@Autowired val restTemplate: TestRestTemplate) {

    @Test
    fun `Unauthorized user is unauthorized`() {
        val entity = restTemplate.getForEntity<String>("/world", String::class.java)
        assertThat(entity.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    @WithMockUser(authorities = ["SCOPE_read:world"])
    fun `Authorized user, without required scope is Forbidden`() {
        val entity = restTemplate.getForEntity<String>("/protected", String::class.java)
        assertThat(entity.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    @WithMockUser(authorities = ["SCOPE_read:client"])
    fun `Authorized user, with required scope is OK`() {
        val entity = restTemplate.getForEntity<String>("/protected", String::class.java)
        assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
    }
}
