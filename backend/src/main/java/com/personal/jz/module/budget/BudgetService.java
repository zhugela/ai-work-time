package com.personal.jz.module.budget;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personal.jz.common.exception.BizException;
import com.personal.jz.common.exception.ErrorCodeEnums;
import com.personal.jz.entity.Budget;
import com.personal.jz.entity.Category;
import com.personal.jz.repository.BudgetRepository;
import com.personal.jz.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepo;
    private final CategoryRepository categoryRepo;
    private static final Pattern MONTH = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    @Transactional
    public Budget upsert(Long bookId, Long categoryId, String yearMonth, BigDecimal amount, BigDecimal warnThreshold) {
        if (yearMonth == null || !MONTH.matcher(yearMonth).matches()) throw new BizException(ErrorCodeEnums.INVALID_MONTH);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new BizException(ErrorCodeEnums.INVALID_AMOUNT);
        Category c = categoryRepo.selectById(categoryId);
        if (c == null) throw new BizException(ErrorCodeEnums.CATEGORY_NOT_FOUND);
        if ("INCOME".equals(c.getType())) throw new BizException(ErrorCodeEnums.BUDGET_ON_INCOME);

        Budget existing = budgetRepo.selectOne(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getBookId, bookId).eq(Budget::getCategoryId, categoryId).eq(Budget::getYearMonth, yearMonth));
        if (existing != null) {
            existing.setAmount(amount);
            existing.setWarnThreshold(warnThreshold == null ? BigDecimal.ONE : warnThreshold);
            budgetRepo.updateById(existing);
            return existing;
        }
        Budget b = new Budget();
        b.setBookId(bookId); b.setCategoryId(categoryId); b.setYearMonth(yearMonth);
        b.setAmount(amount);
        b.setWarnThreshold(warnThreshold == null ? BigDecimal.ONE : warnThreshold);
        budgetRepo.insert(b);
        return b;
    }

    public List<Budget> list(Long bookId, String yearMonth) {
        return budgetRepo.selectList(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getBookId, bookId)
                .eq(yearMonth != null, Budget::getYearMonth, yearMonth));
    }
}
