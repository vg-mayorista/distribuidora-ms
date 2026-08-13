package com.distribuidora.seeder;

import com.distribuidora.model.BusinessConfig;
import com.distribuidora.model.Category;
import com.distribuidora.model.DeliveryMethod;
import com.distribuidora.model.DeliveryMethodScope;
import com.distribuidora.model.DeliveryWindow;
import com.distribuidora.model.Product;
import com.distribuidora.model.Role;
import com.distribuidora.model.User;
import com.distribuidora.repository.BusinessConfigRepository;
import com.distribuidora.repository.CategoryRepository;
import com.distribuidora.repository.DeliveryMethodRepository;
import com.distribuidora.repository.DeliveryWindowRepository;
import com.distribuidora.repository.ProductRepository;
import com.distribuidora.repository.RoleRepository;
import com.distribuidora.repository.UserRepository;
import com.distribuidora.service.BusinessConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final DeliveryMethodRepository deliveryMethodRepository;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final BusinessConfigRepository businessConfigRepository;
  private final DeliveryWindowRepository deliveryWindowRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(String... args) {
    ensureSchemaCompatibility();
    seedRoles();
    seedUsers();
    seedDeliveryMethods();
    seedDeliveryWindows();
    seedCategoriesAndProducts();
    seedBusinessConfig();
  }

  private void ensureSchemaCompatibility() {
    executeQuietly("ALTER TABLE business_config ADD COLUMN min_packs_per_line INTEGER DEFAULT 5");
    executeQuietly("ALTER TABLE business_config ADD COLUMN min_order_amount NUMERIC(12, 2) DEFAULT 30000.00");
    executeQuietly("ALTER TABLE business_config ADD COLUMN updated_at TIMESTAMP");

    executeQuietly("ALTER TABLE delivery_methods ADD COLUMN applies_to_order_type VARCHAR(20) DEFAULT 'BOTH'");

    executeQuietly("ALTER TABLE orders ADD COLUMN type VARCHAR(20) DEFAULT 'STOCK'");
    executeQuietly("ALTER TABLE orders ADD COLUMN delivery_date DATE");
    executeQuietly("ALTER TABLE orders ADD COLUMN stock_decremented BOOLEAN DEFAULT FALSE");

    executeQuietly("ALTER TABLE order_items ADD COLUMN packs_requested INTEGER DEFAULT 0");
    executeQuietly("ALTER TABLE order_items ADD COLUMN units_per_pack_at_order INTEGER DEFAULT 1");
  }

  private void executeQuietly(String sql) {
    try {
      jdbcTemplate.execute(sql);
    } catch (Exception ignored) {
      // Ignorado si la columna ya existe o la tabla aún no se creó
    }
  }

  private void seedBusinessConfig() {
    if (businessConfigRepository.count() == 0) {
      businessConfigRepository.save(BusinessConfig.builder()
          .minPacksPerLine(BusinessConfigService.DEFAULT_MIN_PACKS_PER_LINE)
          .minOrderAmount(BusinessConfigService.DEFAULT_MIN_ORDER_AMOUNT)
          .updatedAt(Instant.now())
          .build());
    }
  }

  private void seedRoles() {
    roleRepository.findByName("ROLE_ADMIN").orElseGet(() ->
        roleRepository.save(Role.builder()
            .name("ROLE_ADMIN")
            .description("Administrator with full access")
            .build()));

    roleRepository.findByName("ROLE_DISTRIBUTOR").orElseGet(() ->
        roleRepository.save(Role.builder()
            .name("ROLE_DISTRIBUTOR")
            .description("Distributor (manages incoming orders)")
            .build()));

    roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() ->
        roleRepository.save(Role.builder()
            .name("ROLE_CUSTOMER")
            .description("Customer (places orders)")
            .build()));
  }

  private void seedUsers() {
    userRepository.findByEmail("admin@distribuidora.com").ifPresentOrElse(
        u -> { if (!u.getActive()) { u.setActive(true); userRepository.save(u); } },
        () -> userRepository.save(User.builder()
            .email("admin@distribuidora.com")
            .password(passwordEncoder.encode("admin123"))
            .firstName("Admin")
            .lastName("Distribuidora")
            .role(roleRepository.findByName("ROLE_ADMIN").orElseThrow())
            .active(true).build()));

    userRepository.findByEmail("distribuidor@distribuidora.com").ifPresentOrElse(
        u -> { if (!u.getActive()) { u.setActive(true); userRepository.save(u); } },
        () -> userRepository.save(User.builder()
            .email("distribuidor@distribuidora.com")
            .password(passwordEncoder.encode("distribuidor123"))
            .firstName("Dueño")
            .lastName("Distribuidora")
            .role(roleRepository.findByName("ROLE_DISTRIBUTOR").orElseThrow())
            .active(true).build()));

    userRepository.findByEmail("almacen@distribuidora.com").ifPresentOrElse(
        u -> {},
        () -> userRepository.save(User.builder()
            .email("almacen@distribuidora.com")
            .password(passwordEncoder.encode("cliente123"))
            .firstName("Almacén")
            .lastName("del Centro")
            .address("Av. San Martín 1234, CABA")
            .phone("+5491145678901")
            .role(roleRepository.findByName("ROLE_CUSTOMER").orElseThrow())
            .active(true).build()));

    userRepository.findByEmail("rotiseria@distribuidora.com").ifPresentOrElse(
        u -> {},
        () -> userRepository.save(User.builder()
            .email("rotiseria@distribuidora.com")
            .password(passwordEncoder.encode("cliente123"))
            .firstName("Rotisería")
            .lastName("La Esquina")
            .address("Av. Rivadavia 5678, CABA")
            .phone("+5491145671234")
            .role(roleRepository.findByName("ROLE_CUSTOMER").orElseThrow())
            .active(true).build()));

    if (!userRepository.existsByEmail("cliente@distribuidora.com")) {
      userRepository.save(User.builder()
          .email("cliente@distribuidora.com")
          .password(passwordEncoder.encode("cliente123"))
          .firstName("Cliente")
          .lastName("Demo")
          .address("Av. Demo 1234")
          .phone("+5491100000000")
          .role(roleRepository.findByName("ROLE_CUSTOMER").orElseThrow())
          .active(true).build());
    }
  }

  private void seedDeliveryMethods() {
    // Defensive backfill: en bases de datos creadas antes de que existiera
    // el campo `applies_to_order_type`, Hibernate con `ddl-auto: update` pudo
    // dejar la columna con NULL en filas existentes, causando 500 al listar.
    // Normalizamos cualquier valor NULL/inválido a BOTH antes de sembrar.
    backfillMissingScope();

    if (deliveryMethodRepository.count() > 0) return;
    deliveryMethodRepository.save(DeliveryMethod.builder()
        .name("Retiro en Local").cost(BigDecimal.ZERO).estimatedDays(1)
        .appliesToOrderType(DeliveryMethodScope.BOTH).active(true).build());
    deliveryMethodRepository.save(DeliveryMethod.builder()
        .name("Envío a Domicilio").cost(new BigDecimal("500.00")).estimatedDays(3)
        .appliesToOrderType(DeliveryMethodScope.BOTH).active(true).build());
    deliveryMethodRepository.save(DeliveryMethod.builder()
        .name("Envío Express").cost(new BigDecimal("1200.00")).estimatedDays(1)
        .appliesToOrderType(DeliveryMethodScope.STOCK).active(true).build());
  }

  /**
   * Recorre todos los métodos de entrega y, si alguno tiene {@code appliesToOrderType}
   * con un valor que no está en el enum (datos huérfanos), lo corrige a {@code BOTH}.
   *
   * <p>Esto compensa dos cosas que pueden haber pasado en despliegues viejos:
   * <ul>
   *   <li>La columna se agregó con Hibernate {@code ddl-auto: update} antes de
   *       que el enum existiera como tal → NULL en filas viejas (Hibernate lo
   *       deserializa como null sin tirar excepción).</li>
   *   <li>El CHECK constraint que limita a WHOLESALE|STOCK|BOTH nunca se creó
   *       automáticamente (las migrations no se ejecutan: solo se monta
   *       {@code db/init/} en docker-entrypoint-initdb.d). Esto permite que
   *       queden valores huérfanos que Hibernate NO PUEDE deserializar al
   *       enum, tirando {@code IllegalArgumentException} y devolviendo 500.</li>
   * </ul>
   *
   * <p>Se hace con SQL crudo porque si Hibernate intenta leer una fila con un
   * valor inválido, tira antes de que podamos remediarla desde el entity.
   */
  private void backfillMissingScope() {
    // 0. Asegurar que la columna exista. En Render la tabla se creó sin la
    // columna (Hibernate `ddl-auto: update` no la había agregado todavía al
    // momento de un deploy viejo), por lo que cualquier UPDATE/CHECK abajo
    // fallaba con "column does not exist" y rompía el startup. La creamos
    // idempotentemente con DEFAULT 'BOTH' para no tocar filas existentes.
    ensureColumnExists();

    // 1. Normalizar NULLs y valores fuera del enum a 'BOTH'.
    int updated = jdbcTemplate.update(
        "UPDATE delivery_methods " +
        "SET applies_to_order_type = 'BOTH' " +
        "WHERE applies_to_order_type IS NULL " +
        "   OR applies_to_order_type NOT IN ('WHOLESALE', 'STOCK', 'BOTH')"
    );
    if (updated > 0) {
      System.out.println("[seeder] Backfilled " + updated + " rows in delivery_methods.applies_to_order_type → BOTH");
    }

    // 2. Asegurar que el CHECK constraint exista (idempotente). Si la migration
    // V1 nunca corrió, este guard es la red de seguridad.
    try {
      jdbcTemplate.execute(
          "ALTER TABLE delivery_methods " +
          "ADD CONSTRAINT delivery_methods_scope_check " +
          "CHECK (applies_to_order_type IN ('WHOLESALE', 'STOCK', 'BOTH'))"
      );
      System.out.println("[seeder] Added missing CHECK constraint delivery_methods_scope_check");
    } catch (Exception e) {
      // Ya existe → ignorar (true en Postgres "already exists", en H2 puede
      // variar). Recorremos la cadena de causas para detectar el caso.
      if (!isConstraintAlreadyExistsError(e)) {
        throw e;
      }
    }
  }

  /**
   * Detecta si una excepción JDBC corresponde a "constraint ya existe" en
   * Postgres o H2. Recorre la cadena de causas porque Spring envuelve el error
   * original en {@code BadSqlGrammarException}.
   */
  private boolean isConstraintAlreadyExistsError(Throwable t) {
    while (t != null) {
      String msg = t.getMessage();
      if (msg != null) {
        String lower = msg.toLowerCase();
        if (lower.contains("already exists")) {       // Postgres / H2 english
          return true;
        }
      }
      t = t.getCause();
    }
    return false;
  }

  /**
   * Garantiza que {@code delivery_methods.applies_to_order_type} exista. La crea
   * con DEFAULT 'BOTH' NOT NULL si falta. Idempotente.
   *
   * <p>Necesario porque en despliegues viejos la tabla se creó antes de que la
   * entidad JPA tuviera este campo, y {@code ddl-auto: update} no llegó a
   * agregarlo. Cualquier intento de UPDATE sobre la columna tira
   * {@code column "applies_to_order_type" does not exist} y rompe el startup.
   *
   * <p>Usa try/catch sobre el propio ALTER en lugar de consultar
   * {@code information_schema}, porque H2 (test) tiene quirks de mayúsculas
   * que hacían fallar la detección incluso cuando la columna existía.
   */
  private void ensureColumnExists() {
    // 1. Intentar agregar la columna nullable con default. Si ya existe,
    //    capturamos el error y seguimos.
    boolean added = false;
    try {
      jdbcTemplate.execute(
          "ALTER TABLE delivery_methods " +
          "ADD COLUMN applies_to_order_type VARCHAR(20) DEFAULT 'BOTH'"
      );
      added = true;
    } catch (Exception e) {
      // Duplicate column → ya existe. Ignorar. (Spring envuelve el SQLException
      // en BadSqlGrammarException; recorremos la cadena para detectar el caso.)
      if (!isDuplicateColumnError(e)) {
        throw e;
      }
    }

    if (added) {
      System.out.println("[seeder] Column delivery_methods.applies_to_order_type missing → created");
      // Llenar NULLs que pudieran existir antes (defensa, normalmente 0).
      jdbcTemplate.update(
          "UPDATE delivery_methods SET applies_to_order_type = 'BOTH' " +
          "WHERE applies_to_order_type IS NULL"
      );
      // Reconciliación histórica: cualquier método "Express" debería ser STOCK,
      // coherente con la migration V1.
      jdbcTemplate.update(
          "UPDATE delivery_methods SET applies_to_order_type = 'STOCK' " +
          "WHERE name ILIKE '%express%' AND applies_to_order_type = 'BOTH'"
      );
    }
  }

  /**
   * Detecta si una excepción JDBC corresponde a un "duplicate column" en Postgres
   * o H2. Recorre la cadena de causas porque Spring envuelve el error original.
   */
  private boolean isDuplicateColumnError(Throwable t) {
    while (t != null) {
      String msg = t.getMessage();
      if (msg != null) {
        String lower = msg.toLowerCase();
        if (lower.contains("already exists")        // Postgres
            || lower.contains("duplicate column")   // H2 english
            || lower.contains("columna duplicada")  // H2 español
            || lower.contains("ya existe")) {       // Postgres español
          return true;
        }
      }
      t = t.getCause();
    }
    return false;
  }

  /**
   * Siembra las dos ventanas semanales por defecto. Es idempotente: si ya existen filas,
   * no crea duplicados. Para entornos donde ya se cargó la BD sin estas filas, se ejecuta
   * una sola vez al primer arranque post-migración.
   */
  private void seedDeliveryWindows() {
    ensureWindow(
            2, LocalTime.of(18, 0), 3,
            "Pedidos hasta Mar 18 h → entrega Mié"
    );
    ensureWindow(
            4, LocalTime.of(18, 0), 5,
            "Pedidos hasta Jue 18 h → entrega Vie"
    );
  }

  private void ensureWindow(int cutoffDow, LocalTime cutoffTime, int deliveryDow, String description) {
    boolean alreadyExists = deliveryWindowRepository.findAll().stream().anyMatch(w ->
            w.getCutoffDayOfWeek() != null
                    && w.getCutoffDayOfWeek() == cutoffDow
                    && cutoffTime.equals(w.getCutoffTime())
                    && w.getDeliveryDayOfWeek() != null
                    && w.getDeliveryDayOfWeek() == deliveryDow
    );
    if (alreadyExists) return;
    deliveryWindowRepository.save(DeliveryWindow.builder()
            .cutoffDayOfWeek(cutoffDow)
            .cutoffTime(cutoffTime)
            .deliveryDayOfWeek(deliveryDow)
            .description(description)
            .active(true)
            .build());
  }

  private void seedCategoriesAndProducts() {
    Category almacen = ensureCategory("Almacén");
    Category bebidas = ensureCategory("Bebidas");
    Category lacteos = ensureCategory("Lácteos");
    Category limpieza = ensureCategory("Limpieza");
    Category carniceria = ensureCategory("Carnicería");
    Category golosinas = ensureCategory("Golosinas");

    ensureProduct("Yerba Mate 1kg", "Yerba mate suave, paquete de 1kg.", 5500.00, 240, 12, almacen);
    ensureProduct("Azúcar Ledesma 1kg", "Azúcar blanca refinada tipo A, bolsa 1kg.", 1800.00, 360, 12, almacen);
    ensureProduct("Aceite Girasol 1.5L", "Aceite de girasol puro, botella PET 1.5L.", 4200.00, 120, 6, almacen);
    ensureProduct("Arroz Largo Fino 1kg", "Arroz parboiled calidad 00000, bolsa 1kg.", 2100.00, 240, 12, almacen);
    ensureProduct("Fideos Spaghetti 500g", "Fideos secos de sémola, paquete 500g.", 1500.00, 480, 12, almacen);
    ensureProduct("Lentejas 500g", "Lentejas secas, paquete 500g.", 1900.00, 200, 12, almacen);
    ensureProduct("Harina 000 1kg", "Harina de trigo 000, bolsa 1kg.", 1700.00, 180, 10, almacen);
    ensureProduct("Sal Fina 500g", "Sal de mesa refinada, paquete 500g.", 900.00, 300, 12, almacen);

    ensureProduct("Coca-Cola 1.5L", "Gaseosa Coca-Cola sin azúcar, botella 1.5L.", 2800.00, 144, 12, bebidas);
    ensureProduct("Agua Mineral 1.5L", "Agua mineral sin gas, botella 1.5L.", 1200.00, 240, 12, bebidas);
    ensureProduct("Jugo Naranja 1L", "Jugo de naranja exprimido, botella 1L.", 2400.00, 96, 12, bebidas);
    ensureProduct("Cerveza Quilmes 1L", "Cerveza rubia pale ale, botella 1L.", 3200.00, 96, 6, bebidas);
    ensureProduct("Vino Tinto Malbec 750ml", "Vino tinto Malbec, botella 750ml.", 5800.00, 60, 6, bebidas);

    ensureProduct("Leche La Serenísima 1L", "Leche entera UHT, caja 1L.", 1900.00, 240, 12, lacteos);
    ensureProduct("Yogurt Bebible Frutilla 1L", "Yogurt bebible sabor frutilla, botella 1L.", 2400.00, 120, 8, lacteos);
    ensureProduct("Queso Cremoso 500g", "Queso cremoso de primera marca, horma 500g.", 6500.00, 60, 1, lacteos);
    ensureProduct("Manteca 200g", "Manteca de primera marca, paquete 200g.", 3200.00, 120, 1, lacteos);

    ensureProduct("Detergente Lavavajillas 750ml", "Detergente concentrado, botella 750ml.", 2800.00, 96, 6, limpieza);
    ensureProduct("Lavandina 1L", "Lavandina concentrada al 3%, botella 1L.", 1900.00, 144, 8, limpieza);
    ensureProduct("Detergente Ropa 800ml", "Detergente líquido para ropa, botella 800ml.", 4500.00, 96, 6, limpieza);
    ensureProduct("Rollo Cocina x3", "Pack de 3 rollos de cocina doble hoja.", 2200.00, 80, 6, limpieza);

    ensureProduct("Carne Picada Especial 1kg", "Carne picada especial, bandeja 1kg.", 8500.00, 40, 1, carniceria);
    ensureProduct("Pollo Entero 2kg", "Pollo entero fresco, bandeja 2kg.", 6200.00, 30, 1, carniceria);
    ensureProduct("Asado de Tira 1kg", "Asado de tira, bandeja 1kg.", 9200.00, 35, 1, carniceria);

    // --- Edge cases para testear el flujo stock-disponible ---
    // Stock = 0 → no aparece en /cliente/stock-disponible, sí en mayorista con badge "Sin stock"
    ensureProduct("Galletas Dulces 200g", "Galletas dulces con chips de chocolate, paquete 200g.", 1400.00, 0, 12, golosinas);
    ensureProduct("Galletitas Saladas 150g", "Galletitas saladas con semillas, paquete 150g.", 1300.00, 0, 12, golosinas);

    // Stock bajo (< 20) → badge "low" en stock-disponible
    ensureProduct("Chocolate en Polvo 250g", "Chocolate en polvo para repostería, lata 250g.", 3600.00, 15, 6, golosinas);
    ensureProduct("Caramelos Masticables 100g", "Caramelos masticables surtidos, bolsa 100g.", 1800.00, 8, 12, golosinas);
    ensureProduct("Mermelada Durazno 250g", "Mermelada de durazno casera, frasco 250g.", 2900.00, 12, 1, lacteos);

    // Stock muy bajo con unidades por pack = 1 (testear 1 pack y máximo)
    ensureProduct("Huevos Docena", "Docena de huevos frescos, bandeja.", 4500.00, 3, 1, lacteos);
  }

  private Category ensureCategory(String name) {
    return categoryRepository.findAll().stream()
        .filter(c -> c.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElseGet(() -> categoryRepository.save(Category.builder()
            .name(name)
            .active(true)
            .build()));
  }

  private void ensureProduct(String name, String description, double price,
                             int stockUnits, int unitsPerPack, Category category) {
    ensureProduct(name, description, price, stockUnits, unitsPerPack, category, true);
  }

  private void ensureProduct(String name, String description, double price,
                             int stockUnits, int unitsPerPack, Category category, boolean active) {
    Product existing = productRepository.findAll().stream()
        .filter(p -> p.getName().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
    if (existing != null) {
      existing.setStock(stockUnits);
      existing.setUnitsPerPack(unitsPerPack);
      existing.setActive(active);
      productRepository.save(existing);
      return;
    }
    productRepository.save(Product.builder()
        .name(name)
        .description(description)
        .price(BigDecimal.valueOf(price))
        .stock(stockUnits)
        .unitsPerPack(unitsPerPack)
        .categoryId(category.getId())
        .active(active)
        .build());
  }
}