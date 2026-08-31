package at.fhooe.sail.mc.taskroulette;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/task")

public class TaskController {

    private final TaskRepository tasks;

    public TaskController(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return tasks.findAll();
    }


    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return tasks.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        Task saved = tasks.save(task);
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        if (!tasks.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        task.setId(id);
        Task updated = tasks.save(task);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
       if (!tasks.existsById(id)) {
           return ResponseEntity.notFound().build();
       }
       tasks.deleteById(id);
       return ResponseEntity.noContent().build();
    }


}
