package com.maharecruitment.gov.in.web.dto.registration;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public class DepartmentRegistrationForm {

    private static final Long OTHER_OPTION_ID = -1L;

    @NotNull(message = "Department selection is required")
    private Long departmentId;

    private Long subDeptId;

    @Size(max = 100, message = "New department name must not exceed 100 characters")
    private String newDepartmentName;

    @Size(max = 100, message = "New sub-department name must not exceed 100 characters")
    private String newSubDeptName;

    @NotBlank(message = "Office address is required")
    @Size(min = 10, max = 250, message = "Office address must be between 10 and 250 characters")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9\\s,\\.\\-\\/]*$", message = "Office address cannot start with a space or contain invalid special characters")
    private String address;

    @NotBlank(message = "Primary contact name is required")
    @Size(max = 100, message = "Primary contact name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z\\s]*$", message = "Primary contact name must start with a letter and contain only alphabets and spaces")
    private String primaryContactName;

    @NotBlank(message = "Primary designation is required")
    @Size(max = 100, message = "Primary designation must not exceed 100 characters")
    private String primaryDesignation;

    @NotBlank(message = "Primary mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Primary mobile number must be 10 digits")
    private String primaryMobile;

    @NotBlank(message = "Primary email is required")
    @Email(message = "Primary email must be valid")
    @Size(max = 150, message = "Primary email must not exceed 150 characters")
    private String primaryEmail;

    @NotBlank(message = "Secondary contact name is required")
    @Size(max = 100, message = "Secondary contact name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z\\s]*$", message = "Secondary contact name must start with a letter and contain only alphabets and spaces")
    private String secondaryContactName;

    @NotBlank(message = "Secondary designation is required")
    @Size(max = 100, message = "Secondary designation must not exceed 100 characters")
    private String secondaryDesignation;

    @NotBlank(message = "Secondary mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Secondary mobile number must be 10 digits")
    private String secondaryMobile;

    @NotBlank(message = "Secondary email is required")
    @Email(message = "Secondary email must be valid")
    @Size(max = 150, message = "Secondary email must not exceed 150 characters")
    private String secondaryEmail;

    @NotBlank(message = "Department name for bill is required")
    @Size(max = 200, message = "Department name for bill must not exceed 200 characters")
    private String billDepartmentName;

    @NotBlank(message = "Encrypted GST number is required")
    @Size(max = 800, message = "Encrypted GST number is invalid")
    private String gstNumberEncrypted;

    @NotBlank(message = "Encrypted PAN number is required")
    @Size(max = 800, message = "Encrypted PAN number is invalid")
    private String panNumberEncrypted;

    @NotBlank(message = "Encrypted TAN number is required")
    @Size(max = 800, message = "Encrypted TAN number is invalid")
    private String tanNumberEncrypted;

    @NotBlank(message = "Encryption key identifier is required")
    @Size(max = 100, message = "Encryption key identifier is invalid")
    private String encryptionKeyId;

    @NotNull(message = "Encrypted request timestamp is required")
    private Long timestamp;

    @NotBlank(message = "Encrypted request nonce is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{22,128}$", message = "Encrypted request nonce is invalid")
    private String nonce;

    private String tanNo;

    @NotBlank(message = "Billing address is required")
    @Size(max = 500, message = "Billing address must not exceed 500 characters")
    private String billAddress;

    private MultipartFile gstFile;

    private MultipartFile panFile;

    private MultipartFile tanFile;

    @NotNull(message = "Terms acceptance is required")
    @AssertTrue(message = "You must accept the declaration")
    private Boolean isTermsConditionAccepted;

    public boolean isOtherDepartmentSelected() {
        return OTHER_OPTION_ID.equals(departmentId);
    }

    public boolean isOtherSubDepartmentSelected() {
        return OTHER_OPTION_ID.equals(subDeptId);
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getSubDeptId() {
        return subDeptId;
    }

    public void setSubDeptId(Long subDeptId) {
        this.subDeptId = subDeptId;
    }

    public String getNewDepartmentName() {
        return newDepartmentName;
    }

    public void setNewDepartmentName(String newDepartmentName) {
        this.newDepartmentName = newDepartmentName;
    }

    public String getNewSubDeptName() {
        return newSubDeptName;
    }

    public void setNewSubDeptName(String newSubDeptName) {
        this.newSubDeptName = newSubDeptName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPrimaryContactName() {
        return primaryContactName;
    }

    public void setPrimaryContactName(String primaryContactName) {
        this.primaryContactName = primaryContactName;
    }

    public String getPrimaryDesignation() {
        return primaryDesignation;
    }

    public void setPrimaryDesignation(String primaryDesignation) {
        this.primaryDesignation = primaryDesignation;
    }

    public String getPrimaryMobile() {
        return primaryMobile;
    }

    public void setPrimaryMobile(String primaryMobile) {
        this.primaryMobile = primaryMobile;
    }

    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public void setPrimaryEmail(String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public String getSecondaryContactName() {
        return secondaryContactName;
    }

    public void setSecondaryContactName(String secondaryContactName) {
        this.secondaryContactName = secondaryContactName;
    }

    public String getSecondaryDesignation() {
        return secondaryDesignation;
    }

    public void setSecondaryDesignation(String secondaryDesignation) {
        this.secondaryDesignation = secondaryDesignation;
    }

    public String getSecondaryMobile() {
        return secondaryMobile;
    }

    public void setSecondaryMobile(String secondaryMobile) {
        this.secondaryMobile = secondaryMobile;
    }

    public String getSecondaryEmail() {
        return secondaryEmail;
    }

    public void setSecondaryEmail(String secondaryEmail) {
        this.secondaryEmail = secondaryEmail;
    }

    public String getBillDepartmentName() {
        return billDepartmentName;
    }

    public void setBillDepartmentName(String billDepartmentName) {
        this.billDepartmentName = billDepartmentName;
    }

    public String getGstNumberEncrypted() {
        return gstNumberEncrypted;
    }

    public void setGstNumberEncrypted(String gstNumberEncrypted) {
        this.gstNumberEncrypted = gstNumberEncrypted;
    }

    public String getPanNumberEncrypted() {
        return panNumberEncrypted;
    }

    public void setPanNumberEncrypted(String panNumberEncrypted) {
        this.panNumberEncrypted = panNumberEncrypted;
    }

    public String getTanNumberEncrypted() {
        return tanNumberEncrypted;
    }

    public void setTanNumberEncrypted(String tanNumberEncrypted) {
        this.tanNumberEncrypted = tanNumberEncrypted;
    }

    public String getEncryptionKeyId() {
        return encryptionKeyId;
    }

    public void setEncryptionKeyId(String encryptionKeyId) {
        this.encryptionKeyId = encryptionKeyId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public void clearEncryptedSubmission() {
        gstNumberEncrypted = null;
        panNumberEncrypted = null;
        tanNumberEncrypted = null;
        encryptionKeyId = null;
        timestamp = null;
        nonce = null;
    }

    public String getTanNo() {
        return tanNo;
    }

    public void setTanNo(String tanNo) {
        this.tanNo = tanNo;
    }

    public String getBillAddress() {
        return billAddress;
    }

    public void setBillAddress(String billAddress) {
        this.billAddress = billAddress;
    }

    public MultipartFile getGstFile() {
        return gstFile;
    }

    public void setGstFile(MultipartFile gstFile) {
        this.gstFile = gstFile;
    }

    public MultipartFile getPanFile() {
        return panFile;
    }

    public void setPanFile(MultipartFile panFile) {
        this.panFile = panFile;
    }

    public MultipartFile getTanFile() {
        return tanFile;
    }

    public void setTanFile(MultipartFile tanFile) {
        this.tanFile = tanFile;
    }

    public Boolean getIsTermsConditionAccepted() {
        return isTermsConditionAccepted;
    }

    public void setIsTermsConditionAccepted(Boolean isTermsConditionAccepted) {
        this.isTermsConditionAccepted = isTermsConditionAccepted;
    }
}
