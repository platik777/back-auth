package ru.platik777.backauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.platik777.backauth.entity.Block;

import java.util.UUID;

@Repository
public interface BlockRepository extends JpaRepository<Block, UUID> {
}