package com.fj;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import java.util.ArrayList;
import java.util.List;

public class App {
    private static final String PROXY_HOST = System.getenv("PROXY_HOST"); // 「http://」は不要
    private static final String PROXY_PORT = System.getenv("PROXY_PORT");
    private static final String NTLM_DOMAIN = System.getenv("NTLM_DOMAIN");
    private static final String PROXY_USER = System.getenv("PROXY_USER");
    private static final String PROXY_PASS = System.getenv("PROXY_PASS");

    private static final String SF_ENDPOINT = System.getenv("SF_DOMAIN") + "/services/Soap/u/53.0";
    private static final String SF_USER = System.getenv("SF_USER");
    private static final String SF_PASS = System.getenv("SF_PASS");

    public static void main(String[] args) throws Exception {
        System.out.println("Hello World");
        PartnerConnection pConnection = generateConnection();
        System.out.println(pConnection);
        System.out.println("connection success!!");

        // createObj(pConnection);
        // for (int i = 0; i < 1; i++) {
        //     createObjs(pConnection);
        // }
        //
        // readObj(pConnection);

        // Salesforce REST API呼び出し
        // callSalesforceRestApi(pConnection);

        // ファイルをアップロードするためのREST APIを呼び出す
        uploadFileToSalesforce(pConnection);
    }

    private static PartnerConnection generateConnection() throws ConnectionException {
        ConnectorConfig connectorConfig = new ConnectorConfig();

        if (PROXY_HOST != null) connectorConfig.setProxy(PROXY_HOST, Integer.parseInt(PROXY_PORT));
        if (NTLM_DOMAIN != null) connectorConfig.setNtlmDomain(NTLM_DOMAIN);
        if (PROXY_USER != null) connectorConfig.setProxyUsername(PROXY_USER);
        if (PROXY_PASS != null) connectorConfig.setProxyPassword(PROXY_PASS);

        connectorConfig.setAuthEndpoint(SF_ENDPOINT);
        connectorConfig.setServiceEndpoint(SF_ENDPOINT);
        connectorConfig.setUsername(SF_USER);
        connectorConfig.setPassword(SF_PASS);
        connectorConfig.setManualLogin(false);

        PartnerConnection pConnection = new PartnerConnection(connectorConfig);
        return pConnection;
    }

    private static void createObj(PartnerConnection pConnection) throws ConnectionException {
        SObject newObj = new SObject();
        newObj.setType("Account");
        newObj.setField("Name", "soap test data");
        SaveResult[] results = pConnection.create(new SObject[]{newObj});
        for (SaveResult result : results) {
            System.out.println(result);
        }
    }

    private static void createObjs(PartnerConnection pConnection) throws ConnectionException {
        List<SObject> newObjs = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            SObject newObj = new SObject();
            newObj.setType("Account");
            newObj.setField("Name", "soap test data " + i);
            newObjs.add(newObj);
        }

        SObject[] objs = newObjs.toArray(new SObject[0]);
        SaveResult[] results = pConnection.create(objs);
        for (SaveResult result : results) {
            System.out.println(result.toString());
        }
    }

    private static void readObj(PartnerConnection pConnection) throws ConnectionException {
        String soql = """
                SELECT
                    Id,
                    Name,
                    CreatedDate
                FROM
                    Account
                """;
        QueryResult result = pConnection.query(soql);
        while (true) {
            SObject[] records = result.getRecords();
            System.out.println("records.length = " + records.length);
            if (records == null || records.length == 0) {
                break;
            }

            for (SObject sobj : records) {
                String str = String.format(
                        "ID = %s, Name = %s, CreatedDate = %s",
                        sobj.getId(),
                        sobj.getField("Name"),
                        sobj.getField("CreatedDate")
                );
//                System.out.println(str);
            }

            if (result.isDone()) {
                break;
            } else {
                result = pConnection.queryMore(result.getQueryLocator());
            }
        }
    }

    private static void callSalesforceRestApi(PartnerConnection pConnection) throws Exception {
        // インスタンスURLとクエリ
        String instanceUrl = pConnection.getConfig().getServiceEndpoint().split("/services/Soap")[0];
        String query = "SELECT+Name+FROM+Account";
        String restEndpoint = instanceUrl + "/services/data/v52.0/query/?q=" + query;

        // HTTP接続を作成
        URL url = new URL(restEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // SalesforceセッションIDをAuthorizationヘッダーに設定
        String sessionId = pConnection.getConfig().getSessionId();
        connection.setRequestProperty("Authorization", "Bearer " + sessionId);

        // レスポンスを取得
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // 結果をコンソールに出力
            System.out.println("Response from Salesforce REST API:");
            System.out.println(response.toString());
        } else {
            System.out.println("GET request failed. Response Code: " + responseCode);
        }

        connection.disconnect();
    }


    private static void uploadFileToSalesforce(PartnerConnection pConnection) throws Exception {
        String instanceUrl = pConnection.getConfig().getServiceEndpoint().split("/services/Soap")[0];
        String restEndpoint = instanceUrl + "/services/data/v61.0/sobjects/ContentVersion";

        // HTTP接続を設定
        URL url = new URL(restEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + pConnection.getConfig().getSessionId());
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=boundary_string");

        // ファイルのパスを指定
        String filePath = "C:\\Users\\yuya.fujii\\OneDrive - FUJITSU\\ドキュメント\\600MB_file.txt";

        // マルチパートデータの準備
        String boundary = "boundary_string";
        String crlf = "\r\n";
        String twoHyphens = "--";

        try (DataOutputStream request = new DataOutputStream(connection.getOutputStream())) {
            // メタデータパート
            request.writeBytes(twoHyphens + boundary + crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"entity_content\"" + crlf);
            request.writeBytes("Content-Type: application/json" + crlf);
            request.writeBytes(crlf);
            request.writeBytes("{\"PathOnClient\":\"Testfile.txt\"}" + crlf);

            // ファイルデータパート
            request.writeBytes(twoHyphens + boundary + crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"VersionData\"; filename=\"" + filePath + "\"" + crlf);
            request.writeBytes("Content-Type: text/plain" + crlf);
            request.writeBytes(crlf);

            // ファイルの内容を読み込んで送信(4096byte毎に逐次送信※メモリに貯めこまない)
            try (FileInputStream fileInputStream = new FileInputStream(new File(filePath))) {
                int bytesRead;
                byte[] buffer = new byte[4096];
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    request.write(buffer, 0, bytesRead);
                }
            }

            // マルチパート終了
            request.writeBytes(crlf);
            request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
            request.flush();
        }

        // レスポンスの取得
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                // レスポンスを表示
                System.out.println("Response from Salesforce REST API:");
                System.out.println(response.toString());
            }
        } else {
            System.out.println("Request failed. Response Code: " + responseCode);
        }

        connection.disconnect();
    }

}
