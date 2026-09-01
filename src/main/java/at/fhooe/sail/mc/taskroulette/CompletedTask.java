package at.fhooe.sail.mc.taskroulette;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CompletedTask")

public class CompletedTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime completedAt;

    @OneToOne
    @JoinColumn(name = "task_id", unique = true)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "reward_id") // nullable = true is default value - think about it
    private Reward reward;


}
