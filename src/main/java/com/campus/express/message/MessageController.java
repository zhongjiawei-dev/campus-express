package com.campus.express.message;

import com.campus.express.common.Result;
import com.campus.express.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<List<Message>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.ok(messageService.list(UserContext.userId(), page, size));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unread() {
        return Result.ok(Map.of("count", messageService.unreadCount(UserContext.userId())));
    }

    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        messageService.markRead(UserContext.userId(), id);
        return Result.ok();
    }
}
