package com.yuji.wellness.partner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.wellness.partner.dto.PartnerDtos;
import com.yuji.wellness.partner.entity.PartnerLead;
import com.yuji.wellness.partner.entity.PartnerOrg;
import com.yuji.wellness.partner.entity.PartnerServiceItem;
import com.yuji.wellness.partner.mapper.PartnerLeadMapper;
import com.yuji.wellness.partner.mapper.PartnerOrgMapper;
import com.yuji.wellness.partner.mapper.PartnerServiceItemMapper;
import com.yuji.wellness.support.PartnerType;
import com.yuji.wellness.support.TimeText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 合作机构：妇幼保健院、月子中心、产后康复与社区机构。
 *
 * <p>平台只做"信息展示 + 预约线索"，不参与诊疗、不做医疗效果背书、不按诊疗收入分成。
 */
@Service
@RequiredArgsConstructor
public class PartnerService {

    private static final List<String> ORG_STATUS = List.of("ACTIVE", "PAUSED", "PENDING");
    private static final List<String> LEAD_STATUS = List.of("NEW", "CONTACTED", "BOOKED", "CLOSED");

    private final PartnerOrgMapper orgMapper;
    private final PartnerServiceItemMapper serviceMapper;
    private final PartnerLeadMapper leadMapper;

