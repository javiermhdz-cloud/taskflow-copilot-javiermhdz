# Proyecto final · Semana 6 · GitHub Copilot


**Alumno:** `Javier Macossay` · **Usuario de GitHub:** `javiermhdz-cloud`

## 1. Qué construí


| | Feature | Especificación |
|---|---|---|
| [ `x` ] | `PATCH /tasks/{id}/assignee` — cambiar el responsable | [`specs/assignee.md`](../specs/assignee.md) |

## 2. El pull request

- **URL del PR (mergeado):** `https://github.com/javiermhdz-cloud/taskflow-copilot-javiermhdz/pull/6`
- **Commit del merge en `main`:** `b54fc26 (HEAD -> main, origin/main, origin/HEAD) Merge pull request #6 from javiermhdz-cloud/feature/assignee`
- **Comentarios de Copilot code review:** `1 comentario con 3 sugerencias de cambios de código`


## 3. Cómo lo hice


| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/assignee` y copié la spec a `specs/` | `git log --oneline main..feature/assignee` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto:`) |
| Tests | `mvn test` en verde | `Tests run: 83, Failures: 0, Errors: 0, Skipped: 0` |
| Comprobación REST | `verificar.ps1` con `casos-assignee.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | la pestaña *Files changed* del PR |

## 4. Qué hizo el agente y qué corregí yo

| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | `los 2 tests nuevos daban UnfinishedStubbingException` | `mvn test` | `se corrigió con el segundo copilot -p (sesion-correccion-tests.md)` |

**Lo que el agente hizo bien a la primera** (una o dos líneas): `Practicamente todo lo demás.`

## 5. Comprobaciones REST

**pwsh -NoProfile -File .github/skills/verificar-taskflow/verificar.ps1**:

```text
PS C:\Users\User\taskflow-copilot-javiermhdz-cloud> pwsh -NoProfile -File .github/skills/verificar-taskflow/verificar.ps1
Repositorio: C:\Users\User\taskflow-copilot-javiermhdz-cloud
URL de la app: http://127.0.0.1:8080
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 800). Esperando a que /info responda...
App lista en 19 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
[OK]    PATCH /tasks/4/assignee asigna a luis
[OK]    GET /tasks/4 conserva el responsable nuevo
[OK]    PATCH /tasks/4/status a DONE ahora responde 200
[OK]    PATCH /tasks/2/assignee (DONE) responde 422
[OK]    PATCH /tasks/99/assignee responde 404
[OK]    PATCH /tasks/6/assignee con {} responde 400 y nombra assigneeId
[OK]    PATCH /tasks/6/assignee con 0 responde 400
[OK]    PATCH /tasks/6/assignee sin token responde 401
App detenida (PID 800).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 16/16 OK
```

**El total de Test run:**

```
[INFO] Tests run: 83, Failures: 0, Errors: 0, Skipped: 0
```

**Lo que se corrigio de la revisión y el code review:**

```
No corregi nada. Los comentarios de GitHub Copilot tienen la tendencia a ser muy esporádicos sin fundamentos. 
```

## 6. Créditos de la semana

| Qué | AI credits |
|---|---|
| Usados en septiembre según github.com (incluye semanas anteriores si usaste Copilot antes) | `21` |
| Implementación con la skill (`AI Credits` del PF-2) | `6.44` |
| Revisión del `revisor` (`AI Credits` del PF-3) | `1.58` |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | `0` |
