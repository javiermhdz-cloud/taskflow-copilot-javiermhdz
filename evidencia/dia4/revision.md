# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `b930eec7-300d-48a2-b462-02d0da71e5ca`  
> - **Started:** 17/9/2026, 2:18:24 p.m.  
> - **Duration:** 40s  
> - **Exported:** 17/9/2026, 2:19:05 p.m.  

---

<sub>1s</sub>

### User

Revisa evidencia/dia4/summary.diff contra la especificación specs/summary.md.

---

<sub>5s</sub>

### Copilot

Reading the diff and the specification to compare them. Opening evidence/dia4/summary.diff and specs/summary.md in parallel.

---

<sub>5s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-javiermhdz-cloud\evidencia\dia4\summary.diff.**

C:\Users\User\taskflow-copilot-javiermhdz-cloud\evidencia\dia4\summary.diff

<details>
<summary>268 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ProjectController.java b/src/main/java/com/taskflow/controller/ProjectController.java
index 56ccccf..a27333a 100644
--- a/src/main/java/com/taskflow/controller/ProjectController.java
+++ b/src/main/java/com/taskflow/controller/ProjectController.java
@@ -2,6 +2,7 @@ package com.taskflow.controller;
 
 import com.taskflow.dto.ProjectRequest;
 import com.taskflow.dto.ProjectResponse;
+import com.taskflow.dto.ProjectSummaryResponse;
 import com.taskflow.dto.TaskResponse;
 import com.taskflow.exception.ProjectNotFoundException;
 import com.taskflow.mapper.ProjectMapper;
@@ -131,4 +132,14 @@ public class ProjectController {
         projectService.eliminar(id);
         return ResponseEntity.noContent().build();
     }
+
+    /** GET /projects/{id}/summary — 200 con el resumen; 404 uniforme si el proyecto no existe. */
+    @Operation(summary = "Resumen de un proyecto",
+            description = "Tareas por estado (siempre TODO, IN_PROGRESS y DONE) y cuántas están vencidas. 404 si el proyecto no existe.")
+    @GetMapping("/projects/{id}/summary")
+    public ProjectSummaryResponse getResumen(@PathVariable("id") Long id) {
+        Project proyecto = projectService.buscarPorId(id)
+                .orElseThrow(() -> new ProjectNotFoundException(id));
+        return projectService.resumen(proyecto);
+    }
 }
diff --git a/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
new file mode 100644
index 0000000..970175b
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
@@ -0,0 +1,19 @@
+package com.taskflow.dto;
+
+import com.taskflow.model.TaskStatus;
+
+import java.util.Map;
+
+/**
+ * ProjectSummaryResponse — contrato de salida de GET /projects/{id}/summary (versión de referencia del Día 4).
+ *
+ * byStatus trae siempre las tres claves de TaskStatus (TODO, IN_PROGRESS, DONE), aunque valgan 0.
+ */
+public record ProjectSummaryResponse(
+        Long projectId,
+        String projectName,
+        long totalTasks,
+        Map<TaskStatus, Long> byStatus,
+        long overdue
+) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index a33a95b..39364f9 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -1,7 +1,11 @@
 package com.taskflow.mapper;
 
 import com.taskflow.dto.ProjectResponse;
+import com.taskflow.dto.ProjectSummaryResponse;
 import com.taskflow.model.Project;
+import com.taskflow.model.TaskStatus;
+
+import java.util.Map;
 
 /**
  * ProjectMapper — puente DTO &lt;-&gt; dominio del lado Project. Estático, a mano, sin MapStruct.
@@ -22,4 +26,10 @@ public final class ProjectMapper {
         return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                 p.getOwnerId(), p.getCreatedAt());
     }
+
+    /** Proyecto + conteos ya calculados por el service -> DTO de salida de GET /projects/{id}/summary. */
+    public static ProjectSummaryResponse aResumen(Project p, long totalTasks, Map<TaskStatus, Long> byStatus,
+                                                  long overdue) {
+        return new ProjectSummaryResponse(p.getId(), p.getName(), totalTasks, byStatus, overdue);
+    }
 }
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index d315e3f..db4698d 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -1,9 +1,12 @@
 package com.taskflow.service;
 
 import com.taskflow.dto.ProjectRequest;
