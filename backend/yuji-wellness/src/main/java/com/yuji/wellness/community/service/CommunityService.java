package com.yuji.wellness.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.wellness.community.dto.CommunityDtos;
import com.yuji.wellness.community.entity.CommunityComment;
import com.yuji.wellness.community.entity.CommunityLike;
import com.yuji.wellness.community.entity.CommunityPost;
import com.yuji.wellness.community.mapper.CommunityCommentMapper;
import com.yuji.wellness.community.mapper.CommunityLikeMapper;
import com.yuji.wellness.community.mapper.CommunityPostMapper;
import com.yuji.wellness.support.AiModerationClient;
import com.yuji.wellness.support.PostCategory;
import com.yuji.wellness.support.RiskAlertService;
import com.yuji.wellness.support.TimeText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 她社区：经验分享与匿名树洞。
 *
 * <p>发帖与评论统一过 AI 风控（AiModerationClient）：
 * 危机信号 → 待审核 + 红色告警工单；导流广告 → 待审核；
 * 消极情绪 → 正常发布但生成黄色预警（消极人选筛查）。
 * 匿名帖只展示"匿名姐妹"，服务端不会把 user_id 返回给前端。
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final int PAGE_SIZE_MAX = 30;

    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityLikeMapper likeMapper;
    private final UserMapper userMapper;
    private final AiModerationClient moderation;
    private final RiskAlertService riskAlert;

    public CommunityDtos.PageVO<CommunityDtos.PostVO> page(Long userId, String category,
                                                           long page, long size) {
        long p = Math.max(page, 1);
        long s = Math.min(Math.max(size, 1), PAGE_SIZE_MAX);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .isNull(CommunityPost::getDeletedAt)
                .eq(CommunityPost::getStatus, "PUBLISHED");
        if (category != null && !category.isBlank()) {
            wrapper.eq(CommunityPost::getCategory, PostCategory.of(category).name());
        }
        wrapper.orderByDesc(CommunityPost::getCreatedAt);

        Page<CommunityPost> result = postMapper.selectPage(new Page<>(p, s), wrapper);
        Set<Long> likedIds = likedIds(userId, result.getRecords());
        List<CommunityDtos.PostVO> records = result.getRecords().stream()
                .map(post -> toVO(post, likedIds.contains(post.getId()), false))
                .toList();
        return new CommunityDtos.PageVO<>(records, result.getTotal(), p, s,
                p * s < result.getTotal());
    }

    public CommunityDtos.PostVO detail(Long userId, Long id) {
        CommunityPost post = requireVisible(userId, id);
        boolean liked = likeMapper.selectCount(new LambdaQueryWrapper<CommunityLike>()
                .eq(CommunityLike::getPostId, id)
                .eq(CommunityLike::getUserId, userId)) > 0;
        return toVO(post, liked, true);
    }

    @Transactional
    public CommunityDtos.PostVO create(Long userId, CommunityDtos.PostReq req) {
        PostCategory category = PostCategory.of(req.category());
        String content = req.content().trim();

        // AI 风控：危机/导流进待审核，消极情绪放行但对人预警
        AiModerationClient.ScanResult scan =
                moderation.scan(req.title() + "\n" + content, "community");

        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setAuthorName(nicknameOf(userId));
        post.setCategory(category.name());
        post.setTitle(req.title().trim());
        post.setContent(content);
        post.setAnonymous(Boolean.TRUE.equals(req.anonymous()));
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus(scan.isAlert() || scan.isBlock() ? "PENDING" : "PUBLISHED");
        post.setRiskLevel(scan.level());
        post.setRiskScore(scan.score());
        post.setRiskTags(scan.tags().isEmpty() ? null : String.join(",", scan.tags()));
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.insert(post);

        if (scan.isAlert()) {
            riskAlert.alert(userId, "COMMUNITY", "RED",
                    "AI 风控：帖子命中危机表达「" + String.join("、", scan.tags()) + "」，已进入人工审核");
        } else if (scan.isWatch()) {
            riskAlert.alert(userId, "COMMUNITY", "YELLOW",
                    "AI 风控：帖子出现消极情绪表达「" + String.join("、", scan.tags()) + "」，建议关注该用户状态");
        }
        return toVO(post, false, true);
    }

    public List<CommunityDtos.CommentVO> comments(Long postId) {
        return commentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                        .eq(CommunityComment::getPostId, postId)
                        .isNull(CommunityComment::getDeletedAt)
                        .orderByAsc(CommunityComment::getCreatedAt))
                .stream().map(this::toCommentVO).toList();
    }

    @Transactional
    public CommunityDtos.CommentVO addComment(Long userId, Long postId, CommunityDtos.CommentReq req) {
        CommunityPost post = requireVisible(userId, postId);
        if (!"PUBLISHED".equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "帖子还在审核中，暂时不能评论");
        }
        // 评论只做拦截（危机/导流直接拒），消极预警仍由帖子与打卡承担
        AiModerationClient.ScanResult scan = moderation.scan(req.content(), "community");
        if (scan.isAlert() || scan.isBlock()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "评论包含不当内容（" + String.join("、", scan.tags()) + "），已拦截");
        }
        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setAuthorName(nicknameOf(userId));
        comment.setContent(req.content().trim());
        comment.setAnonymous(Boolean.TRUE.equals(req.anonymous()));
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        post.setCommentCount((post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1);
        postMapper.updateById(post);
        return toCommentVO(comment);
    }

    @Transactional
    public CommunityDtos.LikeVO toggleLike(Long userId, Long postId) {
        CommunityPost post = requireVisible(userId, postId);
        CommunityLike existing = likeMapper.selectOne(new LambdaQueryWrapper<CommunityLike>()
                .eq(CommunityLike::getPostId, postId)
                .eq(CommunityLike::getUserId, userId));
        int count = post.getLikeCount() == null ? 0 : post.getLikeCount();
        boolean liked;
        if (existing == null) {
            CommunityLike like = new CommunityLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeMapper.insert(like);
            count++;
            liked = true;
        } else {
            likeMapper.deleteById(existing.getId());
            count = Math.max(0, count - 1);
            liked = false;
        }
        post.setLikeCount(count);
        postMapper.updateById(post);
        return new CommunityDtos.LikeVO(liked, count);
    }

    // ---------- 机构端 ----------

    public CommunityDtos.PageVO<CommunityDtos.PostVO> adminPage(String status, long page, long size) {
        long p = Math.max(page, 1);
        long s = Math.min(Math.max(size, 1), PAGE_SIZE_MAX);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .isNull(CommunityPost::getDeletedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(CommunityPost::getStatus, status);
        }
        wrapper.orderByDesc(CommunityPost::getCreatedAt);
        Page<CommunityPost> result = postMapper.selectPage(new Page<>(p, s), wrapper);
        return new CommunityDtos.PageVO<>(result.getRecords().stream()
                .map(post -> toVO(post, false, true)).toList(),
                result.getTotal(), p, s, p * s < result.getTotal());
    }

    public void updateStatus(Long id, String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("PUBLISHED", "PENDING", "HIDDEN").contains(s)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的帖子状态");
        }
        CommunityPost post = postMapper.selectById(id);
        if (post == null || post.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        post.setStatus(s);
        postMapper.updateById(post);
    }

    // ---------- 内部 ----------

    private CommunityPost requireVisible(Long userId, Long id) {
        CommunityPost post = id == null ? null : postMapper.selectById(id);
        if (post == null || post.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在或已删除");
        }
        if (!"PUBLISHED".equals(post.getStatus()) && !userId.equals(post.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在或已删除");
        }
        return post;
    }

    private Set<Long> likedIds(Long userId, List<CommunityPost> posts) {
        if (posts.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = posts.stream().map(CommunityPost::getId).toList();
        return likeMapper.selectList(new LambdaQueryWrapper<CommunityLike>()
                        .eq(CommunityLike::getUserId, userId)
                        .in(CommunityLike::getPostId, ids))
                .stream().map(CommunityLike::getPostId).collect(Collectors.toSet());
    }

    private String nicknameOf(Long userId) {
        User user = userMapper.selectById(userId);
        String nickname = user == null ? null : user.getNickname();
        return nickname == null || nickname.isBlank() ? "一位姐妹" : nickname;
    }

    private CommunityDtos.PostVO toVO(CommunityPost post, boolean liked, boolean full) {
        String content = post.getContent();
        if (!full && content != null && content.length() > 140) {
            content = content.substring(0, 140) + "…";
        }
        String author = Boolean.TRUE.equals(post.getAnonymous()) ? "匿名姐妹" : post.getAuthorName();
        return new CommunityDtos.PostVO(post.getId(), post.getCategory(),
                PostCategory.labelOf(post.getCategory()), post.getTitle(), content, author,
                post.getAnonymous(), post.getLikeCount(), post.getCommentCount(), liked,
                post.getStatus(), TimeText.of(post.getCreatedAt()), post.getCreatedAt());
    }

    private CommunityDtos.CommentVO toCommentVO(CommunityComment comment) {
        String author = Boolean.TRUE.equals(comment.getAnonymous()) ? "匿名姐妹" : comment.getAuthorName();
        return new CommunityDtos.CommentVO(comment.getId(), author, comment.getContent(),
                TimeText.of(comment.getCreatedAt()), comment.getCreatedAt());
    }
}
