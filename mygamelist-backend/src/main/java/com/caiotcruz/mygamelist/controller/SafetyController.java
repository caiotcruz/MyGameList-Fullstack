package com.caiotcruz.mygamelist.controller;
import com.caiotcruz.mygamelist.service.*;
import com.caiotcruz.mygamelist.model.enums.MessagePolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/safety")
public class SafetyController {
 private final CommunitySafety safety;private final ReportService reports;
 public SafetyController(CommunitySafety safety,ReportService reports){this.safety=safety;this.reports=reports;}
 public record Preferences(@NotNull MessagePolicy messagePolicy){}
 @GetMapping("/me") public Map<String,Object> me(){return Map.of("messagePolicy",safety.me().getMessagePolicy(),"moderator",safety.isModerator());}
 @PutMapping("/preferences") public void preferences(@RequestBody @Valid Preferences dto){safety.policy(dto.messagePolicy());}
 @GetMapping("/blocks") public List<Map<String,Object>> blocks(){return safety.blocks();}
 @GetMapping("/relationship/{id}") public Map<String,Boolean> relationship(@PathVariable Long id){return Map.of("blockedByMe",safety.blocks().stream().anyMatch(u->((Number)u.get("id")).longValue()==id),"interactionAllowed",!safety.blocked(safety.me().getId(),id));}
 @PutMapping("/blocks/{id}") public void block(@PathVariable Long id){safety.block(id);}
 @DeleteMapping("/blocks/{id}") public void unblock(@PathVariable Long id){safety.unblock(id);}
 @PostMapping("/reports") public Map<String,Long> report(@RequestBody @Valid ReportService.Request dto){return Map.of("id",reports.submit(dto));}
 @GetMapping("/moderation/reports") public List<Map<String,Object>> reports(@RequestParam(defaultValue="0") int page){return reports.list(page);}
 @PutMapping("/moderation/reports/{id}") public void review(@PathVariable Long id,@RequestBody @Valid ReportService.Decision dto){reports.review(id,dto);}
}
