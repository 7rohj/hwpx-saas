package com.khwajeong.hwpx_saas.api.validator;

import com.khwajeong.hwpx_saas.api.dto.GenerateRequest;
import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;

import java.util.List;
import java.util.Map;

public class GenerateRequestValidator {

  public static void validate(GenerateRequest req) {
    if (req == null) {
      throw new ApiException(ErrorCode.BAD_REQUEST, "request body is required");
    }

    if (req.elements == null) {
      throw new ApiException(ErrorCode.BAD_REQUEST, "elements is required");
    }

    if (req.elements.isEmpty()) {
      throw new ApiException(ErrorCode.BAD_REQUEST, "elements must not be empty");
    }

    for (int i = 0; i < req.elements.size(); i++) {
      Map<String, Object> el = req.elements.get(i);

      if (el == null) {
        throw new ApiException(ErrorCode.BAD_REQUEST, "elements[" + i + "] must not be null");
      }

      String placeholder = str(el.get("placeholder"));
      String type = str(el.get("type"));

      if (placeholder.isBlank()) {
        throw new ApiException(ErrorCode.BAD_REQUEST, "elements[" + i + "].placeholder is required");
      }

      if (type.isBlank()) {
        throw new ApiException(ErrorCode.BAD_REQUEST, "elements[" + i + "].type is required");
      }

      if ("text".equals(type)) {
        String content = str(el.get("content"));
        if (content.isBlank()) {
          throw new ApiException(ErrorCode.BAD_REQUEST, "elements[" + i + "].content is required for text type");
        }
      }

      if ("table".equals(type)) {
        Object tableData = el.get("data");
        if (!(tableData instanceof List<?> list) || list.isEmpty()) {
          throw new ApiException(ErrorCode.BAD_REQUEST, "elements[" + i + "].data is required for table type");
        }
      }
    }
  }

  private static String str(Object v) {
    return v == null ? "" : String.valueOf(v).trim();
  }
}