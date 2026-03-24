package com.khwajeong.hwpx_saas.controller;

import com.khwajeong.hwpx_saas.compose.dto.ComposeResult;
import com.khwajeong.hwpx_saas.compose.service.ComposeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/compose")
public class ComposeController {

    private final ComposeService composeService;

    public ComposeController(ComposeService composeService) {
        this.composeService = composeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComposeResult compose(
            @RequestPart("templateFile") MultipartFile templateFile,
            @RequestPart("sourceFile") MultipartFile sourceFile,
            @RequestPart(value = "prompt", required = false) String prompt
    ) {
        return composeService.compose(templateFile, sourceFile, prompt);
    }
}