package com.maharecruitment.gov.in.web.controller.hr;

import java.net.URLConnection;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeHierarchyNode;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeReportingType;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeHierarchyRepository;
import com.maharecruitment.gov.in.recruitment.service.organization.EmployeeHierarchyService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Controller
@PreAuthorize("hasAuthority('ROLE_HR')")
public class EmployeeHierarchyController {
    private final EmployeeHierarchyService hierarchy;
    private final EmployeeHierarchyRepository repository;
    private final FileStorageService fileStorage;

    public EmployeeHierarchyController(EmployeeHierarchyService hierarchy, EmployeeHierarchyRepository repository,
                                       FileStorageService fileStorage) {
        this.hierarchy = hierarchy;
        this.repository = repository;
        this.fileStorage = fileStorage;
    }

    public record Result<T>(boolean success, String message, T data) {
        static <T> Result<T> of(T data) {
            return new Result<>(true, "Employee hierarchy fetched successfully", data);
        }
    }

    @GetMapping("/hr/employee-hierarchy")
    public String page(Model model) {
        model.addAttribute("sidebarActive", "Reporting Manager");
        return "hr/employee-hierarchy";
    }

    @GetMapping("/api/employees/hierarchy/options")
    @ResponseBody
    public Result<EmployeeHierarchyService.Options> options() {
        return Result.of(hierarchy.options());
    }

    @GetMapping("/api/employees/hierarchy/{hodEmployeeId}")
    @ResponseBody
    public Result<EmployeeHierarchyNode> tree(@PathVariable Long hodEmployeeId,
            @RequestParam(defaultValue = "PRIMARY") EmployeeReportingType reportingType,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "1") int depth) {
        return Result.of(hierarchy.tree(hodEmployeeId, nodeId, reportingType, departmentId, designationId,
                offset, limit, depth));
    }

    @GetMapping("/api/employees/hierarchy/{hodEmployeeId}/search")
    @ResponseBody
    public Result<List<EmployeeHierarchyService.SearchMatch>> search(@PathVariable Long hodEmployeeId,
            @RequestParam(defaultValue = "PRIMARY") EmployeeReportingType reportingType,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam String q) {
        return Result.of(hierarchy.search(hodEmployeeId, reportingType, departmentId, designationId, q));
    }

    /** HR-only proxy; never expose local filesystem paths or serve arbitrary uploaded documents. */
    @GetMapping("/api/employees/hierarchy/photo/{employeeId}")
    @ResponseBody
    public ResponseEntity<Resource> photo(@PathVariable Long employeeId) {
        Optional<Path> photo = repository.findPhotoSources(employeeId).stream()
                .flatMap(row -> Stream.of(row.getProfilePhoto(), row.getEmployeePhoto(), row.getOnboardingPhoto()))
                .filter(Objects::nonNull).filter(path -> !path.isBlank())
                .filter(path -> fileStorage.isManagedFileAllowed(path, "employee-profile-photo"))
                .map(fileStorage::resolveManagedPath).flatMap(Optional::stream).findFirst();
        if (photo.isEmpty()) return ResponseEntity.notFound().build();
        Path path = photo.get();
        String contentType = URLConnection.guessContentTypeFromName(path.getFileName().toString());
        if (!List.of("image/png", "image/jpeg", "image/gif", "image/webp").contains(contentType == null ? "" : contentType)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(path);
        if (!resource.isReadable()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff").body(resource);
    }
}
