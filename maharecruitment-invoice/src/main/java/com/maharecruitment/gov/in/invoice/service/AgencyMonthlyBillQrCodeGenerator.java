package com.maharecruitment.gov.in.invoice.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillView;

@Component
public class AgencyMonthlyBillQrCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(AgencyMonthlyBillQrCodeGenerator.class);
    private static final int QR_SIZE = 360;
    private static final String DATA_URL_PREFIX = "data:image/png;base64,";
    private static final String AUTHORITY_NAME = "Maharashtra Information Technology Corporation Ltd.";
    private static final String ZXING_QR_WRITER = "com.google.zxing.qrcode.QRCodeWriter";
    private static final String ZXING_BARCODE_FORMAT = "com.google.zxing.BarcodeFormat";
    private static final String ZXING_BIT_MATRIX = "com.google.zxing.common.BitMatrix";
    private static final String ZXING_MATRIX_TO_IMAGE_WRITER = "com.google.zxing.client.j2se.MatrixToImageWriter";
    private static final String ZXING_ENCODE_HINT_TYPE = "com.google.zxing.EncodeHintType";
    private static final String ZXING_ERROR_CORRECTION_LEVEL = "com.google.zxing.qrcode.decoder.ErrorCorrectionLevel";

    public String generateDataUrl(AgencyMonthlyBillView bill) {
        return generateDataUrl(bill, bill == null ? null : bill.getCreatedBy());
    }

    public String generateDataUrl(AgencyMonthlyBillView bill, String preparedByName) {
        byte[] pngBytes = generatePngBytes(bill, preparedByName);
        return pngBytes == null ? null : DATA_URL_PREFIX + Base64.getEncoder().encodeToString(pngBytes);
    }

    public byte[] generatePngBytes(AgencyMonthlyBillView bill) {
        return generatePngBytes(bill, bill == null ? null : bill.getCreatedBy());
    }

    public byte[] generatePngBytes(AgencyMonthlyBillView bill, String preparedByName) {
        if (bill == null) {
            return null;
        }

        String payload = buildPayload(bill, preparedByName);
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
            throw new IllegalStateException("Unable to generate agency monthly bill QR code.", ex);
        } catch (ReflectiveOperationException | LinkageError ex) {
            log.warn("Agency monthly bill QR code generation is unavailable: {}", ex.getMessage());
            return null;
        }
    }

    private String buildPayload(AgencyMonthlyBillView bill, String preparedByName) {
        StringBuilder payload = new StringBuilder();
        appendLine(payload, "AMB");
        appendLine(payload, "ID:" + safeNumber(bill.getAgencyMonthlyBillId()));
        appendLine(payload, "NO:" + normalize(bill.getBillNumber()));
        appendLine(payload, "DATE:" + formatDate(bill.getGeneratedDate()));
        appendLine(payload, "TOTAL:" + formatAmount(bill.getTotalAmount()));
        appendLine(payload, "AUTH:MAHAIT");
        appendLine(payload, "BY:" + normalize(preparedByName));
        return payload.toString().trim();
    }

    private Object createBitMatrix(String payload) throws ReflectiveOperationException {
        Class<?> writerClass = Class.forName(ZXING_QR_WRITER);
        Object writer = writerClass.getConstructor().newInstance();

        Class<?> barcodeFormatClass = Class.forName(ZXING_BARCODE_FORMAT);
        Object qrCodeFormat = Enum.valueOf(barcodeFormatClass.asSubclass(Enum.class), "QR_CODE");

        Class<?> hintTypeClass = Class.forName(ZXING_ENCODE_HINT_TYPE);
        Class<?> errorCorrectionLevelClass = Class.forName(ZXING_ERROR_CORRECTION_LEVEL);

        java.util.Map<Object, Object> hints = new java.util.HashMap<>();
        hints.put(Enum.valueOf(hintTypeClass.asSubclass(Enum.class), "MARGIN"), 2);
        hints.put(Enum.valueOf(hintTypeClass.asSubclass(Enum.class), "ERROR_CORRECTION"),
                Enum.valueOf(errorCorrectionLevelClass.asSubclass(Enum.class), "M"));

        Method encodeMethod = writerClass.getMethod("encode", String.class, barcodeFormatClass, int.class, int.class,
                java.util.Map.class);
        return encodeMethod.invoke(writer, payload, qrCodeFormat, QR_SIZE, QR_SIZE, hints);
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
