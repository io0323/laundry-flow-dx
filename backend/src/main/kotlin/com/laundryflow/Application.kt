package com.laundryflow

import com.laundryflow.models.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.laundryflow.routes.*
import java.io.File

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 1. Initialize SQLite Database
    val dbPath = "laundryflow.db"
    Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")
    
    // Auto-migrate tables
    transaction {
        SchemaUtils.create(Customers, Orders, OrderItems)
    }

    // 2. Configure Plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        // Explicitly allow Vite frontend
        allowHost("localhost:5173", schemes = listOf("http", "https"))
    }

    // 3. Routing
    routing {
        get("/") {
            call.respondText("LaundryFlow API is running")
        }
        
        customerRoutes()
        orderRoutes()
    }
}
