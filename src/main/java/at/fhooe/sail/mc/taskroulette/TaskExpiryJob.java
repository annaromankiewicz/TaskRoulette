package at.fhooe.sail.mc.taskroulette;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class TaskExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(TaskExpiryJob.class);
    private final TaskRepository taskRepository;
    private static final Duration TIMEOUT = Duration.ofDays(1);

    public TaskExpiryJob(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }



    @Transactional
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)     // check time
    public void taskExpired() {
        List<Task> taskList = taskRepository.findAll();
        for (Task task : taskList)
            if (task.getState() == State.IN_PROGRESS) {
                if (Duration.between(task.getActivatedAt(), Instant.now()).compareTo(TIMEOUT)>0) { // solar day 86400 seconds
                    log.info("expired task found");
                    task.setBacklog();
                }
            }
    }


}

