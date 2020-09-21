package example.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller {

    @GetMapping("/world")
    fun hello(): String {
        return "hello world"
    }

    @GetMapping("/protected")
    fun client(): String {
        return "hello protected"
    }
}
