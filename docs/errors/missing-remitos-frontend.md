# No aparecen remitos ni descarga en el dashboard del distribuidor

**Módulo**: Frontend  
**Patrón**:  
Ruta `/distribuidor/remitos` accesible pero sin links de navegación, o error 403/404 al cargar remitos.

**Diagnóstico / Fix**:  
Tres fallos frontend:
1. `delivery-note.service.ts` tenía `apiUrl` hardcodeada a `/api/admin/delivery-notes`, así que `ROLE_DISTRIBUTOR` recibía 403. Fix: detectar rol y usar `/api/distributor/delivery-notes` cuando corresponde.
2. `navbar.html` no mostraba el ítem `Remitos` para `ROLE_DISTRIBUTOR`. Fix: agregar `<li>` con `routerLink="/distribuidor/remitos"`.
3. Componentes y templates usaban rutas absolutas `/admin/remitos/...` en `openDetail()`, `openTransition()` y botones volver/cancelar. Fix: usar rutas relativas (`./`, `../`, `../../`).

**Ver también**: [`missing-distributor-remitos.md`](./missing-distributor-remitos.md) (causa backend complementaria)
