package com.yuji.profile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.profile.dto.ProfileCreateReq;
import com.yuji.profile.dto.ProfileVO;
import com.yuji.profile.entity.Profile;
import com.yuji.profile.mapper.ProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileMapper profileMapper;
    private final CheckupService checkupService;
    private final ObjectMapper objectMapper;

    /**
     * 建档：换算 lmp/edd，建档成功后自动生成全孕期产检计划。
     */
    @Transactional
    public ProfileVO create(Long userId, ProfileCreateReq req) {
        if (getEntity(userId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已经建过档了");
        }
        if (req.lmpDate() == null && req.edd() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写末次月经日期或预产期");
        }
        LocalDate lmp = req.lmpDate() != null ? req.lmpDate()
                : GestationalCalculator.lmpFromEdd(req.edd());

        Profile p = new Profile();
        p.setUserId(userId);
        p.setNickname(req.nickname());
        p.setLmpDate(lmp);
        p.setEdd(lmp.plusDays(280));
        p.setConcerns(toJson(req.concerns()));
        p.setConsentVersion("v0.1");
        profileMapper.insert(p);

        // 建档即生成全孕期产检计划（模块 B 核心验收点）
        checkupService.generateForUser(userId, lmp);
        return toVO(p);
    }

    /** 未建档抛 NO_PROFILE(40401)，前端据此跳转建档引导 */
    public ProfileVO getMine(Long userId) {
        Profile p = requireProfile(userId);
        return toVO(p);
    }

    public Profile requireProfile(Long userId) {
        Profile p = getEntity(userId);
        if (p == null) {
            throw new BusinessException(ErrorCode.NO_PROFILE);
        }
        return p;
    }

    private Profile getEntity(Long userId) {
        return profileMapper.selectOne(
                new LambdaQueryWrapper<Profile>().eq(Profile::getUserId, userId));
    }

    private ProfileVO toVO(Profile p) {
        GestationalCalculator.Info g = GestationalCalculator.fromLmp(p.getLmpDate());
        return new ProfileVO(p.getId(), p.getNickname(), p.getLmpDate(), p.getEdd(),
                fromJson(p.getConcerns()), g.weeks(), g.days(), g.stage(),
                g.daysToEdd(), g.postpartum());
    }

    @SneakyThrows
    private String toJson(List<String> list) {
        return list == null ? "[]" : objectMapper.writeValueAsString(list);
    }

    @SneakyThrows
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
