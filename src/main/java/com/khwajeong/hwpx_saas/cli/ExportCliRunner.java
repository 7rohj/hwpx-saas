package com.khwajeong.hwpx_saas.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.core.ExportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExportCliRunner implements CommandLineRunner {

    private final ExportService exportService;
    private final ObjectMapper objectMapper;

    public ExportCliRunner(ExportService exportService, ObjectMapper objectMapper) {
        this.exportService = exportService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        Map<String, String> a = parseArgs(args);

        // ✅ help 지원
        if (a.containsKey("help") || a.containsKey("h")) {
            printHelp();
            System.exit(0);
            return;
        }

        // ✅ CLI 모드 판별: template/data/out 셋 중 하나라도 있으면 "CLI 실행 의도"로 간주
        boolean wantsCli = a.containsKey("template") || a.containsKey("data") || a.containsKey("out");
        if (!wantsCli) {
            // 인자 없으면 REST 서버 모드(기존대로 부팅 지속)
            return;
        }

        // ✅ CLI 실행인데 인자가 부족하면 안내 후 종료
        if (!a.containsKey("template") || !a.containsKey("data") || !a.containsKey("out")) {
            System.err.println("❌ Missing args. Required: --template=... --data=... --out=...");
            printHelp();
            System.exit(2);
            return;
        }

        Path templatePath = Path.of(a.get("template"));
        Path dataPath = Path.of(a.get("data"));
        Path outPath = Path.of(a.get("out"));

        if (!Files.exists(templatePath)) {
            System.err.println("❌ Template file not found: " + templatePath);
            System.exit(2);
            return;
        }
        if (!Files.exists(dataPath)) {
            System.err.println("❌ Data JSON file not found: " + dataPath);
            System.exit(2);
            return;
        }

        byte[] templateBytes = Files.readAllBytes(templatePath);

        Map<String, Object> data = objectMapper.readValue(
                Files.readString(dataPath),
                new TypeReference<Map<String, Object>>() {}
        );

        byte[] result = exportService.renderHwpx(templateBytes, data);

        // ✅ out 폴더 없으면 생성
        Path parent = outPath.getParent();
        if (parent != null) Files.createDirectories(parent);

        Files.write(outPath, result);

        System.out.println("✅ CLI export done: " + outPath);

        // ✅ CLI 모드면 서버 안 띄우고 즉시 종료
        System.exit(0);
    }

    private Map<String, String> parseArgs(String... args) {
        Map<String, String> map = new HashMap<>();
        for (String s : args) {
            if (!s.startsWith("--")) continue;

            String body = s.substring(2);

            // --help 같은 플래그 지원
            if (!body.contains("=")) {
                map.put(body, "true");
                continue;
            }

            int i = body.indexOf('=');
            String key = body.substring(0, i).trim();
            String val = body.substring(i + 1).trim();

            // "..." 따옴표 제거 (Windows에서 흔함)
            if (val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) {
                val = val.substring(1, val.length() - 1);
            }

            map.put(key, val);
        }
        return map;
    }

    private void printHelp() {
        System.out.println("""
Usage (CLI):
  .\\gradlew.bat bootRun --args="--template=D:\\hwpx-saas\\template.hwpx --data=D:\\hwpx-saas\\data.json --out=D:\\hwpx-saas\\output.hwpx"

Args:
  --template=PATH   (required) input .hwpx template path
  --data=PATH       (required) input json file path
  --out=PATH        (required) output .hwpx path
  --help            show this help

Notes:
  - If you run without args, the app starts as REST server mode.
  - In CLI mode, the process exits right after generating the output.
""");
    }
}
