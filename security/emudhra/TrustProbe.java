import java.net.*;
import java.security.cert.CertPathValidatorException;
import javax.net.ssl.*;
public class TrustProbe {
    public static void main(String[] hosts) throws Exception {
        check("example.com", false);
        for (String host : hosts) check(host, true);
    }
    private static void check(String host, boolean reject) throws Exception {
        try (Socket tcp = new Socket()) {
            tcp.connect(new InetSocketAddress(host, 443), 15000);
            tcp.setSoTimeout(15000);
            try (SSLSocket tls = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(tcp, host, 443, true)) {
                SSLParameters params = tls.getSSLParameters();
                params.setEndpointIdentificationAlgorithm("HTTPS");
                tls.setSSLParameters(params);
                tls.startHandshake();
            }
            if (reject) throw new IllegalStateException("ACCEPTED " + host);
            System.out.println("OK " + host);
        } catch (SSLHandshakeException e) {
            boolean certificateFailure = false;
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause.getClass().getSimpleName().equals("SunCertPathBuilderException")) certificateFailure = true;
                if (cause instanceof CertPathValidatorException &&
                    ((CertPathValidatorException) cause).getReason() == CertPathValidatorException.BasicReason.REVOKED) certificateFailure = true;
            }
            if (!reject || !certificateFailure) throw e;
            System.out.println("REJECTED " + host);
        }
    }
}
