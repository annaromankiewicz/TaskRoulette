package at.fhooe.sail.mc.taskroulette;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@SpringBootApplication
@EnableScheduling
public class TaskrouletteApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskrouletteApplication.class, args);
	}


	@Bean
	@Order(value = 0)
	CommandLineRunner createDefaultTasks(TaskRepository taskRepository) {
		return args -> {
			if (taskRepository.count() > 0) return; // don't duplicate on restart
		taskRepository.saveAll(List.of(
			new Task("Wash dishes", TimeWeight.LIGHT, Location.HOME),
			new Task("Change bed sheets", TimeWeight.MEDIUM, Location.APARTMENT),
			new Task("Wash the dogs", TimeWeight.HEAVY, Location.HOME)
			));
		};
	}


	@Bean
	@Order(value = 1)
	CommandLineRunner createDefaultRewards(RewardRepository rewardRepository) {
		return args -> {
			if (rewardRepository.count() > 0) return;
			rewardRepository.saveAll(List.of(
					new Reward("Watch one episode"),
					new Reward("Make a hot chocolate"),
					new Reward("Eat or buy favourite food")
			));
		};
	}







}