+import com.taskflow.dto.ProjectSummaryResponse;
 import com.taskflow.exception.ProjectNotFoundException;
+import com.taskflow.mapper.ProjectMapper;
 import com.taskflow.model.Project;
 import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
 import com.taskflow.model.User;
 import com.taskflow.repository.ProjectRepository;
 import com.taskflow.repository.TaskRepository;
@@ -12,7 +15,9 @@ import org.springframework.security.core.userdetails.UsernameNotFoundException;
 import org.springframework.stereotype.Service;
 
 import java.time.LocalDate;
+import java.util.EnumMap;
 import java.util.List;
+import java.util.Map;
 import java.util.Optional;
 
 /**
@@ -101,4 +106,19 @@ public class ProjectService {
         tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
         projectRepository.deleteById(id);
     }
+
+    /**
+     * Resumen de un proyecto que ya existe (el 404 lo resuelve el controller): cuántas tareas tiene en cada
+     * estado y cuántas están vencidas. Vencida = Task.estaVencida(); la regla no se reescribe aquí.
+     */
+    public ProjectSummaryResponse resumen(Project proyecto) {
+        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
+        Map<TaskStatus, Long> porEstado = new EnumMap<>(TaskStatus.class);
+        for (TaskStatus estado : TaskStatus.values()) {
+            porEstado.put(estado, 0L);                      // las tres claves siempre, aunque valgan 0
+        }
+        tareas.forEach(t -> porEstado.merge(t.getStatus(), 1L, Long::sum));
+        long vencidas = tareas.stream().filter(Task::estaVencida).count();
+        return ProjectMapper.aResumen(proyecto, tareas.size(), porEstado, vencidas);
+    }
 }
