package de.sboe0705.rentals;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class RentalsApplicationForTest {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext appContext = SpringApplication.run(RentalsApplicationForTest.class, args);
		
		// Required to delay application shutdown to keep H2 console open
		// as this application (part) has no external services waiting for requests
		List<String> activeProfiles = determineActiveProfiles(appContext);
		if (activeProfiles.contains("generate-ddl")) {
			Thread.sleep(Duration.ofMinutes(5).toMillis());
		}
	}

	private static List<String> determineActiveProfiles(ConfigurableApplicationContext applicationContext) {
		Environment environment = applicationContext.getBean(Environment.class);
		List<String> activeProfiles = List.of(environment.getActiveProfiles());
		return activeProfiles;
	}

}
