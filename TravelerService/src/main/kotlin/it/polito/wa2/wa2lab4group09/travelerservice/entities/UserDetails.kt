package it.polito.wa2.wa2lab4group09.travelerservice.entities

import javax.persistence.*


@Entity
@Table(name = "user_details")
class UserDetails(
    @Id
    var username: String,
    var name: String? = null,
    var surname: String? = null,
    var address: String? = null,
    var date_of_birth: String? = null,
    var telephone_number: String? = null,
    var role: Role
) {

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "userDetails")
    val tickets = mutableSetOf<TicketPurchased>()

    fun addTicket(t : TicketPurchased): TicketPurchased {
        t.userDetails = this
        tickets.add(t)
        return t
    }
}

enum class Role{
    CUSTOMER,ADMIN
}