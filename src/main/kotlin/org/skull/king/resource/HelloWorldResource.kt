package org.skull.king.resource;

import javax.ws.rs.GET
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType;

@Path("/hello-world")
@Produces(MediaType.APPLICATION_JSON)
class HelloWorldResource(private val appName: String) {

    @GET
    fun sayHello(@QueryParam("name") name: String): String {
        return "Hello $name, welcome to $appName"
    }
}
