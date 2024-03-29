package it.polito.wa2.wa2lab4group09.travelerservice.controllers

import it.polito.wa2.wa2lab4group09.travelerservice.services.UserDetailsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class MyController(val userDetailsService: UserDetailsService) {

    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    @GetMapping("/my/profile")
    fun getUserDetails(@RequestHeader("Authorization") jwt:String) : ResponseEntity<Any>{
        val newToken = jwt.replace("Bearer", "")
        return try {
            val userDetailsDTO = userDetailsService.getUserDetails(newToken)
            //val body = UserDetailsUpdate(userDetailsDTO.name,userDetailsDTO.surname,userDetailsDTO.address,userDetailsDTO.date_of_birth,userDetailsDTO.telephone_number)
            ResponseEntity(userDetailsDTO, HttpStatus.OK)
        } catch (t : Throwable){
            val error = ErrorMessage(t.message)
            ResponseEntity(error, HttpStatus.BAD_REQUEST)
        }
    }

    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    @PutMapping("/my/profile")
    fun updateUserDetails(@RequestHeader("Authorization") jwt:String, @RequestBody userDetailsUpdate: UserDetailsUpdate) : ResponseEntity<Any>{
        val newToken = jwt.replace("Bearer", "")
        return try {
            userDetailsService.updateUserDetails(newToken,userDetailsUpdate)
            ResponseEntity(HttpStatus.OK)
        } catch (t : Throwable){
            val error = ErrorMessage(t.message)
            ResponseEntity(error, HttpStatus.BAD_REQUEST)
        }
    }

    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @GetMapping("/my/tickets")
    fun getUserTickets(@RequestHeader("Authorization") jwt:String) : ResponseEntity<Any>{
        val newToken = jwt.replace("Bearer", "")
        return try {
            val body = userDetailsService.getUserTickets(newToken)
            ResponseEntity(body, HttpStatus.OK)
        } catch (t : Throwable){
            val error = ErrorMessage(t.message)
            ResponseEntity(error, HttpStatus.BAD_REQUEST)
        }
    }

    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/my/tickets")
    fun buyTickets(@RequestHeader("Authorization") jwt:String, @RequestBody actionTicket: ActionTicket) : ResponseEntity<Any>{
        val newToken = jwt.replace("Bearer", "")
        return try {
            val body = userDetailsService.buyTickets(newToken,actionTicket)
            ResponseEntity(body, HttpStatus.OK)
        } catch (t : Throwable){
            val error = ErrorMessage(t.message)
            ResponseEntity(error, HttpStatus.BAD_REQUEST)
        }
    }
}

data class ActionTicket(val cmd : String, val quantity : Int, val zones : String, val type : Long)
//to return a JSON-shaped error
data class ErrorMessage(val error: String?)
data class UserDetailsUpdate(val name : String?, val surname : String?, val address : String?, val date_of_birth : String?, val telephone_number : String?)
