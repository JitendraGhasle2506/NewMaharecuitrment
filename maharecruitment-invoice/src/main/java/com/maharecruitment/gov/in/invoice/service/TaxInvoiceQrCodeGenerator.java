package com.maharecruitment.gov.in.invoice.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Base64;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;

@Component
public class TaxInvoiceQrCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(TaxInvoiceQrCodeGenerator.class);
    private static final int QR_SIZE = 240;
    private static final String DATA_URL_PREFIX = "data:image/png;base64,";
    private static final String ZXING_QR_WRITER = "com.google.zxing.qrcode.QRCodeWriter";
    private static final String ZXING_BARCODE_FORMAT = "com.google.zxing.BarcodeFormat";
    private static final String ZXING_BIT_MATRIX = "com.google.zxing.common.BitMatrix";
    private static final String ZXING_MATRIX_TO_IMAGE_WRITER = "com.google.zxing.client.j2se.MatrixToImageWriter";

    public byte[] generatePngBytes(TaxInvoiceView invoice) {
        if (invoice == null) {
            return null;
        }

        String payload = buildPayload(invoice);
        if (!StringUtils.hasText(payload)) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Object matrix = createBitMatrix(payload);
            if (matrix == null) {
                return null;
            }
            writePng(matrix, outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new TaxInvoiceException("Unable to generate tax invoice QR code.", ex);
        } catch (ReflectiveOperationException | LinkageError ex) {
            log.warn("Tax invoice QR code generation is unavailable: {}", ex.getMessage());
            return null;
        }
    }

    public String generateDataUrl(TaxInvoiceView invoice) {
        byte[] pngBytes = generatePngBytes(invoice);
        return pngBytes == null ? null : DATA_URL_PREFIX + Base64.getEncoder().encodeToString(pngBytes);
    }

    private String buildPayload(TaxInvoiceView invoice) {
        StringBuilder payload = new StringBuilder();
        appendLine(payload, "MAHAIT TAX INVOICE");
        appendLine(payload, "AUTHORITY_NAME", resolveAuthorityName(invoice));
        appendLine(payload, "PROJECT_NAME", invoice.getProjectName());
        appendLine(payload, "TI_NO", invoice.getTiNumber());
        appendLine(payload, "REQUEST_ID", invoice.getRequestId());
        appendLine(payload, "TI_DATE", formatDate(invoice.getTiDate()));
        appendLine(payload, "BASE_AMOUNT", formatAmount(invoice.getBaseAmount()));
        appendLine(payload, "TAX_AMOUNT", formatAmount(invoice.getTaxAmount()));
        appendLine(payload, "GRAND_TOTAL", formatAmount(invoice.getTotalAmount()));
        appendLine(payload, "APPLICATION_ID", safeNumber(invoice.getDepartmentProjectApplicationId()));
        appendLine(payload, "BILLED_TO", invoice.getBilledTo());
        appendLine(payload, "GSTIN", invoice.getGstNumber());
        return payload.toString().trim();
    }

    private String resolveAuthorityName(TaxInvoiceView invoice) {
        String authorityName = normalize(invoice == null ? null : invoice.getCompanyName());
        if (StringUtils.hasText(authorityName)) {
            return authorityName;
        }

        authorityName = normalize(invoice == null ? null : invoice.getAccountHolderName());
        if (StringUtils.hasText(authorityName)) {
            return authorityName;
        }

        return "";
    }

    private Object createBitMatrix(String payload) throws ReflectiveOperationException {
        Class<?> writerClass = Class.forName(ZXING_QR_WRITER);
        Object writer = writerClass.getConstructor().newInstance();
        Class<?> barcodeFormatClass = Class.forName(ZXING_BARCODE_FORMAT);
        Object qrCodeFormat = Enum.valueOf(barcodeFormatClass.asSubclass(Enum.class), "QR_CODE");
        Method encodeMethod = writerClass.getMethod("encode", String.class, barcodeFormatClass, int.class, int.class);
        return encodeMethod.invoke(writer, payload, qrCodeFormat, QR_SIZE, QR_SIZE);
    }

    private void writePng(Object matrix, ByteArrayOutputStream outputStream) throws ReflectiveOperationException {
        Class<?> matrixClass = Class.forName(ZXING_BIT_MATRIX);
        Class<?> writerClass = Class.forName(ZXING_MATRIX_TO_IMAGE_WRITER);
        Method writeToStreamMethod = writerClass.getMethod("writeToStream", matrixClass, String.class,
                java.io.OutputStream.class);
        writeToStreamMethod.invoke(null, matrix, "PNG", outputStream);
    }

    private void appendLine(StringBuilder payload, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (payload.length() > 0) {
            payload.append('\n');
        }
        payload.append(normalize(value));
    }

    private void appendLine(StringBuilder payload, String key, String value) {
        String normalizedValue = normalize(value);
        if (!StringUtils.hasText(normalizedValue)) {
            return;
        }
        if (payload.length() > 0) {
            payload.append('\n');
        }
        payload.append(key).append('=').append(normalizedValue);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim()
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('|', ' ');
        return normalized.replaceAll("\\s+", " ");
    }

    private String safeNumber(Long value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
