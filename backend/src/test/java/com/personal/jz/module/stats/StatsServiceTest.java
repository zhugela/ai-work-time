package com.personal.jz.module.stats;

import com.personal.jz.entity.Book;
import com.personal.jz.entity.Category;
import com.personal.jz.entity.TxRecord;
import com.personal.jz.module.auth.dto.RegisterRequest;
import com.personal.jz.module.auth.service.AuthService;
import com.personal.jz.module.book.BookService;
import com.personal.jz.module.tx.TxService;
import com.personal.jz.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StatsServiceTest {

    @Autowired private StatsService statsService;
    @Autowired private AuthService authService;
    @Autowired private BookService bookService;
    @Autowired private TxService txService;
    @Autowired private CategoryRepository categoryRepo;

    @Test
    void monthly_summary() {
        var reg = authService.register(req("st01", "st01@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        Category inc = categoryRepo.selectList(null).stream().filter(c -> "INCOME".equals(c.getType())).findFirst().orElseThrow();
        Category exp = categoryRepo.selectList(null).stream().filter(c -> "EXPENSE".equals(c.getType())).findFirst().orElseThrow();
        // 录 2 笔
        TxRecord in = new TxRecord();
        in.setBookId(b.getId()); in.setCategoryId(inc.getId()); in.setType("INCOME");
        in.setAmount(new BigDecimal("1000")); in.setOccurredAt(LocalDateTime.now());
        txService.create(uid, in);
        TxRecord ex = new TxRecord();
        ex.setBookId(b.getId()); ex.setCategoryId(exp.getId()); ex.setType("EXPENSE");
        ex.setAmount(new BigDecimal("300")); ex.setOccurredAt(LocalDateTime.now());
        txService.create(uid, ex);

        var m = statsService.monthly(b.getId(), java.time.LocalDate.now().toString().substring(0, 7));
        assertEquals(0, new BigDecimal("1000").compareTo((BigDecimal) m.get("income")));
        assertEquals(0, new BigDecimal("300").compareTo((BigDecimal) m.get("expense")));
    }

    @Test
    void breakdown() {
        var reg = authService.register(req("st02", "st02@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        var list = statsService.breakdown(b.getId(), java.time.LocalDate.now().toString().substring(0, 7), "EXPENSE");
        assertNotNull(list);
    }

    @Test
    void trend_6_months() {
        var reg = authService.register(req("st03", "st03@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        var t = statsService.trend(b.getId(), 6);
        assertEquals(6, t.size());
    }

    private RegisterRequest req(String u, String e) {
        RegisterRequest r = new RegisterRequest(); r.setUsername(u); r.setEmail(e); r.setPassword("Test12345");
        return r;
    }
}
