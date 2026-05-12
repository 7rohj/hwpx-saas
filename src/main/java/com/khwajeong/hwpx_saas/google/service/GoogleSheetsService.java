package com.khwajeong.hwpx_saas.google.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class GoogleSheetsService {

    @Value("${google.sheets.credentials-path}")
    private String credentialsPath;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    private Sheets getSheets() throws Exception {

        GoogleCredentials credentials =
                GoogleCredentials.fromStream(
                                new FileInputStream(credentialsPath)
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

    public void appendFeedbackRow(
            String action,
            Long id,
            String userEmail,
            String maskedName,
            String content,
            String createdAt,
            String updatedAt
    ) {

        try {

            Sheets sheets = getSheets();

            List<Object> row = Arrays.asList(
                    id,
                    action,
                    userEmail,
                    maskedName,
                    content,
                    createdAt,
                    updatedAt
            );

            ValueRange body = new ValueRange()
                    .setValues(List.of(row));

            sheets.spreadsheets().values()
                    .append(
                            spreadsheetId,
                            "feedback_comments!A:G",
                            body
                    )
                    .setValueInputOption("RAW")
                    .execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}