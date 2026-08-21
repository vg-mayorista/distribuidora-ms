# ConflictingBeanDefinitionException al arrancar

**Módulo**: Backend  
**Patrón**:  
`ConflictingBeanDefinitionException: Annotation-specified bean name 'X' for bean class [com.distribuidora.Y.Controller] conflicts with existing, non-compatible bean definition of same name and class [com.distribuidora.Z.Controller]`

**Diagnóstico / Fix**:  
Clase legacy compilada en `target/classes` (paquete viejo) convive con la versión nueva movida a otro paquete. Ejecutar `.\mvnw.cmd clean` y recompilar.
