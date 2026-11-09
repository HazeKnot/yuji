package com.yuji.profile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.profile.dto.CheckupVO;
import com.yuji.profile.entity.CheckupItem;
import com.yuji.profile.entity.CheckupTemplate;
import com.yuji.profile.mapper.CheckupItemMapper;
import com.yuji.profile.mapper.CheckupTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckupService {

    private final CheckupItemMapper itemMapper;
    private final CheckupTemplateMapper templateMapper;

    /**
     * 按模板生成全孕期产检计划：dueDate = lmp + suggestedWeek * 7。
     */
    @Transactional
    public void generateForUser(Long userId, LocalDate lmp) {
        List<CheckupTemplate> templates = templateMapper.selectList(null);
        for (CheckupTemplate t : templates) {
            CheckupItem item = new CheckupItem();
            item.setUserId(userId);
            item.setCode(t.getCode());
            item.setName(t.getName());
            item.setSuggestedWeek(t.getSuggestedWeek());
            item.setDueDate(lmp.plusWeeks(t.getSuggestedWeek()));
            item.setStatus(CheckupVO.PLANNED);
            itemMapper.insert(item);
        }
    }

    public List<CheckupVO> listMine(Long userId) {
        LocalDate today = LocalDate.now();
        return itemMapper.selectList(new LambdaQueryWrapper<CheckupItem>()
                        .eq(CheckupItem::getUserId, userId))
                .stream()
                .map(i -> toVO(i, today))
                .sorted(Comparator.comparing(CheckupVO::suggestedWeek))
                .toList();
    }

    public CheckupVO updateStatus(Long userId, Long id, String status) {
        // 注意：List.of(...).contains(null) 会抛 NPE，必须先判空再判断合法值
        if (status == null
                || !List.of(CheckupVO.PLANNED, CheckupVO.DONE, CheckupVO.SKIPPED).contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的产检状态");
        }
        CheckupItem item = itemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产检项不存在");
        }
        item.setStatus(status);
        itemMapper.updateById(item);
        return toVO(item, LocalDate.now());
    }

    private CheckupVO toVO(CheckupItem i, LocalDate today) {
        String window = templateWindow(i.getCode());
        boolean overdue = CheckupVO.PLANNED.equals(i.getStatus())
                && i.getDueDate() != null && i.getDueDate().isBefore(today);
        return new CheckupVO(i.getId(), i.getCode(), i.getName(), i.getSuggestedWeek(),
                window, i.getDueDate(), i.getStatus(), overdue, null);
    }

    private String templateWindow(String code) {
        CheckupTemplate t = templateMapper.selectOne(
                new LambdaQueryWrapper<CheckupTemplate>().eq(CheckupTemplate::getCode, code));
        return t == null ? null : t.getWindowText();
    }
}
