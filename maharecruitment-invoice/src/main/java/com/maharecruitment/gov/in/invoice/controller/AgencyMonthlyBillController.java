package com.maharecruitment.gov.in.invoice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillListItemView;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillView;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.service.AgencyMonthlyBillQrCodeGenerator;
import com.maharecruitment.gov.in.invoice.service.AgencyMonthlyBillService;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;

@Controller
@RequestMapping("/agency/monthly-bills")
public class AgencyMonthlyBillController {

    private static final String BILL_BASE_PATH = "/agency/monthly-bills";
    private static final String AGENCY_ROLE_LABEL = "Agency";
    private static final String BILL_PAGE_TITLE = "Monthly Generated Bills";

    private final AgencyMonthlyBillService billService;
    private final AgencyMonthlyBillQrCodeGenerator qrCodeGenerator;
    private final UserAffiliationService userAffiliationService;
    private final UserRepository userRepository;
    private final AgencyMasterRepository agencyMasterRepository;

    public AgencyMonthlyBillController(
            AgencyMonthlyBillService billService,
            AgencyMonthlyBillQrCodeGenerator qrCodeGenerator,
            UserAffiliationService userAffiliationService,
            UserRepository userRepository,
            AgencyMasterRepository agencyMasterRepository) {
        this.billService = billService;
        this.qrCodeGenerator = qrCodeGenerator;
        this.userAffiliationService = userAffiliationService;
        this.userRepository = userRepository;
        this.agencyMasterRepository = agencyMasterRepository;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("generatedDate"), Sort.Order.desc("agencyMonthlyBillId")));
        Page<AgencyMonthlyBillListItemView> bills = loadAgencyBills(pageable, model);
        model.addAttribute("bills", bills);
        populateAgencyBillPageModel(model);
        return "invoice/agency-monthly-bill-list";
    }

    @GetMapping("/{billId}")
    public String view(@PathVariable Long billId, Model model) {
        AgencyMonthlyBillView bill = billService.getBillForAgency(billId, resolveAgencyId());
        String preparedByName = resolvePreparedByName(bill.getCreatedBy());
        model.addAttribute("bill", bill);
        model.addAttribute("agencyBillQrCodeDataUrl", qrCodeGenerator.generateDataUrl(bill, preparedByName));
        model.addAttribute("agencyBillAuthorityName", "Maharashtra Information Technology Corporation Ltd.");
        model.addAttribute("preparedByName", preparedByName);
        populateAgencyBillPageModel(model);
        return "invoice/agency-monthly-bill-detail";
    }

    private void populateAgencyBillPageModel(Model model) {
        model.addAttribute("pageRoleLabel", AGENCY_ROLE_LABEL);
        model.addAttribute("billPageTitle", BILL_PAGE_TITLE);
        model.addAttribute("billListTitle", "Monthly Generated Bills");
        model.addAttribute("billListDescription", "View monthly bills generated for your agency.");
        model.addAttribute("billBasePath", BILL_BASE_PATH);
        model.addAttribute("canGenerateBill", false);
        model.addAttribute("canDeleteBill", false);
        model.addAttribute("showSignatureApproval", false);
        model.addAttribute("qrSectionTitle", "6. QR Verification");
    }

    private Page<AgencyMonthlyBillListItemView> loadAgencyBills(Pageable pageable, Model model) {
        try {
            return billService.getGeneratedBillsForAgency(resolveAgencyId(), pageable);
        } catch (TaxInvoiceException | IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return Page.empty(pageable);
        }
    }

    private Long resolveAgencyId() {
        String actorEmail = resolveActorEmail();
        User user = userAffiliationService.loadUserByEmail(actorEmail);
        Long agencyId = userAffiliationService.resolvePrimaryAgencyId(user);
        if (agencyId != null) {
            return agencyId;
        }
        return agencyMasterRepository.findByOfficialEmailIgnoreCase(actorEmail)
                .map(agency -> agency.getAgencyId())
                .orElseThrow(() -> new TaxInvoiceException("No agency profile is linked with this login user."));
    }

    private String resolveActorEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new TaxInvoiceException("Authenticated agency user is required.");
        }
        return authentication.getName().trim();
    }

    private String resolvePreparedByName(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            return "-";
        }
        return userRepository.findByEmailIgnoreCase(actorEmail.trim())
                .map(user -> StringUtils.hasText(user.getName()) ? user.getName().trim() : actorEmail.trim())
                .orElse(actorEmail.trim());
    }
}
