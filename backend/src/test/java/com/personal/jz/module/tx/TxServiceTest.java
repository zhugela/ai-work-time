package com.personal.jz.module.tx;

import com.personal.jz.entity.Book;
import com.personal.jz.entity.Category;
import com.personal.jz.entity.TxRecord;
import com.personal.jz.module.auth.dto.RegisterRequest;
import com.personal.jz.module.auth.service.AuthService;
import com.personal.jz.module.book.BookService;
import com.personal.jz.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TxServiceTest {

    @Autowired private TxService txService;
    @Autowired private AuthService authService;
    @Autowired private BookService bookService;
    @Autowired private CategoryRepository categoryRepo;

    @Test
    void create_tx_and_list() {
        var reg = authService.register(req("tx01", "tx01@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        // 从预置系统分类里选一个
        var cats = categoryRepo.selectList(null);
        Category expCat = cats.stream().filter(c -> "EXPENSE".equals(c.getType())).findFirst().orElseThrow();
        TxRecord in = new TxRecord();
        in.setBookId(b.getId());
        in.setCategoryId(expCat.getId());
        in.setType("EXPENSE");
        in.setAmount(new BigDecimal("38.00"));
        in.setPayMethod("ALIPAY");
        in.setOccurredAt(LocalDateTime.now());
        in.setRemark("午餐");
        TxRecord saved = txService.create(uid, in);
        assertNotNull(saved.getId());

        var page = txService.list(uid, b.getId(), null, null, null, null, null, 1, 20);
        assertEquals(1, page.getTotal());
    }

    @Test
    void create_tx_negative_amount_should_fail() {
        var reg = authService.register(req("tx02", "tx02@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        var cats = categoryRepo.selectList(null);
        Category expCat = cats.stream().filter(c -> "EXPENSE".equals(c.getType())).findFirst().orElseThrow();
        TxRecord in = new TxRecord();
        in.setBookId(b.getId());
        in.setCategoryId(expCat.getId());
        in.setType("EXPENSE");
        in.setAmount(new BigDecimal("-1"));
        assertThrows(RuntimeException.class, () -> txService.create(uid, in));
    }

    @Test
    void create_tx_category_type_mismatch_should_fail() {
        var reg = authService.register(req("tx03", "tx03@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        var cats = categoryRepo.selectList(null);
        Category expCat = cats.stream().filter(c -> "EXPENSE".equals(c.getType())).findFirst().orElseThrow();
        TxRecord in = new TxRecord();
        in.setBookId(b.getId());
        in.setCategoryId(expCat.getId());
        in.setType("INCOME");  // 类型不一致
        in.setAmount(new BigDecimal("100"));
        assertThrows(RuntimeException.class, () -> txService.create(uid, in));
    }

    @Test
    void soft_delete_tx() {
        var reg = authService.register(req("tx04", "tx04@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        var cats = categoryRepo.selectList(null);
        Category expCat = cats.stream().filter(c -> "EXPENSE".equals(c.getType())).findFirst().orElseThrow();
        TxRecord in = new TxRecord();
        in.setBookId(b.getId()); in.setCategoryId(expCat.getId());
        in.setType("EXPENSE"); in.setAmount(new BigDecimal("10"));
        TxRecord saved = txService.create(uid, in);
        txService.softDelete(uid, saved.getId());
        // 软删除后应过滤
        var page = txService.list(uid, b.getId(), null, null, null, null, null, 1, 20);
        assertEquals(0, page.getTotal());
    }

    private RegisterRequest req(String u, String e) {
        RegisterRequest r = new RegisterRequest(); r.setUsername(u); r.setEmail(e); r.setPassword("Test12345");
        return r;
    }
}
