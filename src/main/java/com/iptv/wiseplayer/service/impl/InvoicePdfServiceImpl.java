package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.dto.response.InvoiceResponse;
import com.iptv.wiseplayer.service.InvoicePdfService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

@Service
public class InvoicePdfServiceImpl implements InvoicePdfService {

    @Override
    public ByteArrayInputStream generateInvoicePdf(InvoiceResponse invoice) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font styles
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Title
            Paragraph title = new Paragraph("INVOICE", headerFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Invoice Info Table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);

            addCell(infoTable, "Invoice Number:", subHeaderFont);
            addCell(infoTable, invoice.getInvoiceNumber(), normalFont);

            addCell(infoTable, "Date:", subHeaderFont);
            addCell(infoTable, invoice.getTransactionDate().toString(), normalFont);

            addCell(infoTable, "Device ID:", subHeaderFont);
            addCell(infoTable, invoice.getDeviceId().toString(), normalFont);

            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            // Plan Info Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            Stream.of("Plan", "Description", "Currency", "Amount")
                    .forEach(columnTitle -> {
                        PdfPCell header = new PdfPCell();
                        header.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                        header.setBorderWidth(2);
                        header.setPhrase(new Phrase(columnTitle, subHeaderFont));
                        table.addCell(header);
                    });

            table.addCell(new Phrase(invoice.getPlanName(), normalFont));
            table.addCell(new Phrase(invoice.getPlanDisplayName(), normalFont));
            table.addCell(new Phrase(invoice.getCurrency(), normalFont));
            table.addCell(new Phrase(invoice.getAmount().toString(), normalFont));

            document.add(table);

            document.add(Chunk.NEWLINE);
            Paragraph total = new Paragraph("Total: " + invoice.getAmount() + " " + invoice.getCurrency(),
                    subHeaderFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            // Footer
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Thank you for choosing WisePlayer!", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

        } catch (DocumentException ex) {
            throw new RuntimeException("Error generating PDF", ex);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }
}
