package com.picklebook.repository;

import com.picklebook.entity.EmailTemplate;
import com.picklebook.entity.enums.EmailTemplateCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByCode(EmailTemplateCode code);

    List<EmailTemplate> findAllByOrderByCodeAsc();
}
