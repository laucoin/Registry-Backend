package com.laucoin.registry.core.controller

import com.laucoin.registry.core.model.user.UserModel
import com.laucoin.registry.core.repository.IUserRepository
import com.laucoin.registry.core.util.toUser
import java.security.Principal
import java.time.LocalDateTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class TestController(private val repo: IUserRepository) {
    @GetMapping("/test")
    fun test(principal: Principal): UserModel {
        return principal.toUser()
    }

    @PostMapping("/test")
    fun postTest(@RequestBody user: UserModel): Mono<UserModel> {
        return repo.save(user.apply {
            creationDate = LocalDateTime.now()
            editionDate = LocalDateTime.now()
        })
    }
}
