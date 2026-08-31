package at.fhooe.sail.mc.taskroulette;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class TaskrouletteApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskrouletteApplication.class, args);
	}


	@Bean
	CommandLineRunner createDefaultTasks(TaskRepository taskRepository) {
		return args -> {
			if (taskRepository.count() > 0) return; // don't duplicate on restart
		taskRepository.saveAll(List.of(
			new Task("Wash dishes", TimeWeight.SHORT),
			new Task("Change bed sheets", TimeWeight.SHORT),
			new Task("Wash the dogs", TimeWeight.LONG)
			));
		};
	}


	@Bean
	CommandLineRunner createDefaultRewards(RewardRepository rewardRepository, TaskRepository taskRepository) {
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
