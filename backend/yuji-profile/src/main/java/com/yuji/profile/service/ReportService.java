package com.yuji.profile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.profile.dto.ReportVO;
import com.yuji.profile.entity.CheckupItem;
import com.yuji.profile.entity.Report;
import com.yuji.profile.mapper.CheckupItemMapper;
import com.yuji.profile.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 检查报告：上传校验 → 对象存储 → 元数据落库。
 *
 * <p>安全约束：类型白名单（只认 contentType，不信任扩展名）、10MB 上限、文件名清洗、
 * 对象键由服务端生成（用户无法指定路径）、下载与删除都做归属校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    /** 单文件上限 10MB，与 application.yml 的 multipart 限制保持一致 */
    public static final long MAX_SIZE = 10L * 1024 * 1024;

    /** 允许的类型 → 落盘扩展名（扩展名由类型推导，避免用户上传 xxx.jsp 之类） */
    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "application/pdf", "pdf");

    private final ReportMapper reportMapper;
    private final CheckupItemMapper checkupItemMapper;
    private final FileStorage storage;

    public ReportVO upload(Long userId, Long checkupItemId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能超过 10MB");
        }
        String contentType = file.getContentType();
        String ext = contentType == null ? null : ALLOWED.get(contentType.toLowerCase());
        if (ext == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 JPG / PNG / WEBP / PDF 格式");
        }
        if (checkupItemId != null) {
            requireOwnedItem(userId, checkupItemId);
        }

        String objectKey = "reports/" + userId + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        long size = file.getSize();
        try (InputStream in = file.getInputStream()) {
            storage.store(objectKey, in, size, contentType);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件读取失败，请稍后重试");
        }

        Report r = new Report();
        r.setUserId(userId);
        r.setCheckupItemId(checkupItemId);
        r.setFileName(sanitizeFileName(file.getOriginalFilename(), ext));
        r.setObjectKey(objectKey);
        r.setContentType(contentType);
        r.setSizeBytes(size);
        r.setStorage(storage.type());
        try {
            reportMapper.insert(r);
        } catch (RuntimeException e) {
            // 元数据写失败就回滚文件，避免存储里堆积"孤儿文件"
            safeDelete(objectKey);
            throw e;
        }
        return toVO(r);
    }

    /** checkupItemId 为空表示查全部 */
    public List<ReportVO> list(Long userId, Long checkupItemId) {
        LambdaQueryWrapper<Report> w = new LambdaQueryWrapper<Report>()
                .eq(Report::getUserId, userId)
                .orderByDesc(Report::getId);
        if (checkupItemId != null) {
            w.eq(Report::getCheckupItemId, checkupItemId);
        }
        return reportMapper.selectList(w).stream().map(this::toVO).toList();
    }

    /** 下载：返回元数据 + 输入流，由 controller 负责 Content-Disposition */
    public ReportFile open(Long userId, Long id) {
        Report r = requireOwned(userId, id);
        return new ReportFile(toVO(r), storage.open(r.getObjectKey()));
    }

    public void delete(Long userId, Long id) {
        Report r = requireOwned(userId, id);
        // 先删库再删文件：文件删除失败不影响用户视角（记录已不可见），只留日志待运维清理
        reportMapper.deleteById(id);
        safeDelete(r.getObjectKey());
    }

    private Report requireOwned(Long userId, Long id) {
        Report r = reportMapper.selectById(id);
        if (r == null || !r.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报告不存在");
        }
        return r;
    }

    private void requireOwnedItem(Long userId, Long checkupItemId) {
        CheckupItem item = checkupItemMapper.selectById(checkupItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "产检项不存在");
        }
    }

    private void safeDelete(String objectKey) {
        try {
            storage.delete(objectKey);
        } catch (RuntimeException e) {
            log.warn("删除存储对象失败，需人工清理: {}", objectKey, e);
        }
    }

    private ReportVO toVO(Report r) {
        long size = r.getSizeBytes() == null ? 0L : r.getSizeBytes();
        return new ReportVO(r.getId(), r.getCheckupItemId(), r.getFileName(), r.getContentType(),
                size, "/api/reports/" + r.getId() + "/download", r.getCreatedAt());
    }

    /**
     * 文件名只用于展示：去掉路径分隔符与首尾空白，超长截断，没有名字时用 report.{ext}。
     */
    static String sanitizeFileName(String original, String ext) {
        if (original == null || original.isBlank()) {
            return "report." + ext;
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        if (name.isEmpty()) {
            return "report." + ext;
        }
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }

    /** 下载所需的元数据 + 流 */
    public record ReportFile(ReportVO meta, InputStream stream) {
    }
}
