package org.silkroadpartnership.theway_noti.gsheet.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

@Service
public class GsheetService {

  private static final String APPLICATION_NAME = "theway-noti";

  @Value("${gsheet.spreadsheetId}")
  private String SPREADSHEET_ID;

  private GoogleCredentials getCredentials() {
    InputStream credentialsStream = GsheetService.class.getResourceAsStream("/keys/credentials.json");
    GoogleCredentials credentials = null;
    try {
      credentials = GoogleCredentials.fromStream(credentialsStream)
          .createScoped(Collections.singletonList("https://www.googleapis.com/auth/spreadsheets.readonly"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return credentials;
  }

  private Sheets getSheetsService(GoogleCredentials credentials) {
    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
    Sheets sheetsService = null;
    try {
      sheetsService = new Sheets.Builder(
          GoogleNetHttpTransport.newTrustedTransport(),
          jsonFactory,
          requestInitializer).setApplicationName(APPLICATION_NAME).build();
    } catch (GeneralSecurityException | IOException e) {
      e.printStackTrace();
    }
    return sheetsService;
  }

  private ValueRange getResponse(Sheets sheetsService, String range) {
    ValueRange response = null;
    try {
      response = sheetsService.spreadsheets().values()
          .get(SPREADSHEET_ID, range)
          .execute();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return response;
  }

  public Map<String,List<Object>> getThisWeekSchedule() {
    GoogleCredentials credentials = getCredentials();
    Sheets sheetsService = getSheetsService(credentials);
    LocalDate thisSunday = getThisSunday();
    String range = getRange(thisSunday);

    ValueRange response = getResponse(sheetsService, range);

    List<List<Object>> values = response.getValues();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy.MM.dd");
    String formatted = thisSunday.format(formatter);

    Map<String,List<Object>> weekSchedule = new HashMap<>();

    weekSchedule.put("header", values.get(0));
    for (List<Object> list : values) {
      if( formatted.equals(list.get(0)) ){
        weekSchedule.put("value",list);
        break;
      }
    }
    return weekSchedule;
  }

  private LocalDate getThisSunday() {
    ZoneId seoulZone = ZoneId.of("Asia/Seoul");

    ZonedDateTime nowInSeoul = ZonedDateTime.now(seoulZone);

    LocalTime twoPM = LocalTime.of(14, 0);

    LocalDate today = nowInSeoul.toLocalTime().isAfter(twoPM)
        ? nowInSeoul.toLocalDate().plusDays(1)
        : nowInSeoul.toLocalDate();

    LocalDate thisSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

    return thisSunday;
  }

  private String getRange(LocalDate thisSunday) {
    return thisSunday.getMonthValue() + "월!A1:M8";
  }
}
