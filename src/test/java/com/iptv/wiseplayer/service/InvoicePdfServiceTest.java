package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.response.InvoiceResponse;
import com.iptv.wiseplayer.service.impl.InvoicePdfServiceImpl;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class InvoicePdfServiceTest {

    private final InvoicePdfService invoicePdfService = new InvoicePdfServiceImpl();

    @Test
    public void testGenerateInvoicePdf() {
        InvoiceResponse invoice = new InvoiceResponse(
                "INV-123",
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now(),
                com.iptv.wiseplayer.domain.enums.PaymentStatus.COMPLETED,
                "MONTHLY",
                "Monthly Plan",
                new BigDecimal("10.00"),
                "USD",
                "PayPal",
                "ORDER-123",
                "CAPTURE-123",
                LocalDateTime.now(),
                LocalDateTime.now());

        ByteArrayInputStream bis = invoicePdfService.generateInvoicePdf(invoice);
        assertNotNull(bis);
        assertTrue(bis.available() > 0, "PDF stream should not be empty");
    }
}
