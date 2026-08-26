package com.distribuidora.deliverynote.service;

import com.distribuidora.deliverynote.exception.DeliveryNoteDownloadNotAvailableException;
import com.distribuidora.deliverynote.exception.DeliveryNoteNotFoundException;
import com.distribuidora.deliverynote.model.DeliveryNote;
import com.distribuidora.deliverynote.model.DeliveryNoteItem;
import com.distribuidora.deliverynote.repository.DeliveryNoteRepository;
import com.distribuidora.model.Order;
import com.distribuidora.model.User;
import com.distribuidora.repository.OrderRepository;
import com.distribuidora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryNoteDocxService {

    private static final String TEMPLATE_PATH = "docs/V&G - Remito.docx";
    private static final ZoneId ARG = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalTime CUTOFF_TIME = LocalTime.of(18, 0);

    private final DeliveryNoteRepository deliveryNoteRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private final Clock clock = Clock.system(ARG);

    @Transactional(readOnly = true)
    public byte[] generate(UUID deliveryNoteId) {
        DeliveryNote dn = loadOrThrow(deliveryNoteId);
        validateCutoff(dn);

        Order order = orderRepository.findById(dn.getOrderId())
                .orElseThrow(() -> new DeliveryNoteNotFoundException(dn.getOrderId()));

        User user = userRepository.findById(order.getUserId()).orElse(null);

        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream is = resource.getInputStream();
             XWPFDocument document = new XWPFDocument(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            fillParagraphs(document, order, user, dn);
            fillTable(document, dn);
            document.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Error generando DOCX del remito " + deliveryNoteId, ex);
        }
    }

    private void validateCutoff(DeliveryNote dn) {
        LocalDate today = LocalDate.now(clock);
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.TUESDAY || dow == DayOfWeek.THURSDAY) {
            LocalTime now = LocalTime.now(clock);
            if (now.isBefore(CUTOFF_TIME)) {
                throw new DeliveryNoteDownloadNotAvailableException(
                        "El remito solo está disponible después de las 18:00 los días de corte (Martes y Jueves).");
            }
        }
    }

    private void fillParagraphs(XWPFDocument document, Order order, User user, DeliveryNote dn) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text == null || text.isBlank()) continue;

            if (text.contains("Nombre / Raz") && text.contains("Social:")) {
                String customerName = (user != null)
                        ? user.getFirstName() + " " + user.getLastName()
                        : "Cliente " + order.getUserId();
                replaceUnderscores(paragraph, customerName);
            } else if (text.contains("Zona de Entrega:")) {
                String zone = (user != null && user.getZone() != null) ? user.getZone() : "";
                replaceUnderscores(paragraph, zone);
            } else if (text.contains("Fecha:") && text.contains("Remito")) {
                String dateStr = (dn.getDeliveryDate() != null)
                        ? dn.getDeliveryDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : LocalDate.now(clock).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                replaceUnderscores(paragraph, dateStr + "          Remito Nº: " + dn.getDeliveryNoteNumber());
            } else if (text.contains("Observaciones:")) {
                String notes = dn.getNotes() != null ? dn.getNotes() : "";
                replaceUnderscores(paragraph, notes);
            } else if (text.contains("Firma y Aclaraci") && text.contains("del Cliente:")) {
                replaceUnderscores(paragraph, "");
            } else if (text.contains("FORMA DE PAGO")) {
                com.distribuidora.model.PaymentMethod pm = dn.getPaymentMethod() != null
                        ? dn.getPaymentMethod()
                        : order.getPaymentMethod();
                boolean isTransfer = (pm == com.distribuidora.model.PaymentMethod.TRANSFERENCIA);
                String replacement = isTransfer
                        ? "[   ]          TRANSFERENCIA [ X ]"
                        : "[ X ]          TRANSFERENCIA [   ]";
                replaceUnderscores(paragraph, replacement);
            }
        }
    }

    private void fillTable(XWPFDocument document, DeliveryNote dn) {
        List<XWPFTable> tables = document.getTables();
        if (tables.isEmpty()) return;

        XWPFTable table = tables.get(0);
        int rowCount = table.getNumberOfRows();
        if (rowCount == 0) return;

        // Remove all rows except the header (row 0)
        for (int i = rowCount - 1; i >= 1; i--) {
            table.removeRow(i);
        }

        List<DeliveryNoteItem> items = dn.getItems();
        BigDecimal total = BigDecimal.ZERO;

        for (DeliveryNoteItem item : items) {
            XWPFTableRow row = table.createRow();
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantityDelivered()));
            total = total.add(lineTotal);

            setCellText(row.getCell(0), item.getQuantityDelivered().toString());
            setCellText(row.getCell(1), item.getProductName());
            setCellText(row.getCell(2), item.getUnitPrice().toPlainString());
            setCellText(row.getCell(3), lineTotal.toPlainString());
        }

        // TOTAL row
        XWPFTableRow totalRow = table.createRow();
        setCellText(totalRow.getCell(2), "TOTAL:");
        setCellText(totalRow.getCell(3), total.toPlainString());
    }

    private void replaceUnderscores(XWPFParagraph paragraph, String replacement) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) return;

        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) fullText.append(text);
        }

        int usStart = fullText.indexOf("_");
        int usEnd = fullText.lastIndexOf("_");
        if (usStart < 0 || usEnd < 0) return;

        int charPos = 0;
        XWPFRun firstRun = null;
        int firstLocalStart = 0;
        XWPFRun lastRun = null;
        int lastLocalEnd = 0;

        for (int i = 0; i < runs.size(); i++) {
            XWPFRun run = runs.get(i);
            String text = run.getText(0);
            if (text == null) text = "";
            int runLen = text.length();

            if (firstRun == null && usStart >= charPos && usStart < charPos + runLen) {
                firstRun = run;
                firstLocalStart = usStart - charPos;
            }
            if (usEnd >= charPos && usEnd < charPos + runLen) {
                lastRun = run;
                lastLocalEnd = usEnd - charPos;
            }

            charPos += runLen;
        }

        if (firstRun == null || lastRun == null) return;

        int firstIdx = runs.indexOf(firstRun);
        int lastIdx = runs.indexOf(lastRun);

        String firstText = firstRun.getText(0);
        firstRun.setText(firstText.substring(0, firstLocalStart) + replacement, 0);

        // Remove intermediate runs
        for (int i = lastIdx - 1; i > firstIdx; i--) {
            paragraph.removeRun(i);
        }

        // Cleanup last run only if it's different from first run
        int adjustedIdx = firstIdx + 1;
        if (firstIdx != lastIdx && adjustedIdx < paragraph.getRuns().size()) {
            XWPFRun newLastRun = paragraph.getRuns().get(adjustedIdx);
            String lastText = newLastRun.getText(0);
            if (lastText != null && lastLocalEnd + 1 < lastText.length()) {
                newLastRun.setText(lastText.substring(lastLocalEnd + 1), 0);
            } else {
                newLastRun.setText("", 0);
            }
        }
    }

    private void setCellText(XWPFTableCell cell, String text) {
        if (cell == null) return;
        for (int i = cell.getParagraphs().size() - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
        XWPFParagraph p = cell.addParagraph();
        if (text != null && !text.isEmpty()) {
            p.createRun().setText(text);
        }
    }

    private DeliveryNote loadOrThrow(UUID id) {
        return deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new DeliveryNoteNotFoundException(id));
    }
}
