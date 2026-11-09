package io.allitov.cadr.web;

import io.allitov.cadr.service.AssignmentService;
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

/**
 * Контроллер работы с выдачами техники: выдача, массовая выдача,
 * переназначение, возврат и история выдач.
 */
@Controller
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

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

    private String formatDate(Long epochSeconds, DateTimeFormatter formatter) {
        if (epochSeconds == null || epochSeconds == 0) {
            return "—";
        }
        return formatter.format(Instant.ofEpochSecond(epochSeconds));
    }
}
