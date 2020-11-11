package org.skull.king.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration;

class SkullKingConfig : Configuration() {

    @JsonProperty("app_name")
    var appName: String = "Toto"
}
