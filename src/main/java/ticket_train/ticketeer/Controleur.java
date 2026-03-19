package ticket_train.ticketeer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

@RequestMapping("Hello")
public class Controleur {

    @GetMapping
    public String toto() {
        return "hello essome ";
    }
}
