package io.allitov.pvz.web;

import io.allitov.pvz.model.Order;
import io.allitov.pvz.model.OrderStatus;
import io.allitov.pvz.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Веб-контроллер пункта выдачи заказов (ПВЗ).
 *
 * <p>Тонкий контроллер: отвечает только за маршрутизацию, валидацию формы
 * приёмки и flash-сообщения; вся работа с данными выполняется через
 * {@link OrderService}, внедряемый через конструктор. Каждый изменяющий запрос
 * выполнен по схеме PRG (Post/Redirect/Get): после обработки происходит
 * перенаправление на список заказов, а текст и цвет сообщения передаются
 * через flash-атрибуты {@code msg} и {@code msgType}
 * ({@code ok} — зелёный, {@code warn} — оранжевый, {@code err} — красный).</p>
 */
@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Отображает список заказов с поиском и фильтром по статусу.
     *
     * <p>Поиск по подстроке выполняется слоем данных; фильтр статуса
     * {@code status} применяется к полученному списку в контроллере:
     * пустое значение означает «все» статусы, иначе остаются заказы,
     * ключ статуса которых совпадает с заданным без учёта регистра.</p>
     *
     * @param filter поисковая подстрока или пустая строка для всех заказов.
     * @param status ключ статуса ({@code ready}, {@code issued}, {@code cancelled}) или пустая строка для всех.
     * @param model модель Spring MVC для шаблона.
     * @return имя шаблона списка заказов.
     */
    @GetMapping("/")
    public String list(
            @RequestParam(name = "filter", defaultValue = "") String filter,
            @RequestParam(name = "status", defaultValue = "") String status,
            Model model
    ) {
        List<Order> orders = orderService.listOrders(filter);
        List<Order> filtered = status.isEmpty()
                ? orders
                : orders.stream()
                        .filter(order -> status.equalsIgnoreCase(order.getStatus()))
                        .toList();
        model.addAttribute("orders", filtered);
        model.addAttribute("filter", filter);
        model.addAttribute("status", status);
        model.addAttribute("count", filtered.size());
        model.addAttribute("closedCount", orderService.countClosedOrders());
        return "orders";
    }

    /**
     * Отображает пустую форму приёмки заказа.
     *
     * @param model модель Spring MVC для шаблона.
     * @return имя шаблона формы приёмки.
     */
    @GetMapping("/orders/new")
    public String showAddForm(Model model) {
        model.addAttribute("contact", "");
        model.addAttribute("cell", "");
        return "order-add";
    }

    /**
     * Принимает заказ из формы приёмки с валидацией.
     *
     * <p>Контакт обязан содержать не менее 5 символов после обрезки пробелов,
     * название ячейки — не более 30 символов. При ошибке форма возвращается
     * с введёнными значениями и текстом ошибки, запись в базу данных не
     * выполняется. При успехе заказ сохраняется, а код заказа передаётся
     * во flash-сообщении, после чего выполняется перенаправление на список.</p>
     *
     * @param contact телефон получателя или артикул товара из формы.
     * @param cell название ячейки хранения из формы.
     * @param model модель Spring MVC для повторного показа формы при ошибке.
     * @param attributes flash-атрибуты для сообщения об успехе.
     * @return имя шаблона формы при ошибке или перенаправление на список.
     */
    @PostMapping("/orders")
    public String add(
            @RequestParam(name = "contact", defaultValue = "") String contact,
            @RequestParam(name = "cell", defaultValue = "") String cell,
            Model model,
            RedirectAttributes attributes
    ) {
        String trimmed = contact.trim();
        if (trimmed.length() < 5) {
            model.addAttribute("contactError", "Введите телефон или артикул посылки");
            model.addAttribute("contact", contact);
            model.addAttribute("cell", cell);
            return "order-add";
        }
        if (cell.length() > 30) {
            model.addAttribute("cellError", "Название ячейки слишком длинное (максимум 30)");
            model.addAttribute("contact", contact);
            model.addAttribute("cell", cell);
            return "order-add";
        }
        String code = orderService.addOrder(trimmed, cell);
        attributes.addFlashAttribute("msg", "Принят заказ " + code);
        attributes.addFlashAttribute("msgType", "ok");
        return "redirect:/";
    }

    /**
     * Выдаёт заказ по коду, выбранному в списке.
     *
     * <p>При пустом коде (строка не выбрана) выводится подсказка. При отказе
     * в выдаче контроллер находит текущий статус заказа и выводит его
     * русскую подпись через {@link OrderStatus#fromDb(String)}; если заказ
     * не найден, выводится только текст отказа.</p>
     *
     * @param code код заказа из выбранной строки списка.
     * @param attributes flash-атрибуты для сообщения о результате.
     * @return перенаправление на список заказов.
     */
    @PostMapping("/orders/{code}/issue")
    public String issue(@PathVariable("code") String code, RedirectAttributes attributes) {
        if (code == null || code.isBlank()) {
            attributes.addFlashAttribute("msg", "Выберите заказ в списке.");
            attributes.addFlashAttribute("msgType", "warn");
            return "redirect:/";
        }
        if (orderService.issueOrder(code)) {
            attributes.addFlashAttribute("msg", "Выдан " + code);
            attributes.addFlashAttribute("msgType", "ok");
            return "redirect:/";
        }
        String displayName = orderService.listOrders("").stream()
                .filter(order -> code.equals(order.getCode()))
                .findFirst()
                .map(order -> OrderStatus.fromDb(order.getStatus()).getDisplayName())
                .orElse(null);
        attributes.addFlashAttribute("msg", displayName == null
                ? "Заказ нельзя выдать"
                : "Заказ нельзя выдать (статус: " + displayName + ")");
        attributes.addFlashAttribute("msgType", "err");
        return "redirect:/";
    }

    /**
     * Отменяет заказ по коду, выбранному в списке.
     *
     * <p>При пустом коде (строка не выбрана) выводится подсказка; отменить
     * можно только заказ в статусе «К выдаче», при отказе выводится
     * соответствующее сообщение.</p>
     *
     * @param code код заказа из выбранной строки списка.
     * @param attributes flash-атрибуты для сообщения о результате.
     * @return перенаправление на список заказов.
     */
    @PostMapping("/orders/{code}/cancel")
    public String cancel(@PathVariable("code") String code, RedirectAttributes attributes) {
        if (code == null || code.isBlank()) {
            attributes.addFlashAttribute("msg", "Выберите заказ в списке.");
            attributes.addFlashAttribute("msgType", "warn");
            return "redirect:/";
        }
        if (orderService.cancelOrder(code)) {
            attributes.addFlashAttribute("msg", "Отменён " + code);
            attributes.addFlashAttribute("msgType", "warn");
        } else {
            attributes.addFlashAttribute("msg", "Отменить можно только заказ «К выдаче»");
            attributes.addFlashAttribute("msgType", "err");
        }
        return "redirect:/";
    }

    /**
     * Отображает сводный отчёт по заказам.
     *
     * <p>Помещает в модель три списка заказов (все принятые, выданные
     * и отменённые) и сводку {@code Report} с количеством заказов
     * по каждому статусу.</p>
     *
     * @param model модель Spring MVC для шаблона отчёта.
     * @return имя шаблона отчёта.
     */
    @GetMapping("/report")
    public String report(Model model) {
        model.addAttribute("readyOrders", orderService.listByStatus(""));
        model.addAttribute("issuedOrders", orderService.listByStatus("issued"));
        model.addAttribute("cancelledOrders", orderService.listByStatus("cancelled"));
        model.addAttribute("report", orderService.buildReport());
        return "report";
    }

    /**
     * Удаляет заказы в закрытых статусах («Выдан» и «Отменён»).
     *
     * <p>Если закрытых заказов нет, выводится предупреждение и удаление
     * не выполняется; иначе выводится количество удалённых заказов.</p>
     *
     * @param attributes flash-атрибуты для сообщения о результате.
     * @return перенаправление на список заказов.
     */
    @PostMapping("/orders/cleanup")
    public String cleanup(RedirectAttributes attributes) {
        int closed = orderService.countClosedOrders();
        if (closed == 0) {
            attributes.addFlashAttribute("msg", "Нет выданных или отменённых заказов.");
            attributes.addFlashAttribute("msgType", "warn");
            return "redirect:/";
        }
        int deleted = orderService.deleteClosedOrders();
        attributes.addFlashAttribute("msg", "Удалено заказов: " + deleted);
        attributes.addFlashAttribute("msgType", "ok");
        return "redirect:/";
    }
}
