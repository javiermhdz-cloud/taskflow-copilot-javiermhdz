# Evidencia de la semana · GitHub Copilot
Alumno: Javier Macossay · Repo: https://github.com/javiermhdz-cloud/taskflow-copilot-javiermhdz

## Día 1 · La CLI
- **Qué construí:** Configuré las instrucciones iniciales del asistente y la documentación de arquitectura del proyecto base para habilitar el uso de la interfaz de línea de comandos de Copilot.
- **Dónde está:** `.github/copilot-instructions.md`, `docs/ARQUITECTURA.md`
- **Cómo se comprueba:** `evidencia/dia1/verificador.txt`, última línea `0 NO EXISTE`
- **Qué no salió:** Nada.

## Día 2 · Especificar, implementar y revisar
- **Qué construí:** Redacté especificaciones funcionales y utilicé la CLI para generar código y pruebas automatizadas iniciales validando el flujo de desarrollo guiado por IA.
- **Dónde está:** `specs/`, `evidencia/dia2/`
- **Cómo se comprueba:** Los registros de pruebas en la carpeta del día e impresiones de pantalla en documentos word.
- **Qué no salió:** Al principio el agente generó una discrepancia menor en los nombres de los DTOs, lo cual se ajustó corrigiendo la especificación.

## Día 3 · MCP
- **Qué construí:** Integré y probé un servidor MCP personalizado en Java para conectar herramientas externas al flujo de trabajo del asistente.
- **Dónde está:** `taskflow-mcp/`, `evidencia/dia3/`
- **Cómo se comprueba:** Los registros de pruebas en la carpeta del día e impresiones de pantalla en documentos word.
- **Qué no salió:** Tuve un pequeño problema inicial de puertos ocupados al levantar el servidor, el cual resolví cerrando procesos colgados en el puerto 8080.

## Día 4 · Skills y agentes
- **Qué construí:** Desarrollé y utilicé skills personalizadas y agentes especializados para automatizar tareas repetitivas de desarrollo y validación de endpoints.
- **Dónde está:** `.github/skills/`, `evidencia/dia4/`
- **Cómo se comprueba:** `evidencia/dia4/verificar.txt` con la ejecución correcta de las pruebas de integración.
- **Qué no salió:** En la primera prueba la skill no cargó correctamente al faltar la barra diagonal inicial `/` en el comando, lo cual se corrigió de inmediato.

## Día 5 · VS Code y proyecto final
- **Qué construí:** Implementé la funcionalidad final del endpoint `assignee` usando la skill `crear-endpoint-taskflow`, resolviendo pruebas unitarias, aplicando revisiones de código y validando todo con el script de verificación REST.
- **Dónde está:** `semana6/`, `src/`, `.github/skills/verificar-taskflow/`
- **Cómo se comprueba:** `semana6/proyecto-final.diff`, `semana6/revision.md` y el resultado exitoso de `verificar.ps1`.
- **Qué no salió:** El agente intentó revisar una funcionalidad equivocada (`summary` del día anterior) en lugar de `assignee` en el primer intento, y arrojó un error de mocks (`UnfinishedStubbingException`) que corregimos pidiéndole explícitamente usar `doReturn`/`doThrow`.

## Cierre
- **Créditos:** Gasté aproximadamente los créditos estimados por sesión (dentro de los topes de 30 AI credits por comando). En total gasté 211 de 1500 créditos disponibles. Para gastar menos en el futuro, estructuraría los prompts de manera más concisa y directa, evitando que el agente divague en reintentos de pruebas con errores de Mockito.
- **Una cosa que el agente hizo mal:** El agente armó mal los stubs de Mockito en los tests de asignación (`UnfinishedStubbingException`), intentando múltiples ediciones fallidas antes de rendirse. Lo detecté revisando el reporte de errores de Maven y se corrigió dándole una instrucción específica para usar `doReturn`/`doThrow`.
