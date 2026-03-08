/*
package com.khwajeong.hwpx_saas.service;

import com.khwajeong.hwpx_saas.entity.TemplateEntity;
import com.khwajeong.hwpx_saas.exception.ApiException;
import com.khwajeong.hwpx_saas.exception.ErrorCode;
import com.khwajeong.hwpx_saas.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final StorageService storageService;

    public TemplateEntity getTemplate(Long id) {

        return templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "template not found"));
    }

    @Transactional
    public void deleteTemplate(Long id) {

        TemplateEntity tpl = templateRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "template not found"));

        storageService.delete(tpl.getStoragePath());

        templateRepository.delete(tpl);
    }
}
*/