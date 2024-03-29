package it.polito.wa2.lab5.group09.ticketcatalogueservice

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import it.polito.wa2.lab5.group09.ticketcatalogueservice.controllers.PaymentInfo
import it.polito.wa2.lab5.group09.ticketcatalogueservice.controllers.PurchasingInfo
import it.polito.wa2.lab5.group09.ticketcatalogueservice.entities.Order
import it.polito.wa2.lab5.group09.ticketcatalogueservice.entities.TicketCatalogue
import it.polito.wa2.lab5.group09.ticketcatalogueservice.repositories.OrderRepository
import it.polito.wa2.lab5.group09.ticketcatalogueservice.repositories.TicketCatalogueRepository
import it.polito.wa2.lab5.group09.ticketcatalogueservice.security.Role
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class IntegrationTest {

    @LocalServerPort
    protected final var  port = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var ticketCatalogueRepository: TicketCatalogueRepository
    @Autowired
    lateinit var orderRepository: OrderRepository

    private final val ticketCatalogueEntity = TicketCatalogue(
        type = "testType",
        price = 1F,
        zones = "testZones",
    )

    private final val orderEntity = Order(
        ticketCatalogueId = 1,
        quantity = 3,
        customerUsername = "usernameTest"
    )

    private final var _keyUser = "laboratorio4webapplications2ProfessorGiovanniMalnati"

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
        sub: String? = "usernameAdminTest",
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
            ticketCatalogueRepository.save(ticketCatalogueEntity).also {
                orderRepository.save(orderEntity)
            }
        }
    }


    @Test
    fun getTickets(){
        runBlocking {
            val headers = HttpHeaders()
            val requestEntity = HttpEntity<Unit>(headers)
            val response = restTemplate.exchange(
                "http://localhost:$port/tickets", HttpMethod.GET, requestEntity, Any::class.java, TicketCatalogue::class.java
            )
            Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        }
    }

    @Test
    fun getUserOrderValid(){
        runBlocking {

            lateinit var orderId : UUID
            orderRepository.findAll().last().also{  orderId = it.orderId!! }

            val headers = HttpHeaders()
            val tkn = generateUserToken(_keyUser)
            headers.set("Authorization", "Bearer$tkn")
            val requestEntity = HttpEntity<Unit>(headers)
            val response = restTemplate.exchange(
                "http://localhost:$port/orders/$orderId", HttpMethod.GET, requestEntity, Any::class.java, Order::class.java
            )
            Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        }
    }

    @Test
    fun getUserOrderUnauthorized(){
        runBlocking {

            lateinit var orderId : UUID
            orderRepository.findAll().last().also{  orderId = it.orderId!! }

            val headers = HttpHeaders()
            val tkn = generateUserToken("129837y918273918273198723198731982739182739128273197")
            headers.set("Authorization", "Bearer$tkn")
            val requestEntity = HttpEntity<Unit>(headers)
            val response = restTemplate.exchange(
                "http://localhost:$port/orders/$orderId", HttpMethod.GET, requestEntity, Any::class.java, Order::class.java
            )
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }
    @Test
    fun getUserOrderInvalid(){
        runBlocking {
            val headers = HttpHeaders()
            val tkn = generateUserToken(_keyUser)
            headers.set("Authorization", "Bearer$tkn")
            val requestEntity = HttpEntity<Unit>(headers)
            val response = restTemplate.exchange(
                "http://localhost:$port/orders/fakeOrderId", HttpMethod.GET, requestEntity, Any::class.java, Order::class.java
            )
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }

    @Test
    fun buyTicketsInvalidCreditCardNumber() {
        runBlocking {
            val headers = HttpHeaders()
            val tkn = generateUserToken(_keyUser)
            headers.set("Authorization", "Bearer$tkn")
            val ticketId = ticketCatalogueRepository.findAll().last().ticketId!!
            val purchasingInfo = PurchasingInfo(3,ticketId, PaymentInfo("1234567","11/28", "123","testName testSurname"))
            val request = HttpEntity<PurchasingInfo>(purchasingInfo, headers)
            val result: ResponseEntity<String> = restTemplate.postForEntity(
                "http://localhost:$port/shop/$ticketId", request, String::class.java
            )
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        }
    }

    @Test
    fun buyTicketsInvalidExpDate() {
        runBlocking {
            val headers = HttpHeaders()
            val tkn = generateUserToken(_keyUser)
            headers.set("Authorization", "Bearer$tkn")
            val ticketId = ticketCatalogueRepository.findAll().last().ticketId!!
            val purchasingInfo = PurchasingInfo(3,ticketId, PaymentInfo("12345678901234","11/18", "123","testName testSurname"))
            val request = HttpEntity<PurchasingInfo>(purchasingInfo, headers)
            val result: ResponseEntity<String> = restTemplate.postForEntity(
                "http://localhost:$port/shop/$ticketId", request, String::class.java
            )
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        }
    }

    @Test
    fun buyTicketsInvalidCvv() {
        runBlocking {
            val headers = HttpHeaders()
            val tkn = generateUserToken(_keyUser)
            headers.set("Authorization", "Bearer$tkn")
            val ticketId = ticketCatalogueRepository.findAll().last().ticketId!!
            val purchasingInfo = PurchasingInfo(3,ticketId, PaymentInfo("12345678901234","11/28", "0","testName testSurname"))
            val request = HttpEntity<PurchasingInfo>(purchasingInfo, headers)
            val result: ResponseEntity<String> = restTemplate.postForEntity(
                "http://localhost:$port/shop/$ticketId", request, String::class.java
            )
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        }
    }

    @Test
    fun buyTicketsInvalidNumberOfTickets() {
        runBlocking {
            val headers = HttpHeaders()
            val tkn = generateUserToken(_keyUser)
            headers.set("Authorization", "Bearer$tkn")
            val ticketId = ticketCatalogueRepository.findAll().last().ticketId!!
            val purchasingInfo = PurchasingInfo(-1,ticketId, PaymentInfo("12345678901234","11/28", "123","testName testSurname"))
            val request = HttpEntity<PurchasingInfo>(purchasingInfo, headers)
            val result: ResponseEntity<String> = restTemplate.postForEntity(
                "http://localhost:$port/shop/$ticketId", request, String::class.java
            )
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        }
    }

    @Test
    fun getUserOrdersValid(){
        runBlocking {
            val headers = HttpHeaders()
            val tkn = generateUserToken(_keyUser)
            headers.set("Authorization", "Bearer$tkn")
            val requestEntity = HttpEntity<Unit>(headers)
            val response = restTemplate.exchange(
                "http://localhost:$port/orders", HttpMethod.GET, requestEntity, Any::class.java, Order::class.java
            )
            Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        }
    }


    @Test
    fun getUserOrdersInvalid(){
        runBlocking {
            val headers = HttpHeaders()
            val tkn = generateUserToken("129837y918273918273198723198731982739182739128273197")
            headers.set("Authorization", "Bearer$tkn")
            val requestEntity = HttpEntity<Unit>(headers)
            val response = restTemplate.exchange(
                "http://localhost:$port/orders", HttpMethod.GET, requestEntity, Any::class.java, Order::class.java
            )
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }

    @Test
    fun getAllUsersOrdersValid(){
        runBlocking {
            val headers = HttpHeaders()
            val tkn = generateAdminToken(_keyUser)
            headers.set("Authorization", "Bearer$tkn")
            val requestEntity = HttpEntity<Unit>(headers)
            val response = restTemplate.exchange(
                "http://localhost:$port/admin/orders", HttpMethod.GET, requestEntity, Any::class.java, Order::class.java
            )
            Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        }
    }

    @Test
    fun getAllUsersOrdersInvalid(){
        runBlocking {
            val headers = HttpHeaders()
            val tkn = generateUserToken("129837y918273918273198723198731982739182739128273197")
            headers.set("Authorization", "Bearer$tkn")
            val requestEntity = HttpEntity<Unit>(headers)
            val response = restTemplate.exchange(
                "http://localhost:$port/admin/orders", HttpMethod.GET, requestEntity, Any::class.java, Order::class.java
            )
            Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }

    @Test
    fun getAdminUserOrdersValid() {
        val headers = HttpHeaders()
        val tkn = generateAdminToken(_keyUser)
        headers.set("Authorization", "Bearer$tkn")
        val requestEntity = HttpEntity<Unit>(headers)
        val url = "http://localhost:$port/admin/orders/${orderEntity.customerUsername}"
        val response = restTemplate.exchange(
            url, HttpMethod.GET, requestEntity, Any::class.java, Any::class.java
        )
        Assertions.assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun getAdminUserOrdersInvalid() {
        val headers = HttpHeaders()
        val tkn = generateAdminToken("129837y918273918273198723198731982739182739128273197")
        headers.set("Authorization", "Bearer$tkn")
        val requestEntity = HttpEntity<Unit>(headers)
        val url = "http://localhost:$port/admin/orders/${orderEntity.customerUsername}"
        val response = restTemplate.exchange(
            url, HttpMethod.GET, requestEntity, Any::class.java, Any::class.java
        )
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun addTicketToCatalogueValid() {
        val headers = HttpHeaders()
        val tkn = generateAdminToken(_keyUser)
            headers.set("Authorization", "Bearer$tkn")
        val tmp= TicketCatalogue(
            type = "testAddTicket",
            price = 1F,
            zones = "testZones",
            minAge = 1,
            maxAge = 18
        )
        val request = HttpEntity<TicketCatalogue>(tmp, headers)
        val result: ResponseEntity<String> = restTemplate.postForEntity(
            "http://localhost:$port/admin/tickets", request, String::class.java
        )
         Assertions.assertEquals(HttpStatus.CREATED, result.statusCode)
    }

    @Test
    fun addTicketToCatalogueInvalid() {
        val headers = HttpHeaders()
        val tkn = generateAdminToken("129837y918273918273198723198731982739182739128273197")
        headers.set("Authorization", "Bearer$tkn")
        val requestEntity = HttpEntity<TicketCatalogue>(
            TicketCatalogue(
                type = "test2Type",
                price = 1F,
                zones = "testZones",
                minAge = 1,
                maxAge = 18
            ),
            headers
        )
        val response = restTemplate.exchange(
            "http://localhost:$port/admin/tickets", HttpMethod.POST, requestEntity, Any::class.java, Any::class.java
        )
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)    }



    @AfterEach
    fun deleteTicketCatalogueAndOrder() {
        runBlocking {
            ticketCatalogueRepository.findByType("testAddTicket").also {
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
