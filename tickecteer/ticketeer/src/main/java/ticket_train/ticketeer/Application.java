package ticket_train.ticketeer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"ticket_train.ticketeer.model"})  // Force le scan des entités
@EnableJpaRepositories(basePackages = {"ticket_train.ticketeer.repository"})  // Force le scan des repositories
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}





/*package ticket_train.tickecteer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
*/