diff --git a/src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java b/src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java
new file mode 100644
index 0000000..a9df216
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProjectSummaryControllerTest.java
@@ -0,0 +1,64 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ProjectController;
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.model.Project;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.Map;
+import java.util.Optional;
+
+import static org.mockito.ArgumentMatchers.any;
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+/** Slice web de GET /projects/{id}/summary (versión de referencia del Día 4): solo el contrato HTTP. */
+@WebMvcTest(ProjectController.class)
+@AutoConfigureMockMvc(addFilters = false)   // la seguridad no se prueba en el slice; el 401 de esta ruta lo comprueba verificar.ps1
+class ProjectSummaryControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getResumen_existente_devuelve200ConCadaCampo() throws Exception {
+        when(projectService.buscarPorId(2L)).thenReturn(Optional.of(new Project(2L, "App Móvil", "d", 2L, null)));
+        when(projectService.resumen(any(Project.class))).thenReturn(new ProjectSummaryResponse(2L, "App Móvil", 4,
+                Map.of(TaskStatus.TODO, 1L, TaskStatus.IN_PROGRESS, 2L, TaskStatus.DONE, 1L), 1));
+
+        mockMvc.perform(get("/projects/2/summary"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.projectId").value(2))
+                .andExpect(jsonPath("$.projectName").value("App Móvil"))
+                .andExpect(jsonPath("$.totalTasks").value(4))
+                .andExpect(jsonPath("$.byStatus.TODO").value(1))
+                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(2))
+                .andExpect(jsonPath("$.byStatus.DONE").value(1))
+                .andExpect(jsonPath("$.overdue").value(1));
+    }
+
+    @Test
+    void getResumen_proyectoInexistente_devuelve404() throws Exception {
+        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
+
+        mockMvc.perform(get("/projects/99/summary"))
+                .andExpect(status().isNotFound())
+                .andExpect(jsonPath("$.status").value(404));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProjectSummaryServiceTest.java b/src/test/java/com/taskflow/unit/ProjectSummaryServiceTest.java
new file mode 100644
index 0000000..0bef9ed
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProjectSummaryServiceTest.java
@@ -0,0 +1,72 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.exception.TaskValidationException;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.ProjectRepository;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.repository.UserRepository;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.time.LocalDate;
+import java.util.List;
+import java.util.Map;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.mockito.Mockito.when;
+
+/** Unit de ProjectService.resumen (versión de referencia del Día 4): sin Spring, tareas reales. */
+@ExtendWith(MockitoExtension.class)
+class ProjectSummaryServiceTest {
+
+    @Mock
+    private ProjectRepository projectRepository;
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @Mock
+    private UserRepository userRepository;
+
+    @InjectMocks
+    private ProjectService service;
+
+    private final Project appMovil = new Project(2L, "App Móvil", "d", 2L, null);
+
+    @Test
+    void resumen_cuentaPorEstadoYVencidas() throws TaskValidationException {
+        LocalDate ayer = LocalDate.now().minusDays(1);
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
+                tarea(6L, TaskStatus.TODO, LocalDate.now().plusDays(10)),
+                tarea(7L, TaskStatus.IN_PROGRESS, ayer),          // vencida
+                tarea(8L, TaskStatus.DONE, ayer.minusDays(4)),    // fecha pasada pero DONE: NO vence
+                tarea(9L, TaskStatus.IN_PROGRESS, null)));        // sin fecha: NO vence
+
+        // Contado a mano: TODO 1, IN_PROGRESS 2, DONE 1; vencida solo la 7 (igual que el proyecto 2 de la semilla)
+        assertEquals(new ProjectSummaryResponse(2L, "App Móvil", 4,
+                        Map.of(TaskStatus.TODO, 1L, TaskStatus.IN_PROGRESS, 2L, TaskStatus.DONE, 1L), 1),
+                service.resumen(appMovil));
+    }
+
+    @Test
+    void resumen_proyectoSinTareas_todoEnCeroYLasTresClaves() {
+        Project legacy = new Project(3L, "Migración Legacy", "d", 1L, null);
+        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());
+
+        assertEquals(new ProjectSummaryResponse(3L, "Migración Legacy", 0,
+                        Map.of(TaskStatus.TODO, 0L, TaskStatus.IN_PROGRESS, 0L, TaskStatus.DONE, 0L), 0),
+                service.resumen(legacy));
+    }
+
+    private Task tarea(Long id, TaskStatus estado, LocalDate fecha) throws TaskValidationException {
+        return new Task(id, "Tarea " + id, "d", estado, Priority.MED, 2L, 1L, fecha);
+    }
+}
```

</details>


---

<sub>12s</sub>

### Copilot

Opening the spec and the current versions of the files touched in the diff to compare context and implement the checklist. Running parallel reads of specs/summary.md and the referenced Java files.

---

<sub>12s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-javiermhdz-cloud\specs\summary.md.**

C:\Users\User\taskflow-copilot-javiermhdz-cloud\specs\summary.md

<details>
<summary>52 lines</summary>

````
## Qué se pide

Un endpoint nuevo, `GET /projects/{id}/summary`, que resume un proyecto: **cuántas tareas tiene en
cada estado y cuántas están vencidas**. Sirve para pintar un tablero sin descargar todas las tareas del
proyecto. Pide el token de `POST /auth/login`, como todo lo que no es público.

## Respuesta 200

```json
{
  "projectId": 2,
  "projectName": "App Móvil",
  "totalTasks": 4,
  "byStatus": { "TODO": 1, "IN_PROGRESS": 2, "DONE": 1 },
  "overdue": 1
}
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto pedido |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `byStatus` | objeto | una clave por cada valor de `TaskStatus` (`TODO`, `IN_PROGRESS`, `DONE`), **siempre las tres**, aunque valgan 0 |
| `overdue` | número | cuántas tareas del proyecto cumplen `Task.estaVencida()` |

## Reglas

1. **Vencida** es exactamente lo que ya dice `Task.estaVencida()`: tiene fecha límite, la fecha ya pasó y
   el estado no es `DONE`. Se reutiliza ese método; no se reescribe la regla en el servicio.
