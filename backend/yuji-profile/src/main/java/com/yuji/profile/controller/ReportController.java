package com.yuji.profile.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.profile.dto.ReportVO;
import com.yuji.profile.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 上传报告并关联产检项：multipart/form-data，字段名 file */
    @PostMapping("/checkups/{checkupId}/reports")
    public Result<ReportVO> upload(@AuthenticationPrincipal LoginUser me,
                                  @PathVariable("checkupId") Long checkupId,
                                  @RequestParam("file") MultipartFile file) {
        return Result.ok(reportService.upload(me.userId(), checkupId, file));
    }

    @GetMapping("/checkups/{checkupId}/reports")
    public Result<List<ReportVO>> listByCheckup(@AuthenticationPrincipal LoginUser me,
                                                @PathVariable("checkupId") Long checkupId) {
        return Result.ok(reportService.list(me.userId(), checkupId));
    }

    /** 我的全部报告 */
    @GetMapping("/reports")
    public Result<List<ReportVO>> listMine(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(reportService.list(me.userId(), null));
    }

    /**
     * 下载/预览：必须带 token（图片与 PDF 都不会以静态地址裸奔）。
     */
    @GetMapping("/reports/{id}/download")
    public ResponseEntity<InputStreamResource> download(@AuthenticationPrincipal LoginUser me,
                                                        @PathVariable("id") Long id) {
        ReportService.ReportFile file = reportService.open(me.userId(), id);
        ReportVO meta = file.meta();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(meta.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(file.stream()));
    }

    @DeleteMapping("/reports/{id}")
    public Result<Void> delete(@AuthenticationPrincipal LoginUser me, @PathVariable("id") Long id) {
        reportService.delete(me.userId(), id);
        return Result.ok();
    }
}
