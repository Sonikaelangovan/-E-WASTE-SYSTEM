package com.loopcycle.item;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import com.loopcycle.notification.NotificationService;
@RestController @RequestMapping("/api/items")
public class ItemController {
 private final ItemRepository repository;
 private final LifecycleService lifecycle;
 private final NotificationService notifications;
 public ItemController(ItemRepository repository,LifecycleService lifecycle,NotificationService notifications){this.repository=repository;this.lifecycle=lifecycle;this.notifications=notifications;}
 @GetMapping public List<ItemResponse> all(org.springframework.security.core.Authentication authentication){String email=authentication.getName();boolean citizen=authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_CITIZEN"));var items=citizen?repository.findAllByOwnerEmailOrderByCreatedAtDesc(email):repository.findAll();return items.stream().map(ItemResponse::from).toList();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public ItemResponse create(@Valid @RequestBody ItemRequest request,org.springframework.security.core.Authentication authentication){String email=authentication.getName();EwasteItem item=new EwasteItem(request.name(),request.category(),request.weightKg(),request.address(),request.description(),email);item.record(ItemStatus.SUBMITTED,email,"Submission created");item=repository.save(item);notifications.submissionCreated(item);return ItemResponse.from(item);}
 @GetMapping("/{id}") public ItemResponse get(@PathVariable Long id,org.springframework.security.core.Authentication authentication){String email=authentication.getName();boolean citizen=authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_CITIZEN"));var item=citizen?repository.findByIdAndOwnerEmail(id,email):repository.findById(id);return item.map(ItemResponse::from).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Item not found"));}
 @PostMapping("/{id}/lifecycle") public ItemResponse lifecycle(@PathVariable Long id,@Valid @RequestBody LifecycleRequest request,org.springframework.security.core.Authentication authentication){return lifecycle.apply(id,request,authentication);}
}
