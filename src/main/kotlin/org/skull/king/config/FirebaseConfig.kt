package org.skull.king.config

import com.fasterxml.jackson.annotation.JsonProperty

class FirebaseConfig {


    @JsonProperty("credentials_path")
    lateinit var credentialsPath: String

    @JsonProperty("database_url")
    lateinit var databaseURL: String
}
