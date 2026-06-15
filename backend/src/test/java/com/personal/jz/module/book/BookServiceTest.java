package com.personal.jz.module.book;

import com.personal.jz.entity.Book;
import com.personal.jz.module.auth.dto.RegisterRequest;
import com.personal.jz.module.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookServiceTest {

    @Autowired private BookService bookService;
    @Autowired private AuthService authService;

    @Test
    void create_book_and_list() {
        var reg = authService.register(req("bk01", "bk01@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.create(uid, make("旅行账本", "USD"));
        assertNotNull(b.getId());
        assertEquals(2, bookService.list(uid, false).size()); // 1 default + new
    }

    @Test
    void duplicate_book_name_should_fail() {
        var reg = authService.register(req("bk02", "bk02@x.dev"));
        Long uid = reg.getUser().getId();
        bookService.create(uid, make("家庭", "CNY"));
        assertThrows(RuntimeException.class, () -> bookService.create(uid, make("家庭", "CNY")));
    }

    @Test
    void archive_last_active_should_fail() {
        var reg = authService.register(req("bk03", "bk03@x.dev"));
        Long uid = reg.getUser().getId();
        // 默认有 1 个 active；归档应失败
        Book def = bookService.list(uid, false).get(0);
        assertThrows(RuntimeException.class, () -> bookService.updateStatus(uid, def.getId(), "ARCHIVED"));
    }

    @Test
    void archive_then_unarchive() {
        var reg = authService.register(req("bk04", "bk04@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.create(uid, make("副业", "CNY"));
        bookService.updateStatus(uid, b.getId(), "ARCHIVED");
        assertEquals("ARCHIVED", bookService.get(uid, b.getId()).getStatus());
        bookService.updateStatus(uid, b.getId(), "ACTIVE");
        assertEquals("ACTIVE", bookService.get(uid, b.getId()).getStatus());
    }

    private Book make(String name, String cur) {
        Book b = new Book(); b.setName(name); b.setCurrency(cur); b.setIcon("📒");
        return b;
    }
    private RegisterRequest req(String u, String e) {
        RegisterRequest r = new RegisterRequest(); r.setUsername(u); r.setEmail(e); r.setPassword("Test12345");
        return r;
    }
}
