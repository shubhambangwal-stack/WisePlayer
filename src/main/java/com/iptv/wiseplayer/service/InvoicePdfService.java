package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.response.InvoiceResponse;
import java.io.ByteArrayInputStream;

public interface InvoicePdfService {
    ByteArrayInputStream generateInvoicePdf(InvoiceResponse invoice);
}
