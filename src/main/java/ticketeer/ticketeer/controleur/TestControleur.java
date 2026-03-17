// un test pour voir si mon spring boot fonctionne
/*
package ticketeer.ticketeer.controleur;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "test")

public class TestControleur {
     @GetMapping(path = "string")
     public String getString() {
         return "Chaine de caractere transmise par tickect";
     }
}


// un des test pour voir si mon spring boot fonctionne

package ticketeer.ticketeer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestControleur {

    @GetMapping("/test")
    public String test() {
        return "Projet démarré sans base";
    }
}
*/

package ticketeer.ticketeer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TicketeerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketeerApplication.class, args);
    }
}
