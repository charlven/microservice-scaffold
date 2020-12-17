package com.charlven.dummyservice.utils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequester {

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String PATCH = "PATCH";
    public static final String OPTIONS = "OPTIONS";
    public static final String HEAD = "HEAD";
    public static final String TRACE = "TRACE";

    private String charset = "UTF-8";

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "application/xml";
    public static final String CONTENT_TYPE_ENCODE = "application/x-www-form-urlencoded";
    private String method = GET;

    private int connectTimeout = 3000;
    private int readTimeout = 30000;

    private Map<String, String> requestProperties = new HashMap<>();
    private Map<String, String> requestFormParams = new LinkedHashMap<>();

    private String requestContent;
    private String path;

    private HttpRequester() {
    }


    public String send() throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = calPath(path);

            conn = (HttpURLConnection) url.openConnection();

            acceptAllHttps(conn);
            setRequestProperty(conn);
            doSettingAndConnect(conn);
            writeRequestBody(conn);
            finish(conn);

            return getResponseResult(conn);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


    public static HttpRequester newHttpRequester() {
        return new HttpRequester();
    }


    private void acceptAllHttps(HttpURLConnection conn) {
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(DummySSLSocketFactory.getDefault());
            ((HttpsURLConnection) conn).setHostnameVerifier((hostName, sslSession) -> true);
        }
    }

    private void setRequestProperty(HttpURLConnection conn) {
        for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    private void doSettingAndConnect(HttpURLConnection conn) throws IOException {
        boolean isNotMethodGet = !GET.equalsIgnoreCase(method);

        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(isNotMethodGet);
        conn.connect();
    }

    private void writeRequestBody(HttpURLConnection conn) throws IOException {
        if (GET.equalsIgnoreCase(method)) {
            return;
        }
        String requestBody = getRequestBodyByContentType();

        if (!isBlank(requestBody)) {
            OutputStream requestStream = conn.getOutputStream();
            requestStream.write(requestBody.getBytes(charset));
        }
    }

    private String getRequestBodyByContentType() throws IOException {
        String contentType = getContentType();
        if (contentType != null && contentType.toLowerCase().contains(CONTENT_TYPE_ENCODE)) {
            return requestParamsToUrlParam();
        } else if (requestContent != null) {
            return requestContent;
        }
        return null;
    }

    private void finish(HttpURLConnection conn) throws IOException {
        if (conn.getDoOutput()) {
            conn.getOutputStream().flush();
        }
    }

    private String getResponseResult(HttpURLConnection conn) throws IOException {
        InputStream in;
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            in = conn.getInputStream();
        } else {
            in = conn.getErrorStream();
        }
        return new String(input2byte(in), charset);
    }

    private static byte[] input2byte(InputStream inStream)
            throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        return swapStream.toByteArray();
    }

    private URL calPath(String path) throws MalformedURLException, UnsupportedEncodingException {
        if (GET.equalsIgnoreCase(method)) {
            int paramIndex = path.indexOf("?");
            if (paramIndex > 0) {
                path += "&" + requestParamsToUrlParam();
            } else {
                path += "?" + requestParamsToUrlParam();
            }
        }
        if (path.startsWith("http")) {
            return new URL(path);
        }
        throw new MalformedURLException("NOT a http(s) request!");
    }

    private String requestParamsToUrlParam() throws UnsupportedEncodingException {
        StringBuilder urlParams = new StringBuilder();
        for (Map.Entry<String, String> entry : requestFormParams.entrySet()) {
            String encodedValue = URLEncoder.encode(entry.getValue(), charset);

            urlParams.append(entry.getKey()).append("=").append(encodedValue).append("&");
        }
        return urlParams.toString().length() > 0 ? urlParams.toString().replaceFirst("&$", "") : urlParams.toString();
    }

    private String getContentType() {
        return requestProperties.get("Content-Type");
    }

    public HttpRequester headers(String key, String value) {
        requestProperties.put(key, value);
        return this;
    }

    public HttpRequester param(String key, String value) {
        if (key != null && value != null) {
            requestFormParams.put(key, value);
        }
        return this;
    }

    public HttpRequester method(String method) {
        this.method = method;
        return this;
    }

    public HttpRequester charset(String charset) {
        this.charset = charset;
        return this;
    }

    public HttpRequester connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public HttpRequester readTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public HttpRequester requestContent(String requestContent) {
        this.requestContent = requestContent;
        return this;
    }

    public HttpRequester url(String path) {
        this.path = path;
        return this;
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }


    abstract static class CustomSSLSocketFactory extends javax.net.ssl.SSLSocketFactory {

        private javax.net.ssl.SSLSocketFactory impl;

        CustomSSLSocketFactory() {
            try {
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
                sslContext.init(null, initTrustManager(), null);
                this.impl = sslContext.getSocketFactory();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        abstract javax.net.ssl.TrustManager[] initTrustManager() throws Exception;

        @Override
        public String[] getDefaultCipherSuites() {
            return impl.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return impl.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return configSocket(impl.createSocket(socket, host, port, autoClose));
        }

        @Override
        public Socket createSocket() throws IOException {
            return configSocket(impl.createSocket());
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return configSocket(impl.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return configSocket(impl.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return configSocket(impl.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress host, int port, InetAddress localHost, int localPort) throws IOException {
            return configSocket(impl.createSocket(host, port, localHost, localPort));
        }

        protected Socket configSocket(Socket socket) {
            return socket;
        }
    }


    public static class DummySSLSocketFactory extends CustomSSLSocketFactory {

        @Override
        javax.net.ssl.TrustManager[] initTrustManager() {
            // 构造虚构的 TrustManager
            return new javax.net.ssl.TrustManager[]{new javax.net.ssl.X509TrustManager() {

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) {
                }
            }};
        }


        @Override
        protected Socket configSocket(Socket socket) {
            if (socket instanceof javax.net.ssl.SSLSocket) {
                //ignore
            }
            return socket;
        }


        public static javax.net.ssl.SSLSocketFactory getDefault() {
            return new DummySSLSocketFactory();
        }
    }


}