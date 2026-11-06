package io.allitov.cadr.web;

import io.allitov.cadr.domain.Employee;
import io.allitov.cadr.service.AssetRow;
import io.allitov.cadr.service.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CadrController {

    private final Service service;

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
            employees = service.employees().stream()
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
            service.addEmployee(fullName, email);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/employees";
    }

    @GetMapping("/employees/edit")
    public String editEmployee(@RequestParam long id, Model model) {
        Employee employee = service.employees().stream()
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
            service.updateEmployee(id, fullName, email, active);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/employees";
    }

    @GetMapping("/assets")
    public String assets(
            @RequestParam(name = "filter", defaultValue = "Все") String filter,
            Model model
    ) {
        List<AssetRow> rows = new ArrayList<>();
        try {
            rows = service.assets().stream()
                    .filter(r -> matches(r, filter))
                    .toList();
        } catch (RuntimeException e) {
            model.addAttribute("error", "Ошибка: " + e.getMessage());
        }
        model.addAttribute("rows", rows);
        model.addAttribute("filter", filter);
        model.addAttribute("filters", List.of("Все", "В стойке", "На съёмке"));
        model.addAttribute("activeEmployees", service.activeEmployees());
        model.addAttribute("freeAssets", service.freeAssets());
        return "assets";
    }

    @PostMapping("/assets/add")
    public String addAsset(
            @RequestParam String inventoryNumber,
            @RequestParam(name = "model") String model,
            @RequestParam String category,
            RedirectAttributes ra
    ) {
        if (inventoryNumber.isBlank()) {
            ra.addFlashAttribute("error", "Введите инв. номер");
            return "redirect:/assets";
        }
        if (model.isBlank()) {
            ra.addFlashAttribute("error", "Введите модель");
            return "redirect:/assets";
        }
        try {
            service.addAsset(inventoryNumber, model, category);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/issue")
    public String issue(
            @RequestParam long employeeId,
            @RequestParam long assetId,
            RedirectAttributes ra
    ) {
        try {
            service.issue(assetId, employeeId);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/return")
    public String giveBack(@RequestParam long assetId, RedirectAttributes ra) {
        try {
            service.giveBack(assetId);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    private boolean matches(Employee e, String search) {
        if (search.isBlank()) {
            return true;
        }
        String f = search.toLowerCase();
        return e.getFullName().toLowerCase().contains(f)
                || e.getEmail().toLowerCase().contains(f);
    }

    private boolean matches(AssetRow r, String filter) {
        boolean assigned = r.asset().isAssigned();
        return !("В стойке".equals(filter) && assigned)
                && !("На съёмке".equals(filter) && !assigned);
    }
}
