package ra.demo.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ra.demo.exception.BusinessException;
import ra.demo.model.dto.request.CategoryRequest;
import ra.demo.service.CategoryService;
import java.util.Map;
@Controller @RequiredArgsConstructor @RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categories;
    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categories.list());
        return "categories/list";
    }
    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("categoryForm", new CategoryRequest());
        return form(null, model);
    }
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("categoryForm", categories.getForm(id));
        return form(id, model);
    }
    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryRequest request, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        return save(null, request, result, model, redirect);
    }
    @PostMapping("/{id}")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("categoryForm") CategoryRequest request,
                       BindingResult result, Model model, RedirectAttributes redirect) {
        categories.getForm(id);
        return save(id, request, result, model, redirect);
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            categories.delete(id);
            redirect.addFlashAttribute("successKey", "flash.category.deleted");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("errorKey", ex.getMessageKey());
        }
        return "redirect:/categories";
    }
    private String save(Long id, CategoryRequest request, BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                categories.save(id, request);
                redirect.addFlashAttribute("successKey", id == null ? "flash.category.created" : "flash.category.updated");
                return "redirect:/categories";
            } catch (BusinessException ex) {
                result.rejectValue(ex.getField(), ex.getMessageKey());
            }
        }
        return form(id, model);
    }
    private String form(Long id, Model model) {
        String path = id == null ? "/categories/new" : "/categories/" + id + "/edit";
        model.addAttribute("languageLinks", Map.of("en", path + "?lang=en", "vi", path + "?lang=vi"));
        model.addAttribute("categoryId", id);
        return "categories/form";
    }
}
