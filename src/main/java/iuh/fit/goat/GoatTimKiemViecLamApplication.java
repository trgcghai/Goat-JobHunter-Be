package iuh.fit.goat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GoatTimKiemViecLamApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoatTimKiemViecLamApplication.class, args);

	}

}
