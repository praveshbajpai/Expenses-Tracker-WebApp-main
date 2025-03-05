package com.SpringBootMVC.ExpensesTracker.controller;

import com.SpringBootMVC.ExpensesTracker.DTO.ExpenseDTO;
import com.SpringBootMVC.ExpensesTracker.DTO.FilterDTO;
import com.SpringBootMVC.ExpensesTracker.entity.Category;
import com.SpringBootMVC.ExpensesTracker.entity.Client;
import com.SpringBootMVC.ExpensesTracker.entity.Expense;
import com.SpringBootMVC.ExpensesTracker.service.CategoryService;
import com.SpringBootMVC.ExpensesTracker.service.ExpenseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class MainController {

    private final ExpenseService expenseService;
    private final CategoryService categoryService;

    @Autowired
    public MainController(ExpenseService expenseService, CategoryService categoryService) {
        this.expenseService = expenseService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String landingPage(HttpSession session, Model model) {
        Client client = (Client) session.getAttribute("client");
        if (client == null) {
            return "redirect:/login"; // Redirect to login if no client is in session
        }
        model.addAttribute("sessionClient", client);
        return "landing-page";
    }

    @GetMapping("/showAdd")
    public String addExpense(Model model) {
        model.addAttribute("expense", new ExpenseDTO());
        return "add-expense";
    }

    @PostMapping("/submitAdd")
    public String submitAdd(@ModelAttribute("expense") ExpenseDTO expenseDTO, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        if (client == null) {
            return "redirect:/login";
        }
        System.out.println(client.getId());
        expenseDTO.setClientId(client.getId());
        expenseService.save(expenseDTO);
        return "redirect:/list";
    }

    @GetMapping("/list")
    public String list(Model model, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        if (client == null) {
            return "redirect:/login";
        }

        int clientId = client.getId();

        // Fetch expenses along with their categories to avoid lazy loading issues
        List<Expense> expenseList = expenseService.findAllExpensesByClientId(clientId);

        expenseList.forEach(expense -> {
            // Ensure category is not null before accessing its ID
            if (expense.getCategory() != null) {
                Category category = categoryService.findCategoryById(expense.getCategory().getId());
                if (category != null) {
                    expense.setCategoryName(category.getName());
                } else {
                    expense.setCategoryName("Uncategorized");
                }
            } else {
                expense.setCategoryName("Uncategorized");
            }

            // Convert date and time safely
            try {
                LocalDateTime dateTime = LocalDateTime.parse(expense.getDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                expense.setDate(dateTime.toLocalDate().toString());
                expense.setTime(dateTime.toLocalTime().toString());
            } catch (Exception e) {
                expense.setDate("Invalid Date");
                expense.setTime("Invalid Time");
            }
        });

        model.addAttribute("expenseList", expenseList);
        model.addAttribute("filter", new FilterDTO());

        return "list-page";
    }

    @GetMapping("/showUpdate")
    public String showUpdate(@RequestParam("expId") int id, Model model) {
        Expense expense = expenseService.findExpenseById(id);
        if (expense == null) {
            return "redirect:/list"; // Redirect if the expense does not exist
        }

        ExpenseDTO expenseDTO = new ExpenseDTO();
        expenseDTO.setAmount(expense.getAmount());
        expenseDTO.setCategory(expense.getCategory().getName());
        expenseDTO.setDescription(expense.getDescription());
        expenseDTO.setDateTime(expense.getDateTime());

        model.addAttribute("expense", expenseDTO);
        model.addAttribute("expenseId", id);
        return "update-page";
    }

    @PostMapping("/submitUpdate")
    public String update(@RequestParam("expId") int id, @ModelAttribute("expense") ExpenseDTO expenseDTO, HttpSession session) {
        Client client = (Client) session.getAttribute("client");
        if (client == null) {
            return "redirect:/login";
        }

        expenseDTO.setExpenseId(id);
        expenseDTO.setClientId(client.getId());
        expenseService.update(expenseDTO);
        return "redirect:/list";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam("expId") int id) {
        expenseService.deleteExpenseById(id);
        return "redirect:/list";
    }

    @PostMapping("/processFilter")
    public String processFilter(@ModelAttribute("filter") FilterDTO filter, Model model) {
        List<Expense> expenseList = expenseService.findFilterResult(filter);

        expenseList.forEach(expense -> {
            expense.setCategoryName(categoryService.findCategoryById(expense.getCategory().getId()).getName());
            expense.setDate(LocalDateTime.parse(expense.getDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate().toString());
            expense.setTime(LocalDateTime.parse(expense.getDateTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalTime().toString());
        });

        model.addAttribute("expenseList", expenseList);
        return "filter-result";
    }
}
