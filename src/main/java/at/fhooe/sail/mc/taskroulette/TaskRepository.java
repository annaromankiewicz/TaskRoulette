package at.fhooe.sail.mc.taskroulette;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    // nothing needed for Day 8
}