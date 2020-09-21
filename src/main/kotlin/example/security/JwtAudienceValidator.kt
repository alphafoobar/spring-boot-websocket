package example.security

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_REQUEST
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.util.Assert

private val INVALID_AUDIENCE = OAuth2Error(
    INVALID_REQUEST,
    "This aud claim is not equal to the configured audience",
    "https://tools.ietf.org/html/rfc6750#section-3.1"
)

/**
 * Validates the "aud" claim in a [Jwt], that is matches a configured value.
 */
class JwtAudienceValidator(private val audience: String) : OAuth2TokenValidator<Jwt> {

    /**
     * {@inheritDoc}
     */
    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        Assert.notNull(token, "token cannot be null")

        val tokenAudience = token.getClaimAsStringList(JwtClaimNames.AUD)
        return if (tokenAudience.contains(audience)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE)
        }
    }
}
