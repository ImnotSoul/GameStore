package org.example

object StoreService {
    private val orders = mutableListOf<OrderResponse>()
    private val games = listOf(
        Game(1, "The Witcher 3", 59.99),
        Game(2, "Cyberpunk 2077", 49.99),
        Game(3, "Minecraft", 29.99),
        Game(4, "God of War: Ragnarok", 79.99),
        Game(5, "Liars Bar", 29.99),
        Game(6, "Stardew Valley", 19.99),
        Game(7, "Life is Stange", 39.99),
        )


    // Função para criar um pedido
    fun createOrder(order: OrderResponse) {
        orders.add(order)
    }

    // Função para obter todos os pedidos
    fun getOrders(): List<OrderResponse> {
        return orders
    }

    // Função para obter pedidos por usuário com filtragem e ordenação
    fun getOrdersByUser(userId: Int, status: String? = null, sortBy: String = "id"): List<OrderResponse> {
        val filteredOrders = if (status != null) {
            orders.filter { it.userId == userId && it.status.equals(status, ignoreCase = true) }
        } else {
            orders.filter { it.userId == userId }
        }

        return when (sortBy.lowercase()) {
            "totalprice" -> filteredOrders.sortedBy { it.totalPrice }
            "userid" -> filteredOrders.sortedBy { it.userId }
            else -> filteredOrders.sortedBy { it.id }
        }
    }

    // Função para atualizar o status de um pedido
    fun updateOrderStatus(orderId: Int, newStatus: String): OrderResponse? {
        val order = orders.find { it.id == orderId }
        return if (order != null) {
            val updatedOrder = order.copy(status = newStatus)
            orders[orders.indexOf(order)] = updatedOrder
            updatedOrder
        } else {
            null
        }
    }

    // Função para deletar um pedido
    fun deleteOrder(orderId: Int): Boolean {
        val order = orders.find { it.id == orderId }
        return if (order != null) {
            orders.remove(order)
            true
        } else {
            false
        }
    }

    // Função para obter todos os jogos
    fun getAllGames(): List<Game> = games

    // Função para criar um usuário (simulada para o exemplo)
    fun createUser(user: User): Int {
        return user.id
    }

    // Função para obter um usuário por ID (simulada para o exemplo)
    fun getUserById(userId: Int): User? {
        return User(userId, "User$userId")
    }
}
