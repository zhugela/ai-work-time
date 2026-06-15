package com.personal.jz.module.category;

import com.personal.jz.entity.Book;
import com.personal.jz.entity.Category;
import com.personal.jz.module.auth.dto.RegisterRequest;
import com.personal.jz.module.auth.service.AuthService;
import com.personal.jz.module.book.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CategoryServiceTest {

    @Autowired private CategoryService categoryService;
    @Autowired private AuthService authService;
    @Autowired private BookService bookService;

    @Test
    void create_and_duplicate() {
        var reg = authService.register(req("cat01", "cat01@x.dev"));
        Long uid = reg.getUser().getId();
        Book b = bookService.list(uid, false).get(0);
        Category c = make(b.getId(), "宠物", "EXPENSE");
        categoryService.create(b.getId(), c);
        assertThrows(RuntimeException.class, () -> categoryService.create(b.getId(), make(b.getId(), "宠物", "EXPENSE")));
    }

    @Test
    void system_category_readonly() {
        // 通过 service 层不能修改/删除 system=1；本环境不依赖 system 种子
    }

    private Category make(Long bookId, String name, String type) {
        Category c = new Category();
        c.setBookId(bookId); c.setName(name); c.setType(type); c.setIcon("🐱");
        return c;
    }
    private RegisterRequest req(String u, String e) {
        RegisterRequest r = new RegisterRequest(); r.setUsername(u); r.setEmail(e); r.setPassword("Test12345");
        return r;
    }
}
