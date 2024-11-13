package org.example

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import java.util.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(Json {})
        }


        routing {
            static("/swagger-ui") {
                resources("META-INF/resources/webjars/swagger-ui/4.15.5")
            }



            get("/swagger-json") {
                call.respondText("Aqui pode ir a documentação OpenAPI em formato JSON", ContentType.Application.Json)
        }}

        install(Authentication) {
            jwt("jwt") {
                realm = "Access to 'orders'"
                verifier(
                    JWT.require(Algorithm.HMAC256("your-secret-key"))
                        .withIssuer("ktor")
                        .build()
                )
                validate { credential ->
                    if (credential.payload.getClaim("username").asString().isNotEmpty()) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }

        routing {

            route("/login") {
                post {
                    val username = call.receive<String>()
                    val token = JWT.create()
                        .withClaim("username", username)
                        .withIssuer("ktor")
                        .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // 1 hora
                        .sign(Algorithm.HMAC256("your-secret-key"))

                    call.respond(mapOf("token" to token))
                }
            }


            route("/games") {
                get {
                    val games = StoreService.getAllGames()
                    call.respond(games)
                }
            }


            route("/orders") {
                post {
                    val orderRequest = call.receive<OrderRequest>()
                    val selectedGames = StoreService.getAllGames().filter { game ->

                        println("Pedido criado")

                        orderRequest.gameIds.contains(game.id)
                    }
                    if (selectedGames.isNotEmpty()) {
                        val totalPrice = selectedGames.sumOf { it.price }

                        val orderResponse = OrderResponse(
                            id = (StoreService.getOrders().size + 1),
                            userId = orderRequest.userId,
                            games = selectedGames,
                            totalPrice = totalPrice,
                            status = "pending"
                        )
                        StoreService.createOrder(orderResponse)
                        call.respond(HttpStatusCode.Created, orderResponse)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Invalid game IDs")
                    }
                }

                patch("/{orderId}/status") {
                    val orderId = call.parameters["orderId"]?.toIntOrNull()
                    val newStatus = call.receive<String>()

                    if (orderId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid order ID")
                        return@patch
                    }

                    val updatedOrder = StoreService.updateOrderStatus(orderId, newStatus)
                    if (updatedOrder != null) {
                        call.respond(HttpStatusCode.OK, updatedOrder)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Order not found")
                    }
                }

                get {
                    val orders = StoreService.getOrders()
                    call.respond(orders)
                }

                get("/user/{userId}") {
                    val userId = call.parameters["userId"]?.toIntOrNull()
                    val statusFilter = call.request.queryParameters["status"]
                    val sortField = call.request.queryParameters["sort"] ?: "id"

                    if (userId == null) {
                        call.respond(HttpStatusCode.BadRequest, "User ID is required")
                        return@get
                    }

                    val sortedAndFilteredOrders = StoreService.getOrdersByUser(userId, statusFilter, sortField)

                    if (sortedAndFilteredOrders.isNotEmpty()) {
                        call.respond(sortedAndFilteredOrders)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "No orders found")
                    }
                }

                delete("/{orderId}") {
                    val orderId = call.parameters["orderId"]?.toIntOrNull()
                    val authToken = call.request.headers["Authorization"]

                    if (authToken?.startsWith("Bearer ") == true) {
                        val token = authToken.removePrefix("Bearer ").trim()
                        val principal = call.authentication.principal<JWTPrincipal>()

                        if (principal != null) {
                            orderId?.let {
                                if (StoreService.deleteOrder(orderId)) {
                                    call.respond(HttpStatusCode.NoContent)
                                } else {
                                    call.respond(HttpStatusCode.NotFound, "Order not found")
                                }
                            } ?: call.respond(HttpStatusCode.BadRequest, "Invalid order ID")
                        } else {
                            call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                        }
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    }
                }
            }


            route("/users") {
                post {
                    val userRequest = call.receive<User>()
                    val userId = StoreService.createUser(userRequest)

                    println("Usuário criado: ID = $userId, Nome = ${userRequest.name}")

                    call.respond(HttpStatusCode.Created, userId)
                }

                get("/{userId}") {
                    val userId = call.parameters["userId"]?.toIntOrNull()
                    userId?.let {
                        val user = StoreService.getUserById(it)
                        if (user != null) {
                            call.respond(user)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "User not found")
                        }
                    } ?: call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                }
            }
        }
    }.start(wait = true)
}

@Serializable
data class Game(val id: Int, val name: String, val price: Double)

@Serializable
data class OrderRequest(val userId: Int, val gameIds: List<Int>)

@Serializable
data class User(val id: Int, val name: String)

@Serializable
data class OrderResponse(
    val id: Int,
    val userId: Int,
    val games: List<Game>,
    val totalPrice: Double,
    val status: String
)
