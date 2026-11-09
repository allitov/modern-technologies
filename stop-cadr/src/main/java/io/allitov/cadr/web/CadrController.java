package io.allitov.cadr.web;

import io.allitov.cadr.domain.Asset;
import io.allitov.cadr.domain.Employee;
import io.allitov.cadr.service.AssetRow;
import io.allitov.cadr.service.AssetService;
import io.allitov.cadr.service.AssignmentService;
import io.allitov.cadr.service.EmployeeService;
import io.allitov.cadr.service.PagedAssets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CadrController {

    private final EmployeeService employeeService;
    private final AssetService assetService;
    private final AssignmentService assignmentService;

    private static final int PAGE_SIZE = 20;

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

    @GetMapping("/assets")
    public String assets(
            @RequestParam(name = "filter", defaultValue = "Все") String filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model
    ) {
        List<AssetRow> rows = new ArrayList<>();
        boolean hasNext = false;
        try {
            PagedAssets paged = assetService.listAssetsPaged(page, PAGE_SIZE, filter);
            if (paged.items().isEmpty() && page > 0) {
                page = 0;
                paged = assetService.listAssetsPaged(page, PAGE_SIZE, filter);
            }
            rows = paged.items();
            hasNext = paged.hasNext();
        } catch (RuntimeException e) {
            model.addAttribute("error", "Ошибка: " + e.getMessage());
        }
        model.addAttribute("rows", rows);
        model.addAttribute("filter", filter);
        model.addAttribute("filters", List.of("Все", "В стойке", "На съёмке"));
        model.addAttribute("page", page);
        model.addAttribute("hasNext", hasNext);
        model.addAttribute("activeEmployees", employeeService.activeEmployees());
        model.addAttribute("freeAssets", assetService.freeAssets());
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
            assetService.addAsset(inventoryNumber, model, category);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/copy")
    public String copyAsset(
            @RequestParam long assetId,
            @RequestParam String inventoryNumber,
            RedirectAttributes ra
    ) {
        try {
            assetService.duplicateAsset(assetId, inventoryNumber);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/delete")
    public String deleteAsset(@RequestParam long assetId, RedirectAttributes ra) {
        try {
            assetService.deleteAssetCascade(assetId);
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
            assignmentService.issue(assetId, employeeId);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/bulk-issue")
    public String bulkIssue(
            @RequestParam long employeeId,
            @RequestParam(name = "assetIds") List<Long> assetIds,
            RedirectAttributes ra
    ) {
        try {
            assignmentService.issueMultiple(assetIds, employeeId);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/reassign")
    public String reassignAsset(
            @RequestParam long assetId,
            @RequestParam long employeeId,
            RedirectAttributes ra
    ) {
        try {
            assignmentService.reassignAsset(assetId, employeeId);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/return")
    public String giveBack(@RequestParam long assetId, RedirectAttributes ra) {
        try {
            assignmentService.giveBack(assetId);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @GetMapping("/assets/search")
    public String searchAssets(
            @RequestParam(name = "searched", defaultValue = "false") boolean searched,
            @RequestParam(name = "category", defaultValue = "Все") String category,
            @RequestParam(name = "onlyFree", defaultValue = "false") boolean onlyFree,
            @RequestParam(name = "text", defaultValue = "") String text,
            Model model
    ) {
        List<Asset> results = new ArrayList<>();
        if (searched) {
            try {
                results = assetService.searchAssets("Все".equals(category) ? "" : category, onlyFree, text);
            } catch (RuntimeException e) {
                model.addAttribute("error", "Ошибка: " + e.getMessage());
            }
        }
        model.addAttribute("searched", searched);
        model.addAttribute("results", results);
        model.addAttribute("category", category);
        model.addAttribute("categories",
                List.of("Все", "Камера", "Объектив", "Вспышка", "Штатив", "Свет", "Другое"));
        model.addAttribute("onlyFree", onlyFree);
        model.addAttribute("text", text);
        return "asset-search";
    }

    @GetMapping("/assets/history")
    public String assetHistory(
            @RequestParam long id,
            @RequestParam(name = "inv", defaultValue = "") String inv,
            @RequestParam(name = "model", defaultValue = "") String assetModel,
            Model model
    ) {
        List<HistoryView> rows = new ArrayList<>();
        try {
            DateTimeFormatter date = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    .withZone(ZoneId.systemDefault());
            rows = assignmentService.assetHistory(id).stream()
                    .map(e -> new HistoryView(
                            e.employeeName(),
                            formatDate(e.assignment().getIssuedAt(), date),
                            formatDate(e.assignment().getReturnedAt(), date),
                            e.assignment().isActive()))
                    .toList();
        } catch (RuntimeException e) {
            model.addAttribute("error", "Ошибка: " + e.getMessage());
        }
        model.addAttribute("rows", rows);
        model.addAttribute("assetLabel", inv + " — " + assetModel);
        return "asset-history";
    }

    private boolean matches(Employee e, String search) {
        if (search.isBlank()) {
            return true;
        }
        String f = search.toLowerCase();
        return e.getFullName().toLowerCase().contains(f)
                || e.getEmail().toLowerCase().contains(f);
    }

    private String formatDate(Long epochSeconds, DateTimeFormatter formatter) {
        if (epochSeconds == null || epochSeconds == 0) {
            return "—";
        }
        return formatter.format(Instant.ofEpochSecond(epochSeconds));
    }
}
