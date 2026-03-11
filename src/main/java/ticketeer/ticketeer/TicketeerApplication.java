/*
package ticketeer.ticketeer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TicketeerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketeerApplication.class, args);
	}

}
*/

package ticketeer.ticketeer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
})
public class TicketeerApplication {
	public static void main(String[] args) {
		SpringApplication.run(TicketeerApplication.class, args);
	}
}
