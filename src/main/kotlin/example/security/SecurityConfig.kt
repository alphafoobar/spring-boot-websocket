package example.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val authenticationConverter: ServerAuthenticationConverter,
    @param:Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuerUri: String,
    @param:Value("\${spring.security.oauth2.resourceserver.jwt.audience}") private val audience: String
) {
    @Bean
    fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .csrf().disable()
            .authorizeExchange()
            .pathMatchers("/protected-ws").hasAuthority("SCOPE_read:client-ws")
            .pathMatchers("/protected").hasAuthority("SCOPE_read:client")
            .matchers(EndpointRequest.toAnyEndpoint()).permitAll()
            .anyExchange().authenticated()
            .and()
            .oauth2ResourceServer()
            .bearerTokenConverter(authenticationConverter)
            .jwt()
        return http.build()
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val jwtDecoder = ReactiveJwtDecoders.fromOidcIssuerLocation(issuerUri) as NimbusReactiveJwtDecoder
        jwtDecoder.setJwtValidator(DelegatingOAuth2TokenValidator(withIssuer(), audienceValidator()))
        return jwtDecoder
    }

    private fun withIssuer(): OAuth2TokenValidator<Jwt>? {
        return JwtValidators.createDefaultWithIssuer(
            issuerUri
        )
    }

    private fun audienceValidator(): OAuth2TokenValidator<Jwt> {
        return JwtAudienceValidator(
            audience
        )
    }
}
