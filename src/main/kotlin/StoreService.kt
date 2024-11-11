package org.example

object StoreService {
    private val games = listOf(
        Game(1, "The Witcher 3", 59.99),
        Game(2, "Cyberpunk 2077", 49.99),
        Game(3, "Minecraft", 29.99),
        Game(4, "God of War: Ragnarok", 79.99),
        Game(5, "Liars Bar", 29.99),
        Game(6, "Stardew Valley", 19.99),
        Game(7, "Life is Stange", 39.99),)

    private val orders = mutableListOf<OrderResponse>()

    fun getAllGames(): List<Game> = games

    fun createOrder(orderResponse: OrderResponse) {
        orders.add(orderResponse)
    }

    fun getOrders(): List<OrderResponse> = orders

    fun getOrdersByUser(userId: Int, status: String?, sortBy: String): List<OrderResponse> {
        // Filtra por userId e aplica ordenação simples
        return orders.filter { it.userId == userId }
            .sortedWith(compareBy { if (sortBy == "id") it.id else it.totalPrice })
    }

    fun deleteOrder(orderId: Int): Boolean {
        val orderToDelete = orders.find { it.id == orderId }
        return if (orderToDelete != null) {
            orders.remove(orderToDelete)
            true
        } else {
            false
        }
    }

    fun createUser(userRequest: User): Int {
        // Criação simples de um usuário com ID auto-increment
        return userRequest.id
    }

    fun getUserById(userId: Int): User? {
        // Retorna um usuário (simulação de uma base de dados)
        return User(userId, "User $userId")
    }
}
