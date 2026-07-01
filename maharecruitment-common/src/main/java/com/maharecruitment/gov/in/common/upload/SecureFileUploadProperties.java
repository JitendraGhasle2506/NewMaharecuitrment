package com.maharecruitment.gov.in.common.upload;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "secure.upload")
public class SecureFileUploadProperties {

    private String basePath = Paths.get(System.getProperty("user.home"), "uploaded-files").toString();

    private int fileNameMaxLength = 100;

    private DataSize defaultMaxSize = DataSize.ofMegabytes(2);

    private Set<String> allowedExtensions = new LinkedHashSet<>(List.of("pdf", "jpg", "jpeg", "png"));

    private Set<String> blockedExtensions = new LinkedHashSet<>(List.of(
            "exe", "jsp", "php", "js", "html", "htm", "svg", "sh", "bat", "cmd",
            "com", "scr", "jar", "war", "zip", "rar", "7z", "xml"));

    private Map<String, DataSize> maxSizeByExtension = new LinkedHashMap<>();

    private Map<String, List<String>> allowedMimeTypes = new LinkedHashMap<>();

    public SecureFileUploadProperties() {
        maxSizeByExtension.put("pdf", DataSize.ofMegabytes(2));
        maxSizeByExtension.put("jpg", DataSize.ofMegabytes(1));
        maxSizeByExtension.put("jpeg", DataSize.ofMegabytes(1));
        maxSizeByExtension.put("png", DataSize.ofMegabytes(1));
        maxSizeByExtension.put("doc", DataSize.ofMegabytes(5));
        maxSizeByExtension.put("docx", DataSize.ofMegabytes(5));

        allowedMimeTypes.put("pdf", new ArrayList<>(List.of("application/pdf")));
        allowedMimeTypes.put("jpg", new ArrayList<>(List.of("image/jpeg", "image/jpg")));
        allowedMimeTypes.put("jpeg", new ArrayList<>(List.of("image/jpeg", "image/jpg")));
        allowedMimeTypes.put("png", new ArrayList<>(List.of("image/png")));
        allowedMimeTypes.put("doc", new ArrayList<>(List.of("application/msword")));
        allowedMimeTypes.put("docx", new ArrayList<>(List.of(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")));
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public int getFileNameMaxLength() {
        return fileNameMaxLength;
    }

    public void setFileNameMaxLength(int fileNameMaxLength) {
        this.fileNameMaxLength = fileNameMaxLength;
    }

    public DataSize getDefaultMaxSize() {
        return defaultMaxSize;
    }

    public void setDefaultMaxSize(DataSize defaultMaxSize) {
        this.defaultMaxSize = defaultMaxSize;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(Set<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public Set<String> getBlockedExtensions() {
        return blockedExtensions;
    }

    public void setBlockedExtensions(Set<String> blockedExtensions) {
        this.blockedExtensions = blockedExtensions;
    }

    public Map<String, DataSize> getMaxSizeByExtension() {
        return maxSizeByExtension;
    }

    public void setMaxSizeByExtension(Map<String, DataSize> maxSizeByExtension) {
        this.maxSizeByExtension = maxSizeByExtension;
    }

    public Map<String, List<String>> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(Map<String, List<String>> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }
}
