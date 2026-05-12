package com.khwajeong.hwpx_saas.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class GoogleSheetLogService {

    @Value("${google.sheets.credentials-json}")
    private String credentialsJson;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    private Sheets getSheets() throws Exception {

        GoogleCredentials credentials =
                GoogleCredentials.fromStream(
                                new ByteArrayInputStream(
                                        credentialsJson.getBytes(StandardCharsets.UTF_8)
                                )
                        )
                        .createScoped(List.of(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        )
                .setApplicationName("AutoHWPX")
                .build();
    }

    @Async
    public void send(
            String type,
            String email,
            String provider,
            boolean success,
            String message,
            Long userId
    ) {

        try {

            Sheets sheets = getSheets();

            List<Object> row = Arrays.asList(
                    OffsetDateTime.now().toString(),
                    type,
                    email,
                    provider,
                    success,
                    message,
                    userId
            );

            ValueRange body = new ValueRange()
                    .setValues(List.of(row));

            sheets.spreadsheets().values()
                    .append(
                            spreadsheetId,
                            "logs!A:G",
                            body
                    )
                    .setValueInputOption("RAW")
                    .execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}