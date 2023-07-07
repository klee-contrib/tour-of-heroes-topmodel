package topmodel.toh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import topmodel.toh.configuration.BatchConfiguration;
import topmodel.toh.configuration.JpaConfiguration;
import topmodel.toh.configuration.WebappConfiguration;

@SpringBootApplication
@Import({ //
		JpaConfiguration.class, //
		WebappConfiguration.class, //
		BatchConfiguration.class
})
public class TourOfHeroesApplication {

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(TourOfHeroesApplication.class, args)));
		// SpringApplication.run(TourOfHeroesApplication.class, args);
	}
}
