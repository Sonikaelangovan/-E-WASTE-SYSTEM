package com.loopcycle.item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;
public interface ItemRepository extends JpaRepository<EwasteItem,Long> {
 @Override @EntityGraph(attributePaths="history") List<EwasteItem> findAll();
 @Override @EntityGraph(attributePaths="history") Optional<EwasteItem> findById(Long id);
}
