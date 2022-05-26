package it.polito.wa2.lab5.group09.ticketcatalogueservice

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.lab5.group09.ticketcatalogueservice.entities.Order
import it.polito.wa2.lab5.group09.ticketcatalogueservice.entities.Status
import it.polito.wa2.lab5.group09.ticketcatalogueservice.entities.TicketCatalogue
import it.polito.wa2.lab5.group09.ticketcatalogueservice.repositories.OrderRepository
import it.polito.wa2.lab5.group09.ticketcatalogueservice.repositories.TicketCatalogueRepository
import it.polito.wa2.lab5.group09.ticketcatalogueservice.security.Role
import it.polito.wa2.lab5.group09.ticketcatalogueservice.services.TicketCatalogueService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@SpringBootTest
class ServiceTest {
    @Autowired
    lateinit var ticketCatalogueRepository: TicketCatalogueRepository
    @Autowired
    lateinit var orderRepository: OrderRepository
    @Autowired
    lateinit var ticketCatalogueService : TicketCatalogueService
    private final var _keyUser = "laboratorio4webapplications2ProfessorGiovanniMalnati"
    private final val ticketCatalogue =
        TicketCatalogue(
            type = "Seasonal",
            price = 30.0f,
            zones = "ABC",
            maxAge = 100,
            minAge = 10)
    private final val order = Order(status = Status.PENDING, ticketCatalogueId = 1, quantity = 1, customerUsername = "usernameTest")

    fun generateUserToken(
        key: String,
        sub: String? = "usernameTest",
        exp: Date? = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))
    ): String {
        return Jwts.builder()
            .setSubject(sub)
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(exp)
            .claim("role", Role.CUSTOMER)
            .signWith(Keys.hmacShaKeyFor(key.toByteArray())).compact()
    }

    fun generateAdminToken(
        key: String,
        sub: String? = "adminUsernameTest",
        exp: Date? = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))
    ): String {
        return Jwts.builder()
            .setSubject(sub)
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(exp)
            .claim("role", Role.ADMIN)
            .signWith(Keys.hmacShaKeyFor(key.toByteArray())).compact()
    }

    @BeforeEach
     fun createTicketCatalogueAndOrder(){
        runBlocking {
            ticketCatalogueRepository.save(ticketCatalogue)
            orderRepository.save(order)
        }
    }

  /*  @Test
    @WithMockUser(username = "usernameTest", password = "pwd", roles = ["CUSTOMER"])
    fun getOrders(){
        runBlocking {
            val orders = ticketCatalogueService.getOrders(generateUserToken(_keyUser))
            Assert.assertEquals(1, orders.first().ticketCatalogueId)
            Assert.assertEquals(1, orders.first().quantity)
            Assert.assertEquals("usernameTest", orders.first().customerUsername)
        }
    }*/

    @Test
    @WithMockUser(username = "adminUsernameTest", password = "pwd", roles = ["ADMIN"])
    fun getOrdersByAdmin(){
       runBlocking {
           val orders=ticketCatalogueService.getUserOrders("usernameTest")
           Assert.assertEquals(1, orders.first().ticketCatalogueId)
           Assert.assertEquals(1, orders.first().quantity)
           Assert.assertEquals("usernameTest", orders.first().customerUsername)
        }
    }
    @Test
    @WithMockUser(username = "usernameTest", password = "pwd", roles = ["CUSTOMER"])
    fun getOrdersByUserId(){
        runBlocking {
            lateinit var orderId : UUID
            orderRepository.findAll().collect {  orderId = it.orderId!! }
            val order = ticketCatalogueService.getOrderByUUID(orderId, generateUserToken(_keyUser))
            Assertions.assertEquals(order.orderId,orderId)
        }
    }

   /* @Test
    @WithMockUser(username = "usernameTest", password = "pwd", roles = ["CUSTOMER"])
    fun getOrdersByUUID(){
        runBlocking {
            lateinit var orderId : UUID
            orderRepository.findAll().collect {  orderId = it.orderId!! }
            val order = ticketCatalogueService.getOrderByUUID(orderId, generateUserToken(_keyUser))
            Assertions.assertEquals(order.orderId,orderId)
        }
    }*/

    @Test
    @WithMockUser(username = "usernameTest", password = "pwd", roles = ["CUSTOMER"])
    fun getOrderByUUIDNotAllowed(){
            Assertions.assertThrows(IllegalArgumentException::class.java){
                runBlocking {
                    lateinit var orderId : UUID
                    orderRepository.findAll().collect {  orderId = it.orderId!! }
                    ticketCatalogueService.getOrderByUUID(orderId, generateAdminToken(_keyUser))
                }
            }
    }

    @Test
    @WithMockUser(username = "usernameTest", password = "pwd", roles = ["CUSTOMER"])
    fun getCatalogue(){
        runBlocking {
            val catalogue = ticketCatalogueService.getCatalogue()
            Assertions.assertEquals(catalogue.count(),1)
        }
    }

    //TODO FAI LA SHOP

    @Test
    @WithMockUser(username = "usernameTest", password = "pwd", roles = ["CUSTOMER"])
    fun addTicketCatalogue(){
        runBlocking {
           ticketCatalogueService.addTicketToCatalogue(   TicketCatalogue(
               type = "School",
               price = 30.0f,
               zones = "ABC",
               maxAge = 20,
               minAge = 6))
           val tickets=ticketCatalogueService.getCatalogue()
            Assert.assertEquals("School", tickets.last().type)
            Assert.assertEquals(30.0f, tickets.last().price)
            Assert.assertEquals("ABC", tickets.last().zones)
            Assert.assertEquals(20, tickets.last().maxAge)
            Assert.assertEquals(6, tickets.last().minAge)
        }
    }


    @AfterEach
    fun deleteTicketCatalogueAndOrder(){
        runBlocking {
            ticketCatalogueRepository.findAll().last().also {
                ticketCatalogueRepository.delete(it)
            }
            orderRepository.findAll().last().also {
                orderRepository.delete(it)
            }
        }
    }
}
