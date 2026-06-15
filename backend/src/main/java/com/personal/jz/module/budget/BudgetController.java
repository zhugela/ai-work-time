package com.personal.jz.module.budget;

import com.personal.jz.common.api.ApiResponse;
import com.personal.jz.common.security.CurrentUser;
import com.personal.jz.entity.Budget;
import com.personal.jz.module.book.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final BookService bookService;

    @PutMapping
    public ApiResponse<Budget> upsert(@CurrentUser Long uid, @RequestBody Map<String, Object> body) {
        Long bookId = ((Number) body.get("bookId")).longValue();
        Long categoryId = ((Number) body.get("categoryId")).longValue();
        String ym = (String) body.get("yearMonth");
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        BigDecimal warn = body.get("warnThreshold") == null ? null : new BigDecimal(String.valueOf(body.get("warnThreshold")));
        bookService.get(uid, bookId);
        return ApiResponse.ok(budgetService.upsert(bookId, categoryId, ym, amount, warn));
    }

    @GetMapping
    public ApiResponse<List<Budget>> list(@CurrentUser Long uid, @RequestParam Long bookId, @RequestParam(required = false) String yearMonth) {
        bookService.get(uid, bookId);
        return ApiResponse.ok(budgetService.list(bookId, yearMonth));
    }
}
