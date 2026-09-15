# Arquitectura de TaskFlow

Este documento introduce a un desarrollador nuevo al diseño y convensiones del proyecto TaskFlow. Cubre capas, recorrido de la petición `POST /projects/{projectId}/tasks`, ubicación de las reglas de negocio, seguridad JWT y organización de tests.

## Capas y paquetes

- Capa de presentación (controladores): paquete raíz `com.taskflow.controller`. Ejemplo de clase: `TaskController`.
  - Rutas HTTP y validación de entrada. Archivos típicos: `src/main/java/com/taskflow/controller/TaskController.java` y `src/main/java/com/taskflow/controller/ProjectController.java`.

- Capa de servicio (casos de uso): paquete `com.taskflow.service`.
  - Contiene la lógica orquestadora y transaccional. Ejemplo: `TaskService`, `ProjectService`.
  - Archivos: `src/main/java/com/taskflow/service/TaskService.java`.

- Capa de repositorio (persistencia): paquete `com.taskflow.repository` (Spring Data JPA).
  - Interfaces `ProjectRepository`, `TaskRepository` que extienden `JpaRepository`.
  - Archivos: `src/main/java/com/taskflow/repository/TaskRepository.java`.

- Modelo y reglas de dominio: paquete `com.taskflow.model`.
  - Entidades ricas con comportamiento: `Task`, `Project`, `User`.
  - Regla y fábricas de creación: `Task.crear(...)`, `Task.estaVencida()`.
  - Archivos: `src/main/java/com/taskflow/model/Task.java`.

- Mappers y DTOs: paquetes `com.taskflow.dto` y `com.taskflow.mapper`.
  - Los controladores exponen DTOs (`record`) y usan mappers manuales: `TaskMapper.aResponse`.
  - Archivos: `src/main/java/com/taskflow/mapper/TaskMapper.java`.

- Configuración y seguridad: paquete `com.taskflow.config` y `com.taskflow.security`.
  - JWT, filtros y `ProjectSecurity` para autorización específica.
  - Archivos: `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`, `src/main/java/com/taskflow/security/ProjectSecurity.java`.

- Recursos estáticos: `src/main/resources/static` para la UI embebida.

## Recorrido: POST /projects/{projectId}/tasks

1. Petición HTTP llega al controlador: `POST /projects/{projectId}/tasks` manejada por `src/main/java/com/taskflow/controller/TaskController.java`.
   - El `@RequestBody` contiene un DTO validado con `@Valid`.

2. El controlador comprueba que el proyecto existe usando `ProjectService.buscarPorId(projectId)` y lanza `ProjectNotFoundException` si no existe.

3. El controlador delega en `TaskService.crear(request, projectId)`. En `TaskService.crear` el flujo es:
   - Convertir el DTO a entidad nueva usando `TaskMapper.aEntidadNueva(request, projectId)` (que pasa por `Task.crear` para aplicar la regla de fecha).
   - Persistir la entidad con `TaskRepository.save(nueva)`.

4. El `TaskController` construye la respuesta: devuelve `201 Created` con cabecera `Location` apuntando a `/tasks/{id}` y el cuerpo mapeado por `TaskMapper.aResponse(creada)`.

5. Reglas de validación estricta y de negocio residen en la entidad `Task` y en servicios de dominio específicos:
   - `Task.crear(...)` valida invariantes (no crear con fecha pasada, estado inicial `TODO`).
   - Cambios de estado y validaciones complejas se hacen a través de métodos de la entidad (`Task.setStatus(TaskStatus.DONE)`), no replicando reglas en el controlador.

5. Respuesta HTTP:
   - En caso de éxito, el controlador devuelve `201 Created` con cabecera `Location` apuntando al nuevo recurso y el DTO de respuesta mapeado por `TaskMapper.aResponse`.
   - Errores de validación generan 400; reglas de negocio violadas generan 422; entidad no encontrada 404. `GlobalExceptionHandler` convierte excepciones en respuestas uniformes.

## Dónde viven las reglas de negocio

- Reglas de invariantes y creación: métodos en `src/main/java/com/taskflow/model/Task.java` (p. ej. `Task.crear`, `Task.estaVencida`).
- Reglas orquestadoras (combinaciones de repositorios, verificación de permisos, transacciones): `src/main/java/com/taskflow/service/TaskService.java` y `ProjectService`.
- Autorizaciones por recurso: `src/main/java/com/taskflow/security/ProjectSecurity.java` con anotaciones `@PreAuthorize` en controladores cuando aplica.

Regla general: favor reutilizar la lógica de la entidad (no duplicarla en servicios). Los controladores sólo validan entrada y devuelven respuestas.

## Seguridad con JWT

- El sistema usa JWT sin estado. Endpoints públicos: `/auth/**`, `/info`, Swagger y consola H2. Todo lo demás requiere token.
- Flujo:
  - Login obtiene `JWT` desde `src/main/java/com/taskflow/controller/AuthController.java`.
  - Un filtro global (`JwtAuthenticationFilter`) extrae el token de la cabecera `Authorization: Bearer <token>`, lo valida y carga `UserDetails` en el `SecurityContext`.
  - Roles/claims del token determinan autorización; checks finos usan `@PreAuthorize` y `ProjectSecurity` para comprobar propiedad (por ejemplo, borrar proyecto sólo si es dueño o `ADMIN`).
- No hay sesiones en servidor: la validez y permisos se derivan del token.

## Organización de tests

- Unit tests: JUnit 5 + Mockito, sin arrancar Spring. Prueban lógica pura de servicios y model. Convención de nombres: `*Test.java`. Ejecutar con `mvn -q test` o `mvn -q test "-Dtest=TaskServiceTest"`.
  - Ejemplos: `src/test/java/com/taskflow/unit/TaskServiceTest.java`.

- Slice tests: `@WebMvcTest` o `@DataJpaTest` para capas concretas con Spring de forma liviana.

- Integration tests (`*IT.java`): usan Testcontainers y perfil `test`. No se ejecutan con la suite normal salvo con `-Ddocker.tests=true`.

- Datos de desarrollo: `DataSeeder` rellena la BD con usuarios `ana`, `luis`, `admin` cuando se arranca con el perfil `h2`. Archivo: `src/main/java/com/taskflow/config/DataSeeder.java`.

## Referencias rápidas

- Controladores: `src/main/java/com/taskflow/controller/TaskController.java`, `src/main/java/com/taskflow/controller/ProjectController.java`, `src/main/java/com/taskflow/controller/AuthController.java`.
- Servicios: `src/main/java/com/taskflow/service/TaskService.java`, `src/main/java/com/taskflow/service/ProjectService.java`.
- Modelos: `src/main/java/com/taskflow/model/Task.java`, `src/main/java/com/taskflow/model/Project.java`, `src/main/java/com/taskflow/model/User.java`.
- Repositorios: `src/main/java/com/taskflow/repository/TaskRepository.java`, `src/main/java/com/taskflow/repository/ProjectRepository.java`.
- Mappers/DTOs: `src/main/java/com/taskflow/mapper/TaskMapper.java`, `src/main/java/com/taskflow/dto/TaskRequest.java`.
- Seguridad: `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`, `src/main/java/com/taskflow/security/ProjectSecurity.java`.
- Excepciones y manejador global: `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java`.

Si se requiere, añadir ejemplos de llamadas, diagramas de secuencia o expandir el recorrido de otras operaciones (p. ej. transiciones de estado de tarea).Las fechas límite se validan en `Task.crear` (`src/main/java/com/taskflow/model/Task.java`).
