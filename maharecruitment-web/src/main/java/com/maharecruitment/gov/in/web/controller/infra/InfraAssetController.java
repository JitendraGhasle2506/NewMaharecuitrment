package com.maharecruitment.gov.in.web.controller.infra;

import com.maharecruitment.gov.in.asset.entity.AssetCategoryEntity;
import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import com.maharecruitment.gov.in.asset.service.AssetService;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.service.verification.OtpRequestContext;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/infra/utilities/assets")
public class InfraAssetController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final List<Integer> PAGE_SIZES = List.of(10, 20, 50);

    private final AssetService assetService;
    private final EmployeeRepository employeeRepository;
    private final TransportSecurityProperties transportSecurityProperties;

    public InfraAssetController(AssetService assetService, EmployeeRepository employeeRepository,
            TransportSecurityProperties transportSecurityProperties) {
        this.assetService = assetService;
        this.employeeRepository = employeeRepository;
        this.transportSecurityProperties = transportSecurityProperties;
    }

    @GetMapping
    public String getAssetsDashboard(Model model) {
        model.addAttribute("categories", assetService.getAllActiveCategories());
        model.addAttribute("summary", assetService.getDashboardSummary());
        return "infra/utilities";
    }

    @GetMapping("/inventory")
    public String getAssetInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        int pageSize = normalizePageSize(size);
        model.addAttribute("assetPage", assetService.getAssets(PageRequest.of(
                Math.max(page, 0), pageSize, Sort.by(Sort.Direction.ASC, "assetCode"))));
        model.addAttribute("pageSizes", PAGE_SIZES);
        return "infra/assets/inventory";
    }

    @GetMapping("/allocations")
    public String getActiveAllocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            Model model) {
        int pageSize = normalizePageSize(size);
        String normalizedSearch = normalizeSearch(search);
        Sort allocationSort = Sort.by(
                Sort.Order.desc("allocatedDate"),
                Sort.Order.desc("id"));
        model.addAttribute("allocationPage", assetService.getActiveAllocations(normalizedSearch, PageRequest.of(
                Math.max(page, 0), pageSize, allocationSort)));
        model.addAttribute("search", normalizedSearch);
        model.addAttribute("availableAssets", assetService.getAvailableAssetOptions());
        model.addAttribute("employees", employeeRepository.findActiveEmployeeOptions("ACTIVE"));
        model.addAttribute("pageSizes", PAGE_SIZES);
        return "infra/assets/allocations";
    }

    @PostMapping("/category")
    public String addCategory(@ModelAttribute AssetCategoryEntity category, RedirectAttributes redirectAttributes) {
        try {
            assetService.createCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", "Asset category added successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/infra/utilities/assets";
    }

    @PostMapping("/add")
    public String addAsset(@ModelAttribute AssetEntity asset, Principal principal,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            assetService.createAsset(asset, actorUsername(principal), clientIp(request));
            redirectAttributes.addFlashAttribute("successMessage", "Asset added successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/infra/utilities/assets/inventory";
    }

    @PostMapping("/allocate")
    public String allocateAsset(@RequestParam Long assetId, @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate allocatedDate,
            @RequestParam(required = false) String remarks, Principal principal,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            assetService.allocateAsset(assetId, employeeId, allocatedDate, remarks,
                    actorUsername(principal), clientIp(request));
            redirectAttributes.addFlashAttribute("successMessage", "Asset allocated successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/infra/utilities/assets/allocations";
    }

    @PostMapping("/replace")
    public String replaceAsset(@RequestParam Long allocationId,
            @RequestParam Long replacementAssetId,
            @RequestParam String replacementReason,
            Principal principal, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            assetService.replaceAsset(allocationId, replacementAssetId, replacementReason,
                    actorUsername(principal), clientIp(request));
            redirectAttributes.addFlashAttribute("successMessage", "Asset replaced successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/infra/utilities/assets/allocations";
    }

    @PostMapping("/return")
    public String returnAsset(@RequestParam Long allocationId,
            @RequestParam(required = false) String remarks, Principal principal,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            assetService.returnAsset(allocationId, remarks, actorUsername(principal), clientIp(request));
            redirectAttributes.addFlashAttribute("successMessage", "Asset returned successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/infra/utilities/assets/allocations";
    }

    private String actorUsername(Principal principal) {
        return principal == null || principal.getName() == null || principal.getName().isBlank()
                ? "anonymous"
                : principal.getName();
    }

    private String clientIp(HttpServletRequest request) {
        return OtpRequestContext.from(request, transportSecurityProperties.isTrustForwardedHeaders())
                .normalizedClientIp();
    }

    private int normalizePageSize(int requestedSize) {
        return PAGE_SIZES.contains(requestedSize) ? requestedSize : DEFAULT_PAGE_SIZE;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return "";
        }
        String normalized = search.trim();
        return normalized.length() <= 150 ? normalized : normalized.substring(0, 150);
    }
}
