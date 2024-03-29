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
import it.polito.wa2.lab5.group09.ticketcatalogueservice.utils.PaymentResult
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.reactive.function.client.WebClientRequestException
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

    @Test
    fun getOrders(){
        runBlocking {
            val orders = ticketCatalogueService.getOrders(generateUserToken(_keyUser))
            Assertions.assertEquals(1, orders.last().ticketCatalogueId)
            Assertions.assertEquals(1, orders.last().quantity)
            Assertions.assertEquals("usernameTest", orders.last().customerUsername)
        }
    }

    @Test
    fun getOrdersByAdmin(){
       runBlocking {
           val orders=ticketCatalogueService.getUserOrders("usernameTest")
           Assertions.assertEquals(1, orders.last().ticketCatalogueId)
           Assertions.assertEquals(1, orders.last().quantity)
           Assertions.assertEquals("usernameTest", orders.last().customerUsername)
        }
    }
    @Test
    fun getOrderByUUID(){
        runBlocking {
            lateinit var orderId : UUID
            orderRepository.findAll().collect {  orderId = it.orderId!! }
            val order = ticketCatalogueService.getOrderByUUID(orderId, generateUserToken(_keyUser))
            Assertions.assertEquals(order.orderId,orderId)
        }
    }

    @Test
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
    fun getCatalogue(){
        runBlocking {
            val catalogue = ticketCatalogueService.getCatalogue()
            Assertions.assertEquals(catalogue.last().type,ticketCatalogue.type)
        }
    }



//
    @Test
    fun updateOrderWithFailedPayment(){
        runBlocking {
            lateinit var orderId : UUID
            orderRepository.findAll().collect {  orderId = it.orderId!! }
            val paymentResult = PaymentResult(orderId,false)
            ticketCatalogueService.updateOrder(paymentResult,generateUserToken(_keyUser))
            val status = orderRepository.findById(orderId)!!.status
            Assertions.assertEquals(status, Status.CANCELED)
        }
    }

    /*------NOTE: TRAVELER SERVICE IS OFFLINE!------*/

    @Test
    fun updateOrderWithAcceptedPayment(){
        Assertions.assertThrows(WebClientRequestException::class.java){
            runBlocking {
                lateinit var orderId : UUID
                orderRepository.findAll().collect {  orderId = it.orderId!! }
                val paymentResult = PaymentResult(orderId,true)
                ticketCatalogueService.updateOrder(paymentResult,generateUserToken(_keyUser))
                val status = orderRepository.findById(orderId)!!.status
                Assertions.assertEquals(status, Status.ACCEPTED)
            }
        }
    }

    @Test
    fun addTicketCatalogue(){
        runBlocking {
           ticketCatalogueService.addTicketToCatalogue(   TicketCatalogue(
               type = "School",
               price = 30.0f,
               zones = "ABC",
               maxAge = 20,
               minAge = 6))
           val tickets=ticketCatalogueService.getCatalogue()
            Assertions.assertEquals("School", tickets.last().type)
            Assertions.assertEquals(30.0f, tickets.last().price)
            Assertions.assertEquals("ABC", tickets.last().zones)
            Assertions.assertEquals(20, tickets.last().maxAge)
            Assertions.assertEquals(6, tickets.last().minAge)
        }
    }


    @AfterEach
    fun deleteTicketCatalogueAndOrder(){
        runBlocking {
            ticketCatalogueRepository.findByType("School").also {
                if(it.block() != null ){
                    ticketCatalogueRepository.delete(it.block()!!)
                }
            }

            ticketCatalogueRepository.findAll().last().also {
                ticketCatalogueRepository.delete(it)
            }

            orderRepository.findAll().last().also {
                orderRepository.delete(it)
            }
        }
    }
}
