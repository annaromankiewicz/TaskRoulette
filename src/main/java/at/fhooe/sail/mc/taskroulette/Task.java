package at.fhooe.sail.mc.taskroulette;

import com.google.errorprone.annotations.InlineMeValidationDisabled;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name="Task")


public class Task {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private TimeWeight timeWeight;

    @Enumerated(EnumType.STRING)
    private Location location;

    private boolean completed;

    public Task(String title, TimeWeight timeWeight) {
            this.title = Objects.requireNonNull(title, "Title must be not null");
            this.timeWeight = Objects.requireNonNull(timeWeight, "Time weight has to be not null");
    }

    protected Task() {
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

    public TimeWeight getTimeWeight() {
        return this.timeWeight;
    }

    public Location getLocation() {
        return this.location;
    }

}
