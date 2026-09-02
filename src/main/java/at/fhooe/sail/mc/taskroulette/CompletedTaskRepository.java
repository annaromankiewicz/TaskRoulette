package at.fhooe.sail.mc.taskroulette;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompletedTaskRepository extends JpaRepository<CompletedTask, Long> {

}
