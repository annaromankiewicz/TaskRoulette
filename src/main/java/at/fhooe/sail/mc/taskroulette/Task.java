package at.fhooe.sail.mc.taskroulette;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "Task")


public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private TimeWeight timeWeight;

    @Enumerated(EnumType.STRING)
    private Location location;

    @Enumerated(EnumType.STRING)
    private State state;

    private Instant activatedAt;



    public Task(String title, TimeWeight timeWeight, Location location) {
        this.title = Objects.requireNonNull(title, "Title must be not null");
        this.timeWeight = Objects.requireNonNull(timeWeight, "Time weight has to be not null");
        state = State.BACKLOG;
        this.location = location;
    }

    protected Task() {
    }

    public boolean setInProgress() {
        if (state == State.BACKLOG) {
            state=State.IN_PROGRESS;
            activatedAt = Instant.now();
            return true;
        } return false;
    }

    public boolean setCompleted() {
        if (state==State.DONE) return false;
        state = State.DONE;
        return true;
    }

    public void setBacklog() {
        state = State.BACKLOG;
    }

    // getters and setters or later with lombok interface with @Data
    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTimeWeight(TimeWeight timeWeight) {
        this.timeWeight = timeWeight;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public State getState() {
        return this.state;
    }



    public TimeWeight getTimeWeight() {
        return this.timeWeight;
    }

    public Location getLocation() {
        return this.location;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

}
