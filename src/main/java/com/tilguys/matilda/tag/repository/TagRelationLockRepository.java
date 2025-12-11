package com.tilguys.matilda.tag.repository;

import com.tilguys.matilda.tag.domain.TagRelationLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRelationLockRepository extends JpaRepository<TagRelationLock, Long> {
    Optional<TagRelationLock> findByLockName(String lockName);
}
