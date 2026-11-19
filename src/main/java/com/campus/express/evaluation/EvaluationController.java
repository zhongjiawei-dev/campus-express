package com.campus.express.evaluation;

import com.campus.express.common.Result;
import com.campus.express.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    public Result<Evaluation> submit(@RequestBody EvaluationRequest request) {
        return Result.ok(evaluationService.submit(
                UserContext.userId(),
                request.orderNo(),
                request.score(),
                request.tags(),
                request.content(),
                request.anonymous()));
    }

    @GetMapping("/{orderNo}")
    public Result<Evaluation> get(@PathVariable String orderNo) {
        return Result.ok(evaluationService.findByOrder(orderNo));
    }

    public record EvaluationRequest(
            String orderNo,
            Integer score,
            String tags,
            String content,
            Boolean anonymous
    ) {
    }
}