    public List<PartnerDtos.OrgVO> list(String city, String type) {
        LambdaQueryWrapper<PartnerOrg> wrapper = new LambdaQueryWrapper<PartnerOrg>()
                .eq(PartnerOrg::getStatus, "ACTIVE")
                .isNull(PartnerOrg::getDeletedAt)
                .orderByAsc(PartnerOrg::getId);
        if (city != null && !city.isBlank()) {
            wrapper.eq(PartnerOrg::getCity, city.trim());
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(PartnerOrg::getType, PartnerType.of(type).name());
        }
        return orgMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public PartnerDtos.OrgDetailVO detail(Long id) {
        PartnerOrg org = requireOrg(id);
        return new PartnerDtos.OrgDetailVO(toVO(org), servicesOf(id));
    }

    public List<PartnerDtos.OrgBriefVO> activeBrief(int limit) {
        return orgMapper.selectList(new LambdaQueryWrapper<PartnerOrg>()
                        .eq(PartnerOrg::getStatus, "ACTIVE")
                        .isNull(PartnerOrg::getDeletedAt)
                        .orderByAsc(PartnerOrg::getId)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 10))))
                .stream()
                .map(o -> new PartnerDtos.OrgBriefVO(o.getId(), o.getName(), o.getType(),
                        PartnerType.labelOf(o.getType()), o.getCity(), o.getDistrict(), o.getAddress()))
                .toList();
    }

    @Transactional
    public PartnerDtos.LeadVO submitLead(Long userId, Long orgId, PartnerDtos.LeadReq req) {
        PartnerOrg org = requireOrg(orgId);
        if (!"ACTIVE".equals(org.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该机构暂不接收预约");
        }
        if (req.preferredDate() != null && req.preferredDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "期望日期不能早于今天");
        }
        PartnerLead lead = new PartnerLead();
        lead.setOrgId(orgId);
        lead.setUserId(userId);
        lead.setName(req.name().trim());
        lead.setPhone(req.phone().trim());
        lead.setNeed(req.need().trim());
        lead.setPreferredDate(req.preferredDate());
        lead.setStatus("NEW");
        leadMapper.insert(lead);
        return toLeadVO(lead, org.getName());
    }

    // ---------- 机构端 ----------

    public List<PartnerDtos.OrgVO> adminList() {
        return orgMapper.selectList(new LambdaQueryWrapper<PartnerOrg>()
                        .isNull(PartnerOrg::getDeletedAt)
                        .orderByDesc(PartnerOrg::getId))
                .stream().map(this::toVO).toList();
    }

    public PartnerDtos.OrgDetailVO adminDetail(Long id) {
        PartnerOrg org = requireOrg(id);
        return new PartnerDtos.OrgDetailVO(toVO(org), servicesOf(id));
    }

    @Transactional
    public PartnerDtos.OrgDetailVO saveOrg(Long id, PartnerDtos.OrgReq req) {
        String type = PartnerType.of(req.type()).name();
        String status = req.status() == null || req.status().isBlank()
                ? "ACTIVE" : req.status().trim().toUpperCase();
        if (!ORG_STATUS.contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的机构状态");
        }

        PartnerOrg org;
        if (id == null) {
            org = new PartnerOrg();
            org.setName(req.name().trim());
        } else {
            org = requireOrg(id);
            org.setName(req.name().trim());
        }
        org.setType(type);
        org.setCity(req.city().trim());
        org.setDistrict(req.district());
        org.setAddress(req.address());
        org.setContactName(req.contactName());
        org.setContactPhone(req.contactPhone());
        org.setIntro(req.intro());
        org.setStatus(status);
        if (id == null) {
            orgMapper.insert(org);
        } else {
            orgMapper.updateById(org);
        }

        if (req.services() != null) {
            serviceMapper.delete(new LambdaQueryWrapper<PartnerServiceItem>()
                    .eq(PartnerServiceItem::getOrgId, org.getId()));
            int sort = 1;
            for (PartnerDtos.ServiceReq s : req.services()) {
                PartnerServiceItem service = new PartnerServiceItem();
                service.setOrgId(org.getId());
                service.setName(s.name().trim());
                service.setPriceText(s.priceText());
                service.setDescription(s.description());
                service.setSortNo(sort++);
                serviceMapper.insert(service);
            }
        }
        return adminDetail(org.getId());
    }

    @Transactional
    public void archiveOrg(Long id) {
        PartnerOrg org = requireOrg(id);
        org.setStatus("PAUSED");
        orgMapper.updateById(org);
    }

    public List<PartnerDtos.LeadVO> adminLeads(Long orgId, String status) {
        LambdaQueryWrapper<PartnerLead> wrapper = new LambdaQueryWrapper<PartnerLead>()
                .orderByDesc(PartnerLead::getCreatedAt);
        if (orgId != null) {
            wrapper.eq(PartnerLead::getOrgId, orgId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(PartnerLead::getStatus, status.trim().toUpperCase());
        }
        List<PartnerLead> leads = leadMapper.selectList(wrapper);
        java.util.Map<Long, String> orgNames = new java.util.HashMap<>();
        for (PartnerOrg org : orgMapper.selectList(null)) {
            orgNames.put(org.getId(), org.getName());
        }
        return leads.stream()
                .map(l -> toLeadVO(l, orgNames.getOrDefault(l.getOrgId(), "")))
                .toList();
    }

    @Transactional
    public PartnerDtos.LeadVO updateLead(Long id, PartnerDtos.LeadStatusReq req) {
        String status = req.status().trim().toUpperCase();
        if (!LEAD_STATUS.contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的线索状态");
        }
        PartnerLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "线索不存在");
        }
        lead.setStatus(status);
        if (req.note() != null) {
            lead.setNote(req.note());
        }
        leadMapper.updateById(lead);
        PartnerOrg org = orgMapper.selectById(lead.getOrgId());
        return toLeadVO(lead, org == null ? "" : org.getName());
    }

    public long countActiveOrgs() {
        return orgMapper.selectCount(new LambdaQueryWrapper<PartnerOrg>()
                .eq(PartnerOrg::getStatus, "ACTIVE")
                .isNull(PartnerOrg::getDeletedAt));
    }

    // ---------- 内部 ----------

    private PartnerOrg requireOrg(Long id) {
        PartnerOrg org = id == null ? null : orgMapper.selectById(id);
        if (org == null || org.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构不存在");
        }
        return org;
    }

    private List<PartnerDtos.ServiceVO> servicesOf(Long orgId) {
        return serviceMapper.selectList(new LambdaQueryWrapper<PartnerServiceItem>()
                        .eq(PartnerServiceItem::getOrgId, orgId)
                        .orderByAsc(PartnerServiceItem::getSortNo))
                .stream()
                .map(s -> new PartnerDtos.ServiceVO(s.getId(), s.getName(), s.getPriceText(), s.getDescription()))
                .toList();
    }

    private PartnerDtos.OrgVO toVO(PartnerOrg org) {
        long services = serviceMapper.selectCount(new LambdaQueryWrapper<PartnerServiceItem>()
                .eq(PartnerServiceItem::getOrgId, org.getId()));
        long leads = leadMapper.selectCount(new LambdaQueryWrapper<PartnerLead>()
                .eq(PartnerLead::getOrgId, org.getId()));
        return new PartnerDtos.OrgVO(org.getId(), org.getName(), org.getType(),
                PartnerType.labelOf(org.getType()), org.getCity(), org.getDistrict(), org.getAddress(),
                org.getIntro(), org.getStatus(), (int) services, leads);
    }

    private PartnerDtos.LeadVO toLeadVO(PartnerLead lead, String orgName) {
        return new PartnerDtos.LeadVO(lead.getId(), lead.getOrgId(), orgName, lead.getName(),
                lead.getPhone(), lead.getNeed(), lead.getPreferredDate(), lead.getStatus(),
                leadStatusLabel(lead.getStatus()), lead.getNote(),
                TimeText.of(lead.getCreatedAt()), lead.getCreatedAt());
    }

    static String leadStatusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "NEW" -> "待联系";
            case "CONTACTED" -> "已联系";
            case "BOOKED" -> "已预约";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }
}