2. Un proyecto **sin tareas** responde 200 con `totalTasks` 0, las tres claves de `byStatus` en 0 y `overdue` 0.
3. Un proyecto **que no existe** responde **404** con el `ErrorResponse` uniforme de siempre
   (`ProjectNotFoundException`, igual que `GET /projects/{id}`).
4. **Sin token** responde **401**, como todo lo que no es público.
5. Cualquier usuario autenticado puede pedir el resumen de cualquier proyecto (igual que `GET /projects/{id}/tasks`).

## Resultado esperado con la semilla del perfil `h2`

| Petición | `totalTasks` | `byStatus` | `overdue` |
|---|---|---|---|
| `GET /projects/1/summary` | 5 | `TODO` 3 · `IN_PROGRESS` 1 · `DONE` 1 | 0 (la tarea 2 tiene fecha pasada, pero está `DONE`) |
| `GET /projects/2/summary` | 4 | `TODO` 1 · `IN_PROGRESS` 2 · `DONE` 1 | 1 (la tarea 7) |
| `GET /projects/3/summary` | 0 | `TODO` 0 · `IN_PROGRESS` 0 · `DONE` 0 | 0 |
| `GET /projects/99/summary` | — | — | 404 |

Si antes de probar creaste tareas en un proyecto (por la UI o con el servidor MCP), `totalTasks` y
`TODO` de ese proyecto suben en la misma cantidad. Al reiniciar la app con el perfil `h2` vuelven a
ser los de la tabla.

## Cómo se construye (convenciones de TaskFlow)

- DTO de salida: un `record` en `com.taskflow.dto`, `ProjectSummaryResponse`.
- La lógica de conteo va en el servicio (`ProjectService`), no en el controller.
- El controller solo traduce HTTP: busca el proyecto (404 si no existe) y delega.
- Tests:
  - **unit** del servicio con Mockito: un proyecto con tareas en los tres estados y una vencida; un proyecto sin tareas.
  - **slice** `@WebMvcTest` del controller: 200 con el JSON de arriba y 404 con un proyecto inexistente.
- `mvn -q test` en verde al terminar, sin modificar tests existentes.

## Criterios de aceptación

- [ ] `GET /projects/2/summary` con el token de `ana` devuelve el JSON del ejemplo.
- [ ] `GET /projects/3/summary` devuelve ceros en los cinco números.
- [ ] `GET /projects/99/summary` devuelve 404.
- [ ] `mvn -q test` en verde: los tests de antes más los nuevos.
````

</details>


---

<sub>12s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-javiermhdz-cloud\src\main\java\com\taskflow\controller\ProjectController.java.**

C:\Users\User\taskflow-copilot-javiermhdz-cloud\src\main\java\com\taskflow\controller\ProjectController.java

<details>
<summary>133 lines</summary>

