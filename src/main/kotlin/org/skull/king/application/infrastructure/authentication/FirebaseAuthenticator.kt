package org.skull.king.application.infrastructure.authentication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import io.dropwizard.auth.Authenticator
import java.security.Principal
import java.util.Optional

typealias IdToken = String

class FirebaseAuthenticator(private val firebaseAuth: FirebaseAuth) : Authenticator<IdToken, User> {

    companion object {
        const val PREFIX = "Bearer"
    }

    override fun authenticate(idToken: IdToken): Optional<User> {
        try {
            val decodedToken = firebaseAuth.verifyIdToken(idToken)
            return Optional.of(User(decodedToken.uid, decodedToken.name, decodedToken.email))
        } catch (exception: FirebaseAuthException) {
            return Optional.empty()
        }
    }
}

data class User(val id: String, val displayName: String, val email: String) : Principal {
    override fun getName(): String {
        return email
    }
}
