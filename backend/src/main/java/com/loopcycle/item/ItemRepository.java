package com.loopcycle.item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
public interface ItemRepository extends JpaRepository<EwasteItem,Long> {
 @Override @EntityGraph(attributePaths="history") List<EwasteItem> findAll();
 @Override @EntityGraph(attributePaths="history") Optional<EwasteItem> findById(Long id);
 @EntityGraph(attributePaths="history") List<EwasteItem> findAllByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);
 @Query("select i from EwasteItem i order by i.createdAt desc") List<EwasteItem> findAllForReporting();
 @Query("select i from EwasteItem i where i.ownerEmail = :ownerEmail order by i.createdAt desc")
 List<EwasteItem> findAllByOwnerEmailForReporting(@Param("ownerEmail") String ownerEmail);
 @EntityGraph(attributePaths="history") Optional<EwasteItem> findByIdAndOwnerEmail(Long id, String ownerEmail);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select i from EwasteItem i where i.id = :id") Optional<EwasteItem> findByIdForUpdate(@Param("id") Long id);
}