```
package com.taskflow.controller;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.dto.ProjectResponse;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.dto.TaskResponse;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.mapper.ProjectMapper;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * ProjectController — la puerta HTTP de los proyectos. HOY (integrador) crece a CRUD completo, en
 * DTOs (ProjectRequest / ProjectResponse); las tareas de un proyecto salen como TaskResponse.
 *
 * Repetición deliberada del patrón de TaskController: mismos gestos (201 + Location, @Valid, 404 vía
 * orElseThrow -> advice). El lado Project cierra la tabla de endpoints del capstone (menos /auth/*).
 */
@RestController
@Tag(name = "Projects", description = "CRUD de proyectos y sus tareas. Borrar un proyecto arrastra sus tareas (cascada).")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /** GET /projects — todos los proyectos como ProjectResponse. */
    @Operation(summary = "Lista todos los proyectos",
            description = "Devuelve todos los proyectos como ProjectResponse (lista vacía si no hay ninguno).")
    @GetMapping("/projects")
    public List<ProjectResponse> getProjects() {
        return projectService.listar().stream()
                .map(ProjectMapper::aResponse)
                .toList();
    }

    /** GET /projects/{id} — 200 con ProjectResponse; 404 uniforme si no existe. */
    @Operation(summary = "Obtiene un proyecto por id",
            description = "200 con el ProjectResponse; 404 uniforme si el id no existe.")
    @GetMapping("/projects/{id}")
    public ProjectResponse getProject(@PathVariable("id") Long id) {
        Project proyecto = projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return ProjectMapper.aResponse(proyecto);
    }

    /**
     * GET /projects/{id}/tasks — las tareas de un proyecto como TaskResponse. Conserva la distinción
     * de D2: proyecto inexistente -> 404 (orElseThrow); proyecto sin tareas -> 200 con []. El filtro
     * ?status= es STRETCH (mismo enum que /tasks). Sigue delegando en ProjectService.tareasDe.
     */
    @Operation(summary = "Lista las tareas de un proyecto",
            description = "Tareas del proyecto como TaskResponse; 404 si el proyecto no existe, 200 con [] si no tiene tareas. Filtro opcional ?status= (stretch).")
    @GetMapping("/projects/{id}/tasks")
    public List<TaskResponse> getTareasDeProyecto(
            @PathVariable("id") Long id,
            @RequestParam(name = "status", required = false) TaskStatus status) {   // status: STRETCH
        projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        List<Task> tareas = projectService.tareasDe(id);
        if (status != null) {                          // STRETCH: filtro opcional por estado
            tareas = tareas.stream().filter(t -> t.getStatus() == status).toList();
        }
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    /**
     * POST /projects — 201 + Location a /projects/{id}. @Valid dispara Bean Validation (400 si falla).
     * MP-9: el owner sale del JWT — el Authentication (inyectado por Spring Security) trae el username
     * del token; el service lo resuelve a ownerId. El dueño es QUIEN crea, no una constante.
     */
    @Operation(summary = "Crea un proyecto",
            description = "El owner se toma del usuario autenticado (el username del JWT).")
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request,
                                                         Authentication authentication) {
        Project creado = projectService.crear(request, authentication.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/projects/{id}")
                .buildAndExpand(creado.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProjectMapper.aResponse(creado));
    }

    /** PUT /projects/{id} — reemplazo COMPLETO. 200 con ProjectResponse; 404 si no existe. */
    @Operation(summary = "Reemplaza un proyecto",
            description = "Conserva id, owner y createdAt; name y description vienen del cuerpo.")
    @PutMapping("/projects/{id}")
    public ProjectResponse updateProject(@PathVariable("id") Long id,
                                         @Valid @RequestBody ProjectRequest request) {
        Project actualizado = projectService.reemplazar(id, request);
        return ProjectMapper.aResponse(actualizado);
    }

    /**
     * DELETE /projects/{id} — 204 No Content; 404 si no existe. Borra en cascada las tareas del
     * proyecto (regla "no Task sin Project").
     *
     * MP-9: la regla del capstone EN SERIO. @PreAuthorize se evalúa ANTES del método: pasa si el
     * usuario es ADMIN o es el owner del proyecto (bean @projectSecurity, data-driven). Un USER que no
     * es owner -> 403 (la API SÍ sabe quién es, por eso 403 y no 401). Proyecto inexistente ->
     * esOwner devuelve true a propósito para que hable el 404 del servicio.
     */
    @Operation(summary = "Borra un proyecto y sus tareas",
            description = "Solo ADMIN o el owner. Un USER no-owner recibe 403.")
    @PreAuthorize("hasRole('ADMIN') or @projectSecurity.esOwner(#id, authentication.name)")
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("id") Long id) {
        projectService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** GET /projects/{id}/summary — 200 con el resumen; 404 uniforme si el proyecto no existe. */
    @Operation(summary = "Resumen de un proyecto",
            description = "Tareas por estado (siempre TODO, IN_PROGRESS y DONE) y cuántas están vencidas. 404 si el proyecto no existe.")
    @GetMapping("/projects/{id}/summary")
    public ProjectSummaryResponse getResumen(@PathVariable("id") Long id) {
        Project proyecto = projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return projectService.resumen(proyecto);
    }
}
```

