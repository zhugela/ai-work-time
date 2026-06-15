package com.personal.jz.module.budget;

import com.personal.jz.entity.Book;
import com.personal.jz.entity.Budget;
import com.personal.jz.entity.Category;
import com.personal.jz.module.auth.dto.RegisterRequest;
import com.personal.jz.module.auth.service.AuthService;
import com.personal.jz.module.book.BookService;
import com.personal.jz.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BudgetServiceTest {

    @Autowired private BudgetService budgetService;
    @Autowired private AuthService authService;
    @Autowired private BookService bookService;
    @Autowired private CategoryRepository categoryRepo;

    @Test
    void upsert_and_replace() {
        var reg = authService.register(req("bd01", "bd01@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        var expCat = categoryRepo.selectList(null).stream().filter(c -> "EXPENSE".equals(c.getType())).findFirst().orElseThrow();
        Budget x = budgetService.upsert(b.getId(), expCat.getId(), "2026-06", new BigDecimal("800"), null);
        assertNotNull(x.getId());
        // 覆盖
        Budget y = budgetService.upsert(b.getId(), expCat.getId(), "2026-06", new BigDecimal("1000"), null);
        assertEquals(x.getId(), y.getId());
        assertEquals(0, y.getAmount().compareTo(new BigDecimal("1000")));
    }

    @Test
    void budget_on_income_should_fail() {
        var reg = authService.register(req("bd02", "bd02@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        var incCat = categoryRepo.selectList(null).stream().filter(c -> "INCOME".equals(c.getType())).findFirst().orElseThrow();
        assertThrows(RuntimeException.class,
                () -> budgetService.upsert(b.getId(), incCat.getId(), "2026-06", new BigDecimal("1000"), null));
    }

    @Test
    void invalid_month_format() {
        var reg = authService.register(req("bd03", "bd03@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        var expCat = categoryRepo.selectList(null).stream().filter(c -> "EXPENSE".equals(c.getType())).findFirst().orElseThrow();
        assertThrows(RuntimeException.class,
                () -> budgetService.upsert(b.getId(), expCat.getId(), "2026-13", new BigDecimal("100"), null));
    }

    private RegisterRequest req(String u, String e) {
        RegisterRequest r = new RegisterRequest(); r.setUsername(u); r.setEmail(e); r.setPassword("Test12345");
        return r;
    }
}
