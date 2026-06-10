package com.training.controller;
import jakarta.validation.Valid;

import com.training.dto.request.QuestionRequest;
import com.training.dto.response.PageResult;
import com.training.dto.response.Result;
import com.training.entity.Question;
import com.training.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<PageResult<Question>> list(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("查询题目列表: courseId={}, questionType={}, difficulty={}, page={}, size={}", courseId, questionType, difficulty, page, size);
        PageResult<Question> result = questionService.listQuestions(courseId, questionType, difficulty, page, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Question> getById(@PathVariable Long id) {
        log.info("查询题目详情: id={}", id);
        Question question = questionService.getQuestionById(id);
        return Result.success(question);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Question> create(@RequestBody @Valid QuestionRequest request) {
        log.info("创建题目: courseId={}, questionType={}", request.getCourseId(), request.getQuestionType());
        Question question = questionService.createQuestion(request);
        return Result.success(question);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid QuestionRequest request) {
        log.info("更新题目: id={}", id);
        questionService.updateQuestion(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除题目: id={}", id);
        questionService.deleteQuestion(id);
        return Result.success();
    }
}
