package com.maharecruitment.gov.in.attendance.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.maharecruitment.gov.in.attendance.entity.HolidayMasterEntity;
import com.maharecruitment.gov.in.attendance.service.HolidayService;

@Controller
@RequestMapping("/hr/holidays")
public class HolidayMasterController {

    @Autowired
    private HolidayService holidayService;

    @GetMapping("")
    public String listHolidays(Model model) {
        List<HolidayMasterEntity> holidays = holidayService.getAllHolidays();
        model.addAttribute("holidays", holidays);
        model.addAttribute("pageTitle", "Holiday List");
        return "attendance/holiday-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("holiday", new HolidayMasterEntity());
        model.addAttribute("pageTitle", "Add New Holiday");
        return "attendance/holiday-form";
    }

    @PostMapping("/save")
    public String saveHoliday(@ModelAttribute HolidayMasterEntity holiday) {
        holidayService.saveHoliday(holiday);
        return "redirect:/hr/holidays";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        HolidayMasterEntity holiday = holidayService.getHolidayById(id);
        if (holiday == null) {
            return "redirect:/hr/holidays";
        }
        model.addAttribute("holiday", holiday);
        model.addAttribute("pageTitle", "Edit Holiday");
        return "attendance/holiday-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return "redirect:/hr/holidays";
    }
}
