package com.distribuidora.controller;

import com.distribuidora.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Reportes (Admin)", description = "Volumen, top productos/clientes, stock bajo, con export a CSV")
public class ReportController {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ReportService service;

    @GetMapping("/volume")
    @Operation(summary = "Volumen y ticket promedio (órdenes ENTREGADO)")
    public ReportService.VolumeAndTicket volume(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.volumeAndTicket(from, to);
    }

    @GetMapping("/top-products")
    @Operation(summary = "Productos más vendidos")
    public List<ReportService.TopProduct> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return service.topProducts(from, to, limit);
    }

    @GetMapping("/top-customers")
    @Operation(summary = "Clientes con más compras")
    public List<ReportService.TopCustomer> topCustomers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return service.topCustomers(from, to, limit);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Productos con stock bajo")
    public List<ReportService.LowStock> lowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return service.lowStock(threshold);
    }

    // ── Exports CSV ───────────────────────────────────────────

    @GetMapping(value = "/top-products.csv", produces = "text/csv")
    public ResponseEntity<byte[]> topProductsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<ReportService.TopProduct> rows = service.topProducts(from, to, 1000);
        StringBuilder sb = new StringBuilder("producto,unidades_vendidas,packs_vendidos,pedidos,ingresos\n");
        for (ReportService.TopProduct r : rows) {
            sb.append(csv(r.name())).append(',')
              .append(r.unitsSold()).append(',')
              .append(r.packsSold()).append(',')
              .append(r.orderCount()).append(',')
              .append(r.revenue().toPlainString()).append('\n');
        }
        return csvResponse(sb.toString(), "top-productos");
    }

    @GetMapping(value = "/top-customers.csv", produces = "text/csv")
    public ResponseEntity<byte[]> topCustomersCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<ReportService.TopCustomer> rows = service.topCustomers(from, to, 1000);
        StringBuilder sb = new StringBuilder("cliente,email,pedidos,gastado\n");
        for (ReportService.TopCustomer r : rows) {
            String name = ((r.firstName() == null ? "" : r.firstName()) + " " + (r.lastName() == null ? "" : r.lastName())).trim();
            sb.append(csv(name)).append(',')
              .append(csv(r.email())).append(',')
              .append(r.orderCount()).append(',')
              .append(r.totalSpent().toPlainString()).append('\n');
        }
        return csvResponse(sb.toString(), "top-clientes");
    }

    @GetMapping(value = "/low-stock.csv", produces = "text/csv")
    public ResponseEntity<byte[]> lowStockCsv(
            @RequestParam(defaultValue = "10") int threshold) {

        List<ReportService.LowStock> rows = service.lowStock(threshold);
        StringBuilder sb = new StringBuilder("producto,stock,unidades_por_pack,precio\n");
        for (ReportService.LowStock r : rows) {
            sb.append(csv(r.name())).append(',')
              .append(r.stock()).append(',')
              .append(r.unitsPerPack()).append(',')
              .append(r.price().toPlainString()).append('\n');
        }
        return csvResponse(sb.toString(), "stock-bajo");
    }

    @GetMapping(value = "/volume.csv", produces = "text/csv")
    public ResponseEntity<byte[]> volumeCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        ReportService.VolumeAndTicket v = service.volumeAndTicket(from, to);
        StringBuilder sb = new StringBuilder();
        sb.append("metrica,valor\n");
        sb.append("pedidos_entregados,").append(v.deliveredCount()).append('\n');
        sb.append("ingresos_entregados,").append(v.deliveredRevenue().toPlainString()).append('\n');
        sb.append("ticket_promedio,").append(v.avgTicket().toPlainString()).append('\n');
        sb.append("pedidos_cerrados,").append(v.closedCount()).append('\n');
        sb.append("ingresos_cerrados,").append(v.closedRevenue().toPlainString()).append('\n');
        return csvResponse(sb.toString(), "volumen-y-ticket");
    }

    private static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static ResponseEntity<byte[]> csvResponse(String body, String prefix) {
        String from = LocalDate.now().toString();
        String filename = prefix + "-" + from + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
            .body(body.getBytes(StandardCharsets.UTF_8));
    }
}
