# AGENTS — Backend Distribuidora

## Proyecto
**VG Mayorista API** — Spring Boot 4.1 + Java 25 + PostgreSQL (Docker).  
Arranque: `.\mvnw.cmd spring-boot:run` (perfil default).  
Puerto: `8080` (configurable en `application.properties`).

## Módulos / Paquetes
| Ruta | Contenido |
|------|-----------|
| `src/main/java/com/distribuidora/` | Controladores, servicios, modelos, DTOs |
| `src/main/java/com/distribuidora/deliverynote/` | Módulo de remitos (nuevo) |
| `src/main/resources/` | Configuración, application.properties |

## Convenciones
- **Lenguaje**: código en inglés, nombres de variables/métodos en inglés.
- **Validación**: Jakarta Validation (`@Valid`) en DTOs de entrada.
- **Seguridad**: `@PreAuthorize("hasRole('ROLE_X')")` en controladores.
- **Docs**: Swagger/OpenAPI (`@Tag`, `@Operation`).
- **Build**: Maven wrapper. No commitear `target/`.

## Errores conocidos
- Backend: consultar **`docs/errors/index.md`** para diagnósticos rápidos por patrón.
- Frontend: consultar **`D:\programacion\distribuidora-ui\docs\errors\index.md`** para diagnósticos rápidos por patrón.
- Cada error tiene su propio archivo con referencias cruzadas entre backend y frontend cuando aplica.

## Regla Kilo: documentación de errores
Ante un error recurrente o blocking, documentarlo en `docs/errors/<nombre-descriptivo>.md` siguiendo el formato:
- Título
- Módulo
- Patrón
- Diagnóstico / Fix
Luego actualizar el índice en `docs/errors/index.md`.

## Skills (futuro)
Definir en `.kilo/command/*.md` y `.kilo/agent/*.md`.
