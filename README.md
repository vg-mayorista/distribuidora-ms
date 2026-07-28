# Green Market

Green Market es un catálogo de productos **orgánicos** con vencimiento por lotes. Expone el **ABM (Alta / Baja / Modificación)** de `Product` sobre REST, con Postgres como base de datos, IDs UUID v4, paginación, uniqueness por nombre y tests E2E + unitarios contra Testcontainers.

## Stack

- Java 21 (LTS)
- Spring Boot 4.1.0 (MVC + Data JPA + Validation)
- PostgreSQL 16 (vía Docker Compose en local, vía Testcontainers en tests)
- JPA / Hibernate con `ddl-auto=update` (sin migraciones todavía)
- Lombok
- UUID v4 como identificador (Hibernate `GenerationType.UUID`)
- Testcontainers + RestClient para tests E2E
- Mockito + AssertJ para tests unitarios

## Estructura

```
src/main/java/com/greenmarket/
├── GreenMarketApplication.java
├── exception/
│   ├── ProductNotFoundException.java   # domain 404
│   ├── DuplicateProductException.java    # domain 409
│   └── GlobalExceptionHandler.java       # @ControllerAdvice → ProblemDetail
└── product/
    ├── Product.java                 # entity (aggregate root, UUID id)
    ├── ProductRepository.java       # Spring Data JPA + uniqueness queries
    ├── ProductService.java          # business logic (ABM + patch + uniqueness)
    ├── ProductController.java       # REST + pagination
    ├── dto/
    │   ├── CreateProductRequest.java
    │   ├── UpdateProductRequest.java
    │   ├── PatchProductRequest.java
    │   └── ProductResponse.java
    └── mapper/
        └── ProductMapper.java
```

Los errores de dominio (404, 409) se mapean vía custom exceptions + `@ControllerAdvice` como RFC 7807 `ProblemDetail`. Los 400 (validación) los maneja el handler default de Spring.

## Prerrequisitos

- Java 21 (`java -version`)
- Maven 3.9+ o usar el wrapper (`./mvnw`)
- Docker + Docker Compose (para Postgres local y para los tests E2E)
- `curl` y `jq` (opcional pero recomendado para las pruebas)

## Levantar la base de datos

```bash
docker compose up -d
```

Postgres queda en `localhost:5433` con DB/user/password `greenmarket/greenmarket/greenmarket` (configurable vía env vars `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`).

Para parar y borrar el volumen:

```bash
docker compose down -v
```

## Correr la aplicación

```bash
./mvnw spring-boot:run
```

Sin variables de entorno la app asume los defaults de `docker-compose.yml`. Para apuntar a otra DB:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://otro-host:5432/mydb \
SPRING_DATASOURCE_USERNAME=user \
SPRING_DATASOURCE_PASSWORD=pass \
./mvnw spring-boot:run
```

## Correr los tests

```bash
./mvnw test
```

34 tests en total: 20 unitarios (Mockito, < 1s) + 14 E2E (Testcontainers, ~10s). Los tests E2E levantan su propio contenedor de Postgres 16. **No hace falta tener el `docker compose` corriendo**: el test es autosuficiente.

> Si tu Docker Engine es 29.x y los tests fallan con un error `client version X is too old`, el `pom.xml` ya pinea `api.version=1.43` para que el cliente Docker de testcontainers negocie un API soportado. Si seguís con el error, actualizá testcontainers a 1.21.4+ cuando salga.

---

# API — ABM de Products

Base URL: `http://localhost:8080`

| Método | Path                              | Descripción              | Status éxito | Status error |
|--------|-----------------------------------|--------------------------|--------------|--------------|
| POST   | `/api/products`                   | Alta                     | `201`        | `400`, `409` |
| GET    | `/api/products`                   | Listar paginado          | `200`        | — |
| GET    | `/api/products/{id}`              | Obtener por id           | `200`        | `404` |
| PUT    | `/api/products/{id}`              | Modificar (full replace) | `200`        | `400`, `404`, `409` |
| PATCH  | `/api/products/{id}`              | Modificar parcial        | `200`        | `400`, `404`, `409` |
| DELETE | `/api/products/{id}`              | Baja (soft delete)       | `204`        | `404` |
| PATCH  | `/api/products/{id}/activate`     | Reactivar                | `200`        | `404` |

Los IDs son **UUID v4**. Todos los endpoints que reciben `{id}` esperan un UUID (ej. `a3f2b8c1-7d4e-4f1a-b5c3-9e8d2f6a1b4c`).

El nombre del producto es **único**. No se permiten dos productos activos o inactivos con el mismo nombre.

### Paginación

`GET /api/products` acepta parámetros de paginación estándar de Spring:

| Parámetro | Default | Descripción |
|-----------|---------|-------------|
| `page`    | `0`     | Número de página (0-based) |
| `size`    | `20`    | Elementos por página |
| `sort`    | `id,asc` | Campo y dirección de ordenamiento |

Ejemplo: `GET /api/products?page=0&size=10&sort=name,asc`

### Modelo

```json
{
  "id": "a3f2b8c1-7d4e-4f1a-b5c3-9e8d2f6a1b4c",
  "name": "Manzana roja orgánica",
  "description": "Manzana roja, cajón de 5kg",
  "price": 12500.00,
  "stock": 30,
  "active": true,
  "createdAt": "2026-06-17T15:00:00Z",
  "updatedAt": "2026-06-17T15:00:00Z"
}
```

Todos los productos son orgánicos por definición (es Green Market), no hay flag para eso. `active=false` significa soft delete — la fila queda en la tabla pero no se lista ni se devuelve por GET por id. `createdAt` y `updatedAt` los maneja JPA, no se aceptan en los request bodies.

### Respuesta paginada

`GET /api/products` devuelve un `Page` con metadata:

```json
{
  "content": [ { "id": "...", "name": "...", ... } ],
  "pageable": { "pageNumber": 0, "pageSize": 20, ... },
  "totalElements": 25,
  "totalPages": 2,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

## Documentación de la API (OpenAPI / Swagger)

La aplicación tiene integrado **Swagger UI**, lo que permite interactuar con la API directamente desde el navegador de forma visual y sin necesidad de ejecutar comandos `curl` manualmente.

### Cómo acceder a la documentación interactiva:

1. Levanta la base de datos de desarrollo:
   ```bash
   docker compose up -d
   ```
2. Inicia el servidor Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Abre tu navegador y accede a:
   * **Swagger UI (Interactiva):** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
   * **Especificación OpenAPI 3 (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Desde la interfaz de Swagger UI podrás ver los esquemas detallados de las peticiones (`CreateProductRequest`, `UpdateProductRequest`, etc.), los códigos de respuesta esperados y ejecutar pruebas reales de los 7 endpoints haciendo clic en **"Try it out"**.

---

## Próximos pasos

- **Aggregate `Lot`** (lotes con `expirationDate` y `quantity`): stock real del producto se computa desde la suma de lotes no vencidos. Diseño futuro documentado en `docs/lot-aggregate-design.md`.
- **Flyway/Liquibase** para migraciones (hoy `ddl-auto=update` solo sirve en dev).
- **Filtrado/búsqueda** en `GET /api/products` (por nombre, rango de precio, etc.).
- **Segunda entidad** (Category, Supplier, etc.).