</details>


---

<sub>12s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-javiermhdz-cloud\src\main\java\com\taskflow\mapper\ProjectMapper.java.**

C:\Users\User\taskflow-copilot-javiermhdz-cloud\src\main\java\com\taskflow\mapper\ProjectMapper.java

<details>
<summary>29 lines</summary>

```
package com.taskflow.mapper;

import com.taskflow.dto.ProjectResponse;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.model.Project;
import com.taskflow.model.TaskStatus;

import java.util.Map;

/**
 * ProjectMapper — puente DTO &lt;-&gt; dominio del lado Project. Estático, a mano, sin MapStruct.
 *
 * HOY se SIMPLIFICÓ (lo prometía D3): la entidad ya guarda 'ownerId' directo (se aplanó el 'User
 * owner' en MP-4), así que aResponse ya no deriva el id desde un objeto (p.getOwner().id()) — lee
 * p.getOwnerId() tal cual. El contrato de salida (ProjectResponse con ownerId Long) no cambió; el
 * mapeo se volvió trivial porque el dominio por fin coincide con la forma canónica.
 */
public final class ProjectMapper {

    private ProjectMapper() {
        // no instanciable
    }

    /** Entidad -> DTO de salida. Ahora ownerId sale directo del campo (sin puente por objeto). */
    public static ProjectResponse aResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getOwnerId(), p.getCreatedAt());
    }

    /** Proyecto + conteos ya calculados por el service -> DTO de salida de GET /projects/{id}/summary. */
    public static ProjectSummaryResponse aResumen(Project p, long totalTasks, Map<TaskStatus, Long> byStatus,
                                                  long overdue) {
        return new ProjectSummaryResponse(p.getId(), p.getName(), totalTasks, byStatus, overdue);
    }
}
```

</details>


---

<sub>12s</sub>

### `view`

**view the file at C:\Users\User\taskflow-copilot-javiermhdz-cloud\src\main\java\com\taskflow\service\ProjectService.java.**

C:\Users\User\taskflow-copilot-javiermhdz-cloud\src\main\java\com\taskflow\service\ProjectService.java

<details>
<summary>112 lines</summary>

