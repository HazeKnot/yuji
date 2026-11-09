package com.yuji.family.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.family.dto.TaskAssignReq;
import com.yuji.family.dto.TaskStatusReq;
import com.yuji.family.dto.TaskTemplateVO;
import com.yuji.family.dto.TaskVO;
import com.yuji.family.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** 妈妈看全家任务，家属只看派给自己的 */
    @GetMapping("/tasks")
    public Result<List<TaskVO>> list(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(taskService.list(me.userId()));
    }

    /** 按任务包批量下发（仅妈妈） */
    @PostMapping("/tasks/assign")
    public Result<List<TaskVO>> assign(@AuthenticationPrincipal LoginUser me,
                                       @Valid @RequestBody TaskAssignReq req) {
        return Result.ok(taskService.assign(me.userId(), req));
    }

    /** 打卡：DOING / DONE / PENDING */
    @PutMapping("/tasks/{id}/status")
    public Result<TaskVO> updateStatus(@AuthenticationPrincipal LoginUser me,
                                       @PathVariable("id") Long id,
                                       @Valid @RequestBody TaskStatusReq req) {
        return Result.ok(taskService.updateStatus(me.userId(), id, req.status()));
    }

    @GetMapping("/task-templates")
    public Result<List<TaskTemplateVO>> templates() {
        return Result.ok(taskService.templates());
    }
}
