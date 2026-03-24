package com.khwajeong.hwpx_saas.controller;

import com.khwajeong.hwpx_saas.compose.dto.ComposeDebugResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRenderResult;
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

    @PostMapping(
            value = "/debug",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ComposeDebugResponse composeDebug(
            @RequestPart("templateFile") MultipartFile templateFile,
            @RequestPart("sourceFile") MultipartFile sourceFile,
            @RequestPart(value = "prompt", required = false) String prompt
    ) {
        return composeService.composeDebug(templateFile, sourceFile, prompt);
    }

    @PostMapping(
            value = "/render",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ComposeRenderResult composeRender(
            @RequestPart("templateFile") MultipartFile templateFile,
            @RequestPart("sourceFile") MultipartFile sourceFile,
            @RequestPart(value = "prompt", required = false) String prompt
    ) throws Exception {
        return composeService.composeRender(templateFile, sourceFile, prompt);
    }
}