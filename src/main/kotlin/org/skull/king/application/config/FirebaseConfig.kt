package org.skull.king.application.config

import com.fasterxml.jackson.annotation.JsonProperty

class FirebaseConfig {

    @JsonProperty("database_url")
    lateinit var databaseURL: String

    @JsonProperty("service_account")
    lateinit var serviceAccount: FirebaseServiceAccount

    class FirebaseServiceAccount {
        @JsonProperty("type")
        lateinit var type: String

        @JsonProperty("project_id")
        lateinit var projectId: String

        @JsonProperty("private_key_id")
        lateinit var privateKeyId: String

        @JsonProperty("private_key")
        lateinit var privateKey: String

        @JsonProperty("client_email")
        lateinit var clientEmail: String

        @JsonProperty("client_id")
        lateinit var clientId: String

        @JsonProperty("auth_uri")
        lateinit var authUri: String

        @JsonProperty("token_uri")
        lateinit var tokenUri: String

        @JsonProperty("auth_provider_x509_cert_url")
        lateinit var authProviderCertUrl: String

        @JsonProperty("client_x509_cert_url")
        lateinit var clientCertUrl: String
    }
}
