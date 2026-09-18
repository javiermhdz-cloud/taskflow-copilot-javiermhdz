package com.taskflow.unit;

import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReasignarTareaServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService service;

    @Test
    void reasignar_tareaTODO_sinResponsable_guardaConNuevoAssignee() throws TaskValidationException {
        Task tarea = new Task(1L, "Task1", "d", TaskStatus.TODO, Priority.MED, 1L, null, null);
        // Simular que save devuelve la misma tarea guardada
        org.mockito.Mockito.doReturn(tarea).when(taskRepository).save(org.mockito.ArgumentMatchers.any(Task.class));
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);

        Task result = service.reasignar(tarea, 2L);

        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertEquals(2L, saved.getAssigneeId());
        // el método devuelve la tarea guardada (mock save no está stubbeado, comprobamos el campo)
        assertEquals(2L, result.getAssigneeId());
    }

    @Test
    void reasignar_tareaDONE_lanzaTaskStateException_yNoGuarda() throws TaskValidationException {
        Task tareaDone = new Task(2L, "Task2", "d", TaskStatus.DONE, Priority.HIGH, 1L, 3L, LocalDate.now());

        assertThrows(TaskStateException.class, () -> service.reasignar(tareaDone, 5L));
        verify(taskRepository, never()).save(tareaDone);
    }
}
