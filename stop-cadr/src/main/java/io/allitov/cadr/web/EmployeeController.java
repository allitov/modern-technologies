package io.allitov.cadr.web;

import io.allitov.cadr.domain.Employee;
import io.allitov.cadr.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Контроллер работы с фотографами: список, добавление и редактирование.
 */
@Controller
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/")
    public String index() {
        return "redirect:/employees";
    }

    @GetMapping("/employees")
    public String employees(
            @RequestParam(name = "search", defaultValue = "") String search,
            Model model
    ) {
        List<Employee> employees = new ArrayList<>();
        try {
            employees = employeeService.employees().stream()
                    .filter(e -> matches(e, search))
                    .toList();
        } catch (RuntimeException e) {
            model.addAttribute("error", "Ошибка: " + e.getMessage());
        }
        model.addAttribute("employees", employees);
        model.addAttribute("search", search);
        return "employees";
    }

    @PostMapping("/employees/add")
    public String addEmployee(
            @RequestParam String fullName,
            @RequestParam String email,
            RedirectAttributes ra
    ) {
        if (fullName.isBlank()) {
            ra.addFlashAttribute("error", "Введите ФИО");
            return "redirect:/employees";
        }
        if (email.isBlank() || !email.contains("@")) {
            ra.addFlashAttribute("error", "Введите корректный email");
            return "redirect:/employees";
        }
        try {
            employeeService.addEmployee(fullName, email);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/employees";
    }

    @GetMapping("/employees/edit")
    public String editEmployee(@RequestParam long id, Model model) {
        Employee employee = employeeService.employees().stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElse(null);
        if (employee == null) {
            return "redirect:/employees";
        }
        model.addAttribute("employee", employee);
        return "employee-edit";
    }

    @PostMapping("/employees/update")
    public String updateEmployee(
            @RequestParam long id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(name = "active", defaultValue = "false") boolean active,
            RedirectAttributes ra
    ) {
        if (fullName.isBlank()) {
            ra.addFlashAttribute("error", "Введите ФИО");
            return "redirect:/employees/edit?id=" + id;
        }
        if (email.isBlank() || !email.contains("@")) {
            ra.addFlashAttribute("error", "Введите корректный email");
            return "redirect:/employees/edit?id=" + id;
        }
        try {
            employeeService.updateEmployee(id, fullName, email, active);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
            return "redirect:/employees/edit?id=" + id;
        }
        return "redirect:/employees";
    }

    private boolean matches(Employee e, String search) {
        if (search.isBlank()) {
            return true;
        }
        String f = search.toLowerCase();
        return e.getFullName().toLowerCase().contains(f)
                || e.getEmail().toLowerCase().contains(f);
    }
}
