package at.fhooe.sail.mc.taskroulette;

import jakarta.persistence.*;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "Reward")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    protected Reward() {}   // for Hibernate

    public Reward(String description) {
        this.description = Objects.requireNonNull(description);
    }

    public Long getId() { return id; }

    public String getDescription() { return description; }

    @OneToMany(mappedBy = "reward")
    private List<CompletedTask> completions;

}
