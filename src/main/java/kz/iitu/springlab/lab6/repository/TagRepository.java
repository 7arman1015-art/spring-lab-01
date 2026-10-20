package kz.iitu.springlab.lab6.repository;

import kz.iitu.springlab.lab6.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
