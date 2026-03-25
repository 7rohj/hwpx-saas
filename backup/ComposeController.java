package com.khwajeong.hwpx_saas.controller;

import com.khwajeong.hwpx_saas.compose.dto.ComposeDebugResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRenderResult;
import com.khwajeong.hwpx_saas.compose.service.ComposeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

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
    ) throws Exception {
        Path templatePath = saveTempFile(templateFile);
        Path sourcePath = saveTempFile(sourceFile);

        try {
            return composeService.composeDebug(templatePath, sourcePath, prompt);
        } finally {
            Files.deleteIfExists(templatePath);
            Files.deleteIfExists(sourcePath);
        }
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
        Path templatePath = saveTempFile(templateFile);
        Path sourcePath = saveTempFile(sourceFile);

        try {
            return composeService.composeRender(templatePath, sourcePath, prompt);
        } finally {
            Files.deleteIfExists(templatePath);
            Files.deleteIfExists(sourcePath);
        }
    }

    private Path saveTempFile(MultipartFile file) throws Exception {
        String original = file.getOriginalFilename();
        String ext = getExtension(original);

        Path temp = Files.createTempFile("upload-", ext);
        file.transferTo(temp.toFile());
        return temp;
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return ".tmp";
        }

        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return ".tmp";
        }

        return filename.substring(idx);
    }
}