package com.khwajeong.hwpx_saas.imagefix.controller;

import com.khwajeong.hwpx_saas.imagefix.dto.ImageFixResponse;
import com.khwajeong.hwpx_saas.imagefix.service.ImageFixService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/image-fix")
public class ImageFixController {

    private final ImageFixService imageFixService;

    public ImageFixController(ImageFixService imageFixService) {
        this.imageFixService = imageFixService;
    }

    @PostMapping(
            value = "/repair",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ImageFixResponse repair(
            @RequestPart("image") MultipartFile image,
            @RequestParam("text") String text
    ) {
        return imageFixService.repair(image, text);
    }
}