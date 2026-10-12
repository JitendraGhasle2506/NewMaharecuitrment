package com.maharecruitment.gov.in.web.controller.infra;

import com.maharecruitment.gov.in.asset.entity.AssetCategoryEntity;
import com.maharecruitment.gov.in.asset.entity.AssetEntity;
import com.maharecruitment.gov.in.asset.service.AssetService;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/infra/utilities/assets")
public class InfraAssetController {

    private final AssetService assetService;
    private final EmployeeRepository employeeRepository;

    public InfraAssetController(AssetService assetService, EmployeeRepository employeeRepository) {
        this.assetService = assetService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public String getAssetsDashboard(Model model) {
        model.addAttribute("categories", assetService.getAllActiveCategories());
        model.addAttribute("assets", assetService.getAllAssets());
        model.addAttribute("allocations", assetService.getAssetAllocations());
        model.addAttribute("employees", employeeRepository.findActiveEmployeeOptions("ACTIVE"));
        return "infra/utilities";
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
    public String addAsset(@ModelAttribute AssetEntity asset, RedirectAttributes redirectAttributes) {
        try {
            assetService.createAsset(asset);
            redirectAttributes.addFlashAttribute("successMessage", "Asset added successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/infra/utilities/assets";
    }

    @PostMapping("/allocate")
    public String allocateAsset(@RequestParam Long assetId, @RequestParam Long employeeId, @RequestParam(required = false) String remarks, RedirectAttributes redirectAttributes) {
        try {
            assetService.allocateAsset(assetId, employeeId, remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Asset allocated successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/infra/utilities/assets";
    }

    @PostMapping("/return")
    public String returnAsset(@RequestParam Long allocationId, @RequestParam(required = false) String remarks, RedirectAttributes redirectAttributes) {
        try {
            assetService.returnAsset(allocationId, remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Asset returned successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/infra/utilities/assets";
    }
}
