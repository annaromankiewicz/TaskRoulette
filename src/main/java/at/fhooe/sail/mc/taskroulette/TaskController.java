package at.fhooe.sail.mc.taskroulette;

import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/task")

public class TaskController {

    private final TaskRepository taskRepository;
    private final TaskService taskService;

    public TaskController(TaskRepository taskRepository, TaskService taskService) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
    }


    @GetMapping
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }


    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return taskRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        Task saved = taskRepository.save(task);
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        task.setId(id);
        Task updated = taskRepository.save(task);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        taskRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CompletedTask> postCompletedTask(@PathVariable Long id) {
        RequestResponse requestResponse = taskService.completeTask(id);
        if (requestResponse == RequestResponse.NOT_FOUND) return ResponseEntity.status(404).build(); // 404 means that the resource does not exist
        if (requestResponse == RequestResponse.COMPLETED) return ResponseEntity.status(201).build(); // 201 is created successfully
            // completed task triggers the instantiation of a completedTask and a reward is going to be randomly selected
        return ResponseEntity.status(409).build(); // 409 conflict - task at id is not in_progress
    }


}