```
package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.mapper.ProjectMapper;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.model.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ProjectService — la capa de negocio del lado Project. Cero HTTP aquí (el "no existe" se traduce con
 * orElseThrow -> ProjectNotFoundException; el advice le pone el 404).
 *
 * Cambios de HOY (S2D4) — y por qué la promesa del día NO lo cubre:
 *   - MP-4: el modelo Project se aplanó (User owner -> Long ownerId), así que 'crear' y 'reemplazar'
 *     manejan un ownerId (Long), no un objeto User. Antes 'crear' construía con 0L (convención del
 *     InMemory de "aún sin id"); ahora construye con null y la BD asigna el id (IDENTITY).
 *   - MP-6: 'tareasDe' cambió su stream-filter de findAll() por taskRepository.findByProjectId(id)
 *     — la nota de D3 ("en D4 esto se vuelve un query method") se paga AQUÍ. La promesa "no tocar el
 *     servicio" protege a TaskService (que no se tocó), no a ProjectService (que ya se tocó en MP-4).
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;   // D5: para resolver el owner desde el username del JWT

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** Todos los proyectos. */
    public List<Project> listar() {
        return projectRepository.findAll();
    }

    /** Un proyecto por id (el Optional sube tal cual: el controller decide 404 vs 200). */
    public Optional<Project> buscarPorId(Long id) {
        return projectRepository.findById(id);
    }

    /**
     * Las tareas de un proyecto: MP-6 sustituye el stream-filter de D2 por la derived query
     * taskRepository.findByProjectId(projectId) — el LIKE... perdón, el WHERE project_id = ? lo hace
     * la BD (viajan solo las filas del proyecto). "no existe el proyecto" (404) lo decide el
     * controller con buscarPorId; ESTE método, si el proyecto existe pero no tiene tareas, devuelve
     * [] (-> 200 con []). No son lo mismo.
     */
    public List<Task> tareasDe(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Crea un proyecto (POST): el request trae name y description; el ownerId ya NO es una constante
     * (murió el 1L fijo de D3) — se resuelve del USERNAME AUTENTICADO (el que puso el JWT en el
     * Authentication). El dueño no lo decide el cliente ni una semilla: es QUIEN llama. createdAt = hoy;
     * id == null -> la BD lo asigna (IDENTITY).
     */
    public Project crear(ProjectRequest request, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new UsernameNotFoundException(ownerUsername));
        Project nuevo = new Project(null, request.name(), request.description(),
                owner.getId(), LocalDate.now());
        return projectRepository.save(nuevo);
    }

    /**
     * Reemplazo COMPLETO (PUT): existe -> reconstruye con el MISMO id, conservando ownerId y createdAt
     * (el cliente no los toca); name y description vienen del request. save con id presente = update.
     * No existe -> 404.
     */
    public Project reemplazar(Long id, ProjectRequest request) {
        Project actual = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        Project reemplazo = new Project(actual.getId(), request.name(), request.description(),
                actual.getOwnerId(), actual.getCreatedAt());
        return projectRepository.save(reemplazo);
    }

    /**
     * Elimina un proyecto (DELETE) con CASCADA MANUAL: borra primero sus tareas (regla "no Task sin
     * Project" — y además la FK task.project_id lo EXIGE: borrar el proyecto con tareas colgando daría
     * una violación de integridad) y luego el proyecto. No existe -> 404. La regla owner/ADMIN llega
     * en D5 (hoy queda abierto, documentado en el @Operation del controller).
     */
    public void eliminar(Long id) {
        projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
        projectRepository.deleteById(id);
    }

    /**
     * Resumen de un proyecto que ya existe (el 404 lo resuelve el controller): cuántas tareas tiene en cada
     * estado y cuántas están vencidas. Vencida = Task.estaVencida(); la regla no se reescribe aquí.
     */
    public ProjectSummaryResponse resumen(Project proyecto) {
        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
        Map<TaskStatus, Long> porEstado = new EnumMap<>(TaskStatus.class);
        for (TaskStatus estado : TaskStatus.values()) {
            porEstado.put(estado, 0L);                      // las tres claves siempre, aunque valgan 0
        }
        tareas.forEach(t -> porEstado.merge(t.getStatus(), 1L, Long::sum));
        long vencidas = tareas.stream().filter(Task::estaVencida).count();
        return ProjectMapper.aResumen(proyecto, tareas.size(), porEstado, vencidas);
    }
}
```

</details>


---

<sub>40s</sub>

### Copilot

Resumen breve: leída la diff y la spec; los cambios implementan la especificación y los tests pedidos. Entrega la revisión.

| # | Gravedad | Archivo:línea | Punto de la checklist | Qué pasa | Qué harías |
|---|---:|---|---|---|---|
| 1 | Sugerencia | evidence diff: ProjectController +132 | Alcance / Convenciones | Se añadió endpoint, DTO record, mapper, service y tests slice+unit: todo esperado por la spec. | Ninguna acción obligatoria; coincide con la spec. |
| 2 | Sugerencia | evidence diff: ProjectService (resumen) | Reglas del dominio | Usa Task::estaVencida() tal cual; no reescribe la regla. | Ninguna; correcto. |
| 3 | Sugerencia | evidence diff: ProjectSummaryResponse.java | Contrato JSON | DTO tiene los campos y Map\<TaskStatus,Long> para byStatus — Jackson serializará las claves enum como nombres. | Ninguna; correcto. |
| 4 | Sugerencia | evidence diff: tests slice & unit | Tests existentes / Cobertura | No se modificaron tests previos; se añadieron unit y slice que cubren los casos pedidos por la spec. | Ninguna; correcto. |

Casos sin test:
- Ninguno: la unit cubre proyecto con tareas (3 estados + 1 vencida) y proyecto sin tareas; el slice cubre 200 con el JSON y 404 para inexistente.

Veredicto: APROBADO.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
