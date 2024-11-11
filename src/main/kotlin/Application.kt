package org.example

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(Json {

            })
        }

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
            // Endpoint de Login para gerar o JWT
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

            // Endpoints de Jogos
            route("/games") {
                get {
                    val games = StoreService.getAllGames()
                    call.respond(games)
                }
            }

            // CRUD de pedidos
            route("/orders") {
                // Criar um novo pedido
                post {
                    val orderRequest = call.receive<OrderRequest>()
                    val selectedGames = StoreService.getAllGames().filter { game ->
                        orderRequest.gameIds.contains(game.id)
                    }
                    if (selectedGames.isNotEmpty()) {
                        val totalPrice = selectedGames.sumOf { it.price }

                        val orderResponse = OrderResponse(
                            id = (StoreService.getOrders().size + 1),
                            userId = orderRequest.userId,
                            games = selectedGames,
                            totalPrice = totalPrice
                        )
                        StoreService.createOrder(orderResponse)
                        call.respond(HttpStatusCode.Created, orderResponse)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Invalid game IDs")
                    }
                }

                // Obter todos os pedidos
                get {
                    val orders = StoreService.getOrders()
                    call.respond(orders)
                }

                // Obter pedidos de um usuário com filtragem
                get("/user/{userId}") {
                    val userId = call.parameters["userId"]?.toIntOrNull()
                    val status = call.request.queryParameters["status"] // Filtrar por status (opcional)
                    val sortedBy = call.request.queryParameters["sort"] ?: "id" // Ordenar por ID por padrão

                    val orders = userId?.let {
                        StoreService.getOrdersByUser(userId, status, sortedBy)
                    } ?: emptyList()

                    if (orders.isNotEmpty()) {
                        call.respond(orders)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "No orders found")
                    }
                }

                // Deletar um pedido (com autenticação)
                delete("/{orderId}") {
                    val orderId = call.parameters["orderId"]?.toIntOrNull()
                    val authToken = call.request.headers["Authorization"]

                    // Verificar a autenticação do token JWT
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

            // Endpoints de usuários
            route("/users") {
                post {
                    val userRequest = call.receive<User>()
                    val userId = StoreService.createUser(userRequest)
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
    val totalPrice: Double
)
