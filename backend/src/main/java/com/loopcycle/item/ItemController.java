package com.loopcycle.item;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController @RequestMapping("/api/items")
public class ItemController {
 private final ItemRepository repository;
 public ItemController(ItemRepository repository){this.repository=repository;}
 @GetMapping public List<ItemResponse> all(){return repository.findAll().stream().map(ItemResponse::from).toList();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ItemResponse create(@Valid @RequestBody ItemRequest request){EwasteItem item=new EwasteItem(request.name(),request.category(),request.weightKg(),request.address(),request.description());item.record(ItemStatus.SUBMITTED,"system","Submission created");return ItemResponse.from(repository.save(item));}
 @GetMapping("/{id}") public ItemResponse get(@PathVariable Long id){return repository.findById(id).map(ItemResponse::from).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Item not found"));}
}
