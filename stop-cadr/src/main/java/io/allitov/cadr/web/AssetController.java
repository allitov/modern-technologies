package io.allitov.cadr.web;

import io.allitov.cadr.domain.Asset;
import io.allitov.cadr.service.AssetRow;
import io.allitov.cadr.service.AssetService;
import io.allitov.cadr.service.EmployeeService;
import io.allitov.cadr.service.PagedAssets;
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
 * Контроллер работы с техникой: список, добавление, копирование,
 * удаление и поиск.
 */
@Controller
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final EmployeeService employeeService;

    private static final int PAGE_SIZE = 20;

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
}
