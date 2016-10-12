package com.icoderman.evndt.util;

import com.icoderman.evndt.AppOptions;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class DeployUtil {

    private static final String URL_DEPLOY_FORMAT = "http://%s/evn/tbl/unzipProject";
    private static final String URL_AUTH_FORMAT = "http://%s/evn/tbl/j_security_check";
    private static final String URL_LOGIN_PAGE_FORMAT = "http://%s/evn/tbl/";

    private static final String UTF_8 = "UTF-8";
    private static final String FIELD_OLD_PROJECT = "old-project";
    private static final String FIELD_PROJECT_NAME = "projectName";
    private static final String FIELD_PROJECT = "project";
    private static final String VALUE_DELETE = "delete";
    private static final String SUCCESS_DEPLOYMENT_MARK = "was uploaded successfully";
    private static final String FIELD_J_USERNAME = "j_username";
    private static final String FIELD_J_PASSWORD = "j_password";
    private static final String APPLICATION_URLENCODED = "application/x-www-form-urlencoded";


    public static boolean deployProject(AppOptions options, String compressedProject) {

        CookieStore httpCookieStore = new BasicCookieStore();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore).build()) {

            HttpGet loginFormRequest = new HttpGet(String.format(URL_LOGIN_PAGE_FORMAT, options.getServer()));
            try (CloseableHttpResponse response = httpClient.execute(loginFormRequest) ){
                //System.out.println(response.toString());
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return false;
            }

            /// authentication web
            HttpPost authPostRequest = buildAuthPostRequest(options);
            try (CloseableHttpResponse response = httpClient.execute(authPostRequest) ){
                //System.out.println("Successful login...");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return false;
            }

            // deploy project
            HttpPost httpPostRequest = buildRequest(options, compressedProject);
            try (CloseableHttpResponse response = httpClient.execute(httpPostRequest) ){
                return isResponseSuccess(response);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return false;
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static HttpPost buildAuthPostRequest(AppOptions options) {
        HttpPost httpPostRequest = new HttpPost(String.format(URL_AUTH_FORMAT, options.getServer()));

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(FIELD_J_USERNAME, options.getUser()));
        params.add(new BasicNameValuePair(FIELD_J_PASSWORD, options.getPassword()));
        try {
            httpPostRequest.setEntity(new UrlEncodedFormEntity(params));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        httpPostRequest.setHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_URLENCODED);
        return httpPostRequest;
    }

    private static HttpPost buildRequest(AppOptions options, String compressedProject) {
        HttpEntity httpEntity = buildRequestEntity(options.getProject(), compressedProject);
        String authHeader = buildRequestAuthHeader(options.getUser(), options.getPassword());

        HttpPost httpPostRequest = new HttpPost(String.format(URL_DEPLOY_FORMAT, options.getServer()));
        httpPostRequest.setEntity(httpEntity);
        httpPostRequest.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        return httpPostRequest;
    }

    private static boolean isResponseSuccess(CloseableHttpResponse response) {
        if (response == null || response.getStatusLine() == null ||
                response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            return false;
        }
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return false;
        }

        try (InputStream is = entity.getContent()) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, UTF_8);
            String results = writer.toString();
            return results.contains(SUCCESS_DEPLOYMENT_MARK);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static String buildRequestAuthHeader(String user, String password) {
        String auth = String.format("%s:%s", user, password);
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName(UTF_8)));
        return "Basic " + new String(encodedAuth);
    }


    private static HttpEntity buildRequestEntity(String projectName, String projectFile) {
        if (projectName == null || projectFile == null) {
            throw new IllegalArgumentException("projectName and projectFile should not be null");
        }
        MultipartEntityBuilder multipartBuilder = MultipartEntityBuilder.create();
        multipartBuilder.addPart(FIELD_OLD_PROJECT, new StringBody(VALUE_DELETE, ContentType.TEXT_PLAIN));
        multipartBuilder.addPart(FIELD_PROJECT_NAME, new StringBody(projectName, ContentType.TEXT_PLAIN));
        multipartBuilder.addPart(FIELD_PROJECT,  new FileBody(new File(projectFile)));
        return multipartBuilder.build();
    }

}
