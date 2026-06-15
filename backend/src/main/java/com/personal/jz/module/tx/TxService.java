package com.personal.jz.module.tx;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personal.jz.common.api.PageResponse;
import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import com.personal.jz.entity.Book;
import com.personal.jz.entity.Category;
import com.personal.jz.entity.TxRecord;
import com.personal.jz.repository.BookRepository;
import com.personal.jz.repository.CategoryRepository;
import com.personal.jz.repository.TxRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TxService {

    private final TxRecordRepository txRepo;
    private final BookRepository bookRepo;
    private final CategoryRepository categoryRepo;

    @Transactional
    public TxRecord create(Long uid, TxRecord in) {
        // 账本归属
        Book b = bookRepo.selectById(in.getBookId());
        if (b == null || !b.getUserId().equals(uid)) throw new BizException(ErrorCodeEnums.NOT_FOUND);
        if (!"ACTIVE".equals(b.getStatus())) throw new BizException(ErrorCodeEnums.BOOK_ARCHIVED);

        // 金额
        if (in.getAmount() == null || in.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || in.getAmount().compareTo(new BigDecimal("9999999.99")) > 0) {
            throw new BizException(ErrorCodeEnums.INVALID_AMOUNT);
        }

        // 日期 ≤ today+7
        if (in.getOccurredAt() != null) {
            LocalDate maxDate = LocalDate.now(ZoneId.systemDefault()).plusDays(7);
            if (in.getOccurredAt().toLocalDate().isAfter(maxDate)) {
                throw new BizException(ErrorCodeEnums.DATE_OUT_OF_RANGE);
            }
        } else {
            in.setOccurredAt(LocalDateTime.now(ZoneId.systemDefault()));
        }

        // 分类校验
        Category c = categoryRepo.selectById(in.getCategoryId());
        if (c == null) throw new BizException(ErrorCodeEnums.CATEGORY_NOT_FOUND);
        if (!c.getType().equals(in.getType())) throw new BizException(ErrorCodeEnums.CATEGORY_TYPE_MISMATCH);

        in.setUserId(uid);
        if (in.getPayMethod() == null) in.setPayMethod("CASH");
        txRepo.insert(in);
        return in;
    }

    public PageResponse<TxRecord> list(Long uid, Long bookId, LocalDate from, LocalDate to,
                                       Long categoryId, String type, String q, int page, int size) {
        Page<TxRecord> p = new Page<>(page, size);
        var wrapper = new LambdaQueryWrapper<TxRecord>()
                .eq(TxRecord::getUserId, uid)
                .eq(TxRecord::getBookId, bookId)
                .eq(categoryId != null, TxRecord::getCategoryId, categoryId)
                .eq(type != null && !type.isEmpty(), TxRecord::getType, type)
                .ge(from != null, TxRecord::getOccurredAt, from == null ? null : from.atStartOfDay())
                .le(to != null, TxRecord::getOccurredAt, to == null ? null : to.atTime(23, 59, 59))
                .like(q != null && !q.isEmpty(), TxRecord::getRemark, q)
                .orderByDesc(TxRecord::getOccurredAt);
        Page<TxRecord> res = txRepo.selectPage(p, wrapper);
        return PageResponse.of(res.getRecords(), page, size, res.getTotal());
    }

    @Transactional
    public TxRecord update(Long uid, Long id, TxRecord in) {
        TxRecord t = txRepo.selectById(id);
        if (t == null || !t.getUserId().equals(uid)) throw new BizException(ErrorCodeEnums.TX_NOT_FOUND);
        Book b = bookRepo.selectById(t.getBookId());
        if (b == null || !b.getUserId().equals(uid)) throw new BizException(ErrorCodeEnums.NOT_FOUND);
        if (!"ACTIVE".equals(b.getStatus())) throw new BizException(ErrorCodeEnums.BOOK_ARCHIVED);
        if (in.getCategoryId() != null) {
            Category c = categoryRepo.selectById(in.getCategoryId());
            if (c == null) throw new BizException(ErrorCodeEnums.CATEGORY_NOT_FOUND);
            if (!c.getType().equals(t.getType())) throw new BizException(ErrorCodeEnums.CATEGORY_TYPE_MISMATCH);
            t.setCategoryId(in.getCategoryId());
        }
        if (in.getAmount() != null) {
            if (in.getAmount().compareTo(BigDecimal.ZERO) <= 0
                    || in.getAmount().compareTo(new BigDecimal("9999999.99")) > 0)
                throw new BizException(ErrorCodeEnums.INVALID_AMOUNT);
            t.setAmount(in.getAmount());
        }
        if (in.getPayMethod() != null) t.setPayMethod(in.getPayMethod());
        if (in.getOccurredAt() != null) t.setOccurredAt(in.getOccurredAt());
        if (in.getRemark() != null) t.setRemark(in.getRemark());
        txRepo.updateById(t);
        return t;
    }

    @Transactional
    public void softDelete(Long uid, Long id) {
        TxRecord t = txRepo.selectById(id);
        if (t == null || !t.getUserId().equals(uid)) throw new BizException(ErrorCodeEnums.TX_NOT_FOUND);
        t.setIsDeleted(1);
        txRepo.updateById(t);
    }

    public TxRecord get(Long uid, Long id) {
        TxRecord t = txRepo.selectById(id);
        if (t == null || !t.getUserId().equals(uid)) throw new BizException(ErrorCodeEnums.TX_NOT_FOUND);
        return t;
    }
}
