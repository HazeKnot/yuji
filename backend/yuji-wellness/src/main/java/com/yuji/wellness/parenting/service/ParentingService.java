package com.yuji.wellness.parenting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.wellness.parenting.dto.ParentingDtos;
import com.yuji.wellness.parenting.entity.ParentingCourse;
import com.yuji.wellness.parenting.entity.ParentingLesson;
import com.yuji.wellness.parenting.mapper.ParentingCourseMapper;
import com.yuji.wellness.parenting.mapper.ParentingLessonMapper;
import com.yuji.wellness.profile.service.WellnessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 育儿教育：按宝宝月龄命中课程，先给此刻最相关的一节。
 */
@Service
@RequiredArgsConstructor
public class ParentingService {

    public static final String DISCLAIMER = "课程为健康教育内容，不替代儿科门诊。宝宝出现发热、呼吸急促、精神差、拒奶等情况请及时就医。";

    private final ParentingCourseMapper courseMapper;
    private final ParentingLessonMapper lessonMapper;
    private final WellnessService wellnessService;

    public ParentingDtos.PageVO page(Long userId) {
        Integer months = wellnessService.babyAgeMonths(userId);
        List<ParentingCourse> courses = courseMapper.selectList(
                new LambdaQueryWrapper<ParentingCourse>()
                        .eq(ParentingCourse::getPublished, true)
                        .isNull(ParentingCourse::getDeletedAt)
                        .orderByAsc(ParentingCourse::getMinMonth));
        if (months != null) {
            courses = courses.stream()
                    .filter(c -> c.getMinMonth() <= months && c.getMaxMonth() >= months)
                    .toList();
        }
        return new ParentingDtos.PageVO(months, monthTip(months),
                courses.stream().map(this::toCourseVO).toList(), DISCLAIMER);
    }

    public ParentingDtos.CourseDetailVO detail(Long courseId) {
        ParentingCourse course = courseMapper.selectById(courseId);
        if (course == null || course.getDeletedAt() != null
                || Boolean.FALSE.equals(course.getPublished())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程不存在或已下线");
        }
        List<ParentingDtos.LessonVO> lessons = lessonMapper.selectList(
                        new LambdaQueryWrapper<ParentingLesson>()
                                .eq(ParentingLesson::getCourseId, courseId)
                                .isNull(ParentingLesson::getDeletedAt)
                                .orderByAsc(ParentingLesson::getSortNo))
                .stream()
                .map(l -> new ParentingDtos.LessonVO(l.getId(), l.getTitle(), l.getContent(), l.getDurationMin()))
                .toList();
        return new ParentingDtos.CourseDetailVO(toCourseVO(course), lessons, DISCLAIMER);
    }

    // ---------- 内部 ----------

    private ParentingDtos.CourseVO toCourseVO(ParentingCourse c) {
        long count = lessonMapper.selectCount(new LambdaQueryWrapper<ParentingLesson>()
                .eq(ParentingLesson::getCourseId, c.getId())
                .isNull(ParentingLesson::getDeletedAt));
        String hint = c.getMinMonth().equals(c.getMaxMonth())
                ? c.getMinMonth() + " 月龄" : c.getMinMonth() + "-" + c.getMaxMonth() + " 月龄";
        return new ParentingDtos.CourseVO(c.getId(), c.getCode(), c.getTitle(), c.getSummary(),
                c.getMinMonth(), c.getMaxMonth(), hint, (int) count, c.getSource());
    }

    static String monthTip(Integer months) {
        if (months == null) {
            return "填写宝宝生日后，这里会自动显示当前月龄最相关的课程。";
        }
        if (months < 6) {
            return "这个阶段最重要的是回应与照护：按需喂养、及时回应、每天一点俯趴时间。";
        }
        if (months < 12) {
            return "满 6 月龄开始添加辅食，一次一种、连续三天观察，不加盐和糖。";
        }
        if (months < 24) {
            return "一岁后是语言爆发期，多描述、多回应，比早教课更有用。";
        }
        return "两岁后开始建立边界与如厕习惯，不强迫、不和别人家孩子比较。";
    }
}
