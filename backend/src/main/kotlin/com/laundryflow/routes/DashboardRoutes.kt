package com.laundryflow.routes

import com.laundryflow.services.DashboardService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.dashboardRoutes() {
    val dashboardService = DashboardService()
    
    route("/api/dashboard") {
        get("/stats") {
            val stats = dashboardService.getDashboardStats()
            call.respond(stats)
        }
    }
}
