package at.fhooe.sail.mc.taskroulette;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/tasks")

public class TaskController {

    private List<Task> tasks = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        return ResponseEntity.ok(tasks);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return tasks.stream()
                .filter(task -> id.equals(task.getId()))
                .findFirst().map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        if (!(task.getTitle() == null && task.getTitle().isBlank())) {
            task.setId(idCounter.incrementAndGet());
            tasks.add(task);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(task);
        }
        else return ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        if (!(task.getTitle() == null && task.getTitle().isBlank())) {
            for (int i = 0; i < tasks.size(); i++) {
                if (id.equals(tasks.get(i).getId())) {
                    task.setId(id);
                    tasks.set(i, task);
                    return ResponseEntity.ok(task); // 200 + status code
                }
            }
            return ResponseEntity.notFound().build(); // 404
        } else {
            ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        boolean removed = tasks.removeIf(task -> id.equals(task.getId()));
        return removed
                ? ResponseEntity.noContent().build() // 204
                : ResponseEntity.notFound().build(); // 404
    }


}
