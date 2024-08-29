package com.fj;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import java.util.ArrayList;
import java.util.List;

public class App2 {
    private static final String PROXY_HOST = System.getenv("PROXY_HOST"); // 「http://」は不要
    private static final String PROXY_PORT = System.getenv("PROXY_PORT");
    private static final String NTLM_DOMAIN = System.getenv("NTLM_DOMAIN");
    private static final String PROXY_USER = System.getenv("PROXY_USER");
    private static final String PROXY_PASS = System.getenv("PROXY_PASS");

    private static final String SF_ENDPOINT = "https://login.salesforce.com/services/Soap/u/53.0";
    private static final String SF_USER = System.getenv("SF_USER");
    private static final String SF_PASS = System.getenv("SF_PASS");

    public static void main(String[] args) throws Exception {
        System.out.println("Hello World");
        PartnerConnection pConnection = generateConnection();
        System.out.println(pConnection);
        System.out.println("connection success!!");

        createObj(pConnection);
        for (int i = 0; i < 1; i++) {
            createObjs(pConnection);
        }

        readObj(pConnection);
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
}
