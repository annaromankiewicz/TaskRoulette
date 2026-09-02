package at.fhooe.sail.mc.taskroulette;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TaskService {

    private final RewardRepository rewardRepository;
    private final TaskRepository taskRepository;
    private final CompletedTaskRepository completedTaskRepository;

    public TaskService(TaskRepository taskRepository, RewardRepository rewardRepository, CompletedTaskRepository completedTaskRepository) {
        this.taskRepository = taskRepository;
        this.rewardRepository = rewardRepository;
        this.completedTaskRepository = completedTaskRepository;
    }

    @Transactional
    public RequestResponse completeTask(Long id) {
        CompletedTask completedTask = null;
        if (taskRepository.existsById(id)) {
            Task task = taskRepository.getReferenceById(id);
                if (task.setComplete()) {      // check if task wasn´t set done before
                    // random number <= number of rewards
                    List<Reward> rewardList = rewardRepository.findAll();
                    int randomNumber = ThreadLocalRandom.current().nextInt(0, rewardList.size());
                    Reward reward = rewardList.get(randomNumber);            // draw a reward
                    completedTask = new CompletedTask(task, reward);
                    completedTaskRepository.save(completedTask);
                    return RequestResponse.COMPLETED;
                }
                return RequestResponse.NOT_IN_PROGRESS;
            }
        return RequestResponse.NOT_FOUND;

        }


}
