package com.maharecruitment.gov.in.web.controller.employee;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeTaskLogDto;
import com.maharecruitment.gov.in.recruitment.dto.employee.TaskSubmissionForm;
import com.maharecruitment.gov.in.web.service.employee.EmployeeTaskService;

@Controller
@RequestMapping("/employee/tasks")
@PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
public class EmployeeTaskController {

    private static final int PAGE_SIZE = 10;

    private final EmployeeTaskService employeeTaskService;
    private final ProjectMstRepository projectMstRepository;

    public EmployeeTaskController(EmployeeTaskService employeeTaskService,
                                  ProjectMstRepository projectMstRepository) {
        this.employeeTaskService = employeeTaskService;
        this.projectMstRepository = projectMstRepository;
    }

    @GetMapping
    public String viewTaskTracker(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model, Principal principal) {

        String loginEmail = principal != null ? principal.getName() : null;

        TaskSubmissionForm form = new TaskSubmissionForm();
        EmployeeTaskLogDto initialTask = new EmployeeTaskLogDto();
        initialTask.setSelected(true);
        form.getTaskList().add(initialTask);

        int targetMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        int targetYear  = (year  != null) ? year  : LocalDate.now().getYear();

        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate minDate   = yearMonth.atDay(1);
        LocalDate maxDate   = yearMonth.atEndOfMonth();

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "taskDate"));
        Page<EmployeeTaskLogDto> taskPage = employeeTaskService.getRecentTasks(loginEmail, month, year, pageable);

        List<ProjectMst> allProjects = projectMstRepository.findAll(Sort.by(Sort.Direction.ASC, "projectName"));

        model.addAttribute("projectList",   allProjects);
        model.addAttribute("taskForm",      form);
        model.addAttribute("recentTasks",   taskPage.getContent());
        model.addAttribute("taskPage",      taskPage);
        model.addAttribute("currentPage",   taskPage.getNumber());
        model.addAttribute("totalPages",    taskPage.getTotalPages());
        model.addAttribute("selectedMonth", targetMonth);
        model.addAttribute("selectedYear",  targetYear);
        model.addAttribute("minDate",       minDate.toString());
        model.addAttribute("maxDate",       maxDate.toString());
        model.addAttribute("pageHeading",   "My Tasks");
        model.addAttribute("pageSubtitle",  "Manage your daily/weekly tasks here.");

        return "employee/task-tracker";
    }

    @PostMapping("/save")
    public String saveTasks(@ModelAttribute("taskForm") TaskSubmissionForm taskForm,
                            @RequestParam(value = "month", required = false) Integer month,
                            @RequestParam(value = "year",  required = false) Integer year,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        String loginEmail = principal != null ? principal.getName() : null;
        try {
            if (month != null && year != null && taskForm.getTaskList() != null) {
                if ("DAILY".equals(taskForm.getEntryMode())) {
                    if (taskForm.getGlobalTaskDate() == null) {
                        throw new IllegalArgumentException("Global task date is required for Daily mode.");
                    }
                    if (taskForm.getGlobalTaskDate().getMonthValue() != month || taskForm.getGlobalTaskDate().getYear() != year) {
                        throw new IllegalArgumentException("Global Date must be within selected month and year.");
                    }
                }

                boolean anySelected = false;
                for (EmployeeTaskLogDto task : taskForm.getTaskList()) {
                    if (task.isSelected()) {
                        anySelected = true;
                        if ("DAILY".equals(taskForm.getEntryMode())) {
                            task.setTaskDate(taskForm.getGlobalTaskDate());
                        } else {
                            if (task.getTaskDate() == null) {
                                throw new IllegalArgumentException("Task date is required for each selected row in this mode.");
                            }
                            if (task.getTaskDate().getMonthValue() != month || task.getTaskDate().getYear() != year) {
                                throw new IllegalArgumentException("Row Date must be within selected month and year.");
                            }
                        }
                    }
                }
                if (!anySelected) {
                    throw new IllegalArgumentException("No tasks selected for submission.");
                }
            }
            employeeTaskService.saveTasks(taskForm, loginEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Tasks saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving tasks: " + e.getMessage());
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/employee/tasks");
        if (month != null && year != null) {
            redirectUrl.append("?month=").append(month).append("&year=").append(year);
        }
        return redirectUrl.toString();
    }

    @GetMapping("/edit/{taskId}")
    public String editTask(@PathVariable Long taskId, Model model, Principal principal,
                           RedirectAttributes redirectAttributes) {
        String loginEmail = principal != null ? principal.getName() : null;
        try {
            // Fetch just this one task by iterating all (small overhead; task IDs are unique per employee)
            Pageable all = PageRequest.of(0, Integer.MAX_VALUE);
            EmployeeTaskLogDto taskDto = employeeTaskService.getRecentTasks(loginEmail, null, null, all)
                    .getContent().stream()
                    .filter(t -> t.getTaskId().equals(taskId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Task not found or unauthorized"));

            if ("APPROVED".equalsIgnoreCase(taskDto.getStatus())) {
                throw new IllegalArgumentException("Cannot edit an approved task");
            }

            taskDto.setSelected(true);

            TaskSubmissionForm form = new TaskSubmissionForm();
            form.setGlobalTaskDate(taskDto.getTaskDate());
            form.getTaskList().add(taskDto);

            int targetMonth = taskDto.getTaskDate().getMonthValue();
            int targetYear  = taskDto.getTaskDate().getYear();

            YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
            LocalDate minDate   = yearMonth.atDay(1);
            LocalDate maxDate   = yearMonth.atEndOfMonth();

            Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "taskDate"));
            Page<EmployeeTaskLogDto> taskPage = employeeTaskService.getRecentTasks(loginEmail, targetMonth, targetYear, pageable);

            List<ProjectMst> allProjects = projectMstRepository.findAll(Sort.by(Sort.Direction.ASC, "projectName"));

            model.addAttribute("projectList",   allProjects);
            model.addAttribute("taskForm",      form);
            model.addAttribute("recentTasks",   taskPage.getContent());
            model.addAttribute("taskPage",      taskPage);
            model.addAttribute("currentPage",   taskPage.getNumber());
            model.addAttribute("totalPages",    taskPage.getTotalPages());
            model.addAttribute("selectedMonth", targetMonth);
            model.addAttribute("selectedYear",  targetYear);
            model.addAttribute("minDate",       minDate.toString());
            model.addAttribute("maxDate",       maxDate.toString());
            model.addAttribute("pageHeading",   "Edit Task");
            model.addAttribute("pageSubtitle",  "Update your submitted task details.");
            model.addAttribute("isEditMode",    true);

            return "employee/task-tracker";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/employee/tasks";
        }
    }

    @GetMapping("/get-in-time")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getInTime(@RequestParam("date") String dateString, Principal principal) {
        String loginEmail = principal != null ? principal.getName() : null;
        String inTime = employeeTaskService.fetchInTime(loginEmail, dateString);
        Map<String, String> response = new HashMap<>();
        response.put("inTime", inTime);
        return ResponseEntity.ok(response);
    }
}
