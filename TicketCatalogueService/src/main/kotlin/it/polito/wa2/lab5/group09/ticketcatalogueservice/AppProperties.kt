package it.polito.wa2.lab5.group09.ticketcatalogueservice

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AppProperties {

    @Value("\${application.jwt.jwtHeader}")
    lateinit var jwtHeader: String

    @Value("\${application.jwt.jwtHeaderStart}")
    lateinit var jwtHeaderStart: String

    @Value("\${application.jwt.jwtSecret}")
    lateinit var key: String
}