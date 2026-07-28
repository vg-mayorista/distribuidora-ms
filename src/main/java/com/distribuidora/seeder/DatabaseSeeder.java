package com.distribuidora.seeder;

import com.distribuidora.model.Category;
import com.distribuidora.model.DeliveryMethod;
import com.distribuidora.model.Product;
import com.distribuidora.model.Role;
import com.distribuidora.model.User;
import com.distribuidora.repository.CategoryRepository;
import com.distribuidora.repository.DeliveryMethodRepository;
import com.distribuidora.repository.ProductRepository;
import com.distribuidora.repository.RoleRepository;
import com.distribuidora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(String... args) {
    seedRoles();
    seedUsers();
    seedDeliveryMethods();
    seedCategoriesAndProducts();
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
    if (deliveryMethodRepository.count() > 0) return;
    deliveryMethodRepository.save(DeliveryMethod.builder()
        .name("Retiro en Local").cost(BigDecimal.ZERO).estimatedDays(1).active(true).build());
    deliveryMethodRepository.save(DeliveryMethod.builder()
        .name("Envío a Domicilio").cost(new BigDecimal("500.00")).estimatedDays(3).active(true).build());
    deliveryMethodRepository.save(DeliveryMethod.builder()
        .name("Envío Express").cost(new BigDecimal("1200.00")).estimatedDays(1).active(true).build());
  }

  private void seedCategoriesAndProducts() {
    Category almacen = ensureCategory("Almacén");
    Category bebidas = ensureCategory("Bebidas");
    Category lacteos = ensureCategory("Lácteos");
    Category limpieza = ensureCategory("Limpieza");
    Category carniceria = ensureCategory("Carnicería");

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
    boolean exists = productRepository.findAll().stream()
        .anyMatch(p -> p.getName().equalsIgnoreCase(name));
    if (exists) return;

    productRepository.save(Product.builder()
        .name(name)
        .description(description)
        .price(BigDecimal.valueOf(price))
        .stock(stockUnits)
        .unitsPerPack(unitsPerPack)
        .categoryId(category.getId())
        .active(true)
        .build());
  }
}
