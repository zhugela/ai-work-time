package com.personal.jz.module.book;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import com.personal.jz.entity.Book;
import com.personal.jz.entity.SysUser;
import com.personal.jz.repository.BookRepository;
import com.personal.jz.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepo;
    private final SysUserRepository userRepo;
    private static final int MAX_BOOKS = 50;

    public List<Book> list(Long uid, boolean includeArchived) {
        var q = new LambdaQueryWrapper<Book>().eq(Book::getUserId, uid);
        if (!includeArchived) q.eq(Book::getStatus, "ACTIVE");
        q.orderByDesc(Book::getCreatedAt);
        return bookRepo.selectList(q);
    }

    public Book get(Long uid, Long id) {
        Book b = bookRepo.selectById(id);
        if (b == null || !b.getUserId().equals(uid)) throw new BizException(ErrorCodeEnums.NOT_FOUND);
        return b;
    }

    @Transactional
    public Book create(Long uid, Book in) {
        long count = bookRepo.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getUserId, uid));
        if (count >= MAX_BOOKS) throw new BizException(ErrorCodeEnums.BOOK_LIMIT_REACHED);
        Long dup = bookRepo.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getUserId, uid).eq(Book::getName, in.getName()));
        if (dup != null && dup > 0) throw new BizException(ErrorCodeEnums.BOOK_NAME_TAKEN);
        Book b = new Book();
        b.setUserId(uid);
        b.setName(in.getName());
        b.setCurrency(in.getCurrency() == null ? "CNY" : in.getCurrency());
        b.setIcon(in.getIcon());
        b.setStatus("ACTIVE");
        bookRepo.insert(b);
        return b;
    }

    @Transactional
    public void switchBook(Long uid, Long bookId) {
        Book b = get(uid, bookId);
        if (!"ACTIVE".equals(b.getStatus())) throw new BizException(ErrorCodeEnums.BOOK_ARCHIVED);
        SysUser u = userRepo.selectById(uid);
        u.setCurrentBookId(bookId);
        userRepo.updateById(u);
    }

    @Transactional
    public Book updateStatus(Long uid, Long bookId, String status) {
        Book b = get(uid, bookId);
        if ("ARCHIVED".equals(status)) {
            long active = bookRepo.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getUserId, uid).eq(Book::getStatus, "ACTIVE"));
            if (active <= 1) throw new BizException(ErrorCodeEnums.LAST_ACTIVE_BOOK);
        }
        b.setStatus(status);
        bookRepo.updateById(b);
        return b;
    }
}
