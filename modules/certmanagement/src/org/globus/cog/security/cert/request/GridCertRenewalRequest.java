/*
 * Swift Parallel Scripting Language (http://swift-lang.org)
 * Code from Java CoG Kit Project (see notice below) with modifications.
 *
 * Copyright 2005-2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// ----------------------------------------------------------------------
// This code is developed as part of the Java CoG Kit project
// The terms of the license can be found at http://www.cogkit.org/license
// This message may not be removed or altered.
// ----------------------------------------------------------------------

/**
 * Copyright (c) 2003, National Research Council of Canada
 * All rights reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this 
 * software and associated documentation files (the "Software"), to deal in the Software 
 * without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice(s) and this licence appear in all copies of the Software or 
 * substantial portions of the Software, and that both the above copyright notice(s) and this 
 * license appear in supporting documentation.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE BE LIABLE 
 * FOR ANY CLAIM, OR ANY DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL 
 * DAMAGES, OR ANY DAMAGES WHATSOEVER (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS 
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWSOEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OF THE SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Except as contained in this notice, the name of a copyright holder shall NOT be used in 
 * advertising or otherwise to promote the sale, use or other dealings in this Software 
 * without specific prior written authorization.  Title to copyright in this software and any 
 * associated documentation will at all times remain with copyright holders.
 */

package org.globus.cog.security.cert.request;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.DERConstructedSet;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Base64;
import org.globus.common.CoGProperties;
import org.globus.gsi.CertUtil;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.OpenSSLKey;
import org.globus.gsi.bc.BouncyCastleCertProcessingFactory;
import org.globus.gsi.bc.BouncyCastleOpenSSLKey;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.util.PEMUtils;
import org.globus.util.Util;
import org.ietf.jgss.GSSCredential;

import cryptix.util.core.Hex;


/**
 * GridCertRenewalRequest Command Line Client
 * @author Jean-Claude Cote
 */
public class GridCertRenewalRequest {

    public static final String usage =
        "\n"
            + "\ngrid-cert-request [-help] [ options ...]"
            + "\n-help"
            + "\n-usage "
            + "\nDisplays usage. "
            + "\n"
            + "\n-version "
            + "\nDisplays version. "
            + "\n"
            + "\n-debug "
            + "\nDisplays extra information (if problems occur). "
            + "\n"
            + "\n-nopassphrase "
            + "\nSignals that the new key will not be protected by a passphrase. "
            + "\n"
            + "\n-oldcert file "
            + "\nLocation of the certificate that is about to expire. If not set,  $HOME/.globus/usercert.pem  "
            + "\n"
            + "\n-oldkey file "
            + "\nLocation of the private key to the certificate that is about to expire. If not set,   $HOME/.globus/userkey.pem"
            + "\n"
            + "\n-newkey file "
            + "\nLocation of the replacement key that is generated by grid-cert-renew. If not set,   $HOME/.globus/userkey_new.pem is assumed. "
            + "\n"
            + "\n-newcertreq file "
            + "\nLocation of the certificate renewal request. If not set,  $HOME/.globus/usercert_renew_request.pem is assumed. "
            + "\n"
            + "\n-force"
            + "\nReplaces any existing renewal request"
            + "\n"
            + "\nchallenge "
            + "\nChallenge text string, as instructed in the CA renewal notification message. This is the only required argument. "
            + "\n";

    private static GlobusGSSCredentialImpl cred = null;
    private static String chalenge = "";
    private static String newPrivKeyLoc = "";
    private static String userCertRenewFile ="";
    private static String oldPassword = "";
    private static boolean noPswd = false;
    private static String userCertFile = "";
    private static String userKeyFile = "";
    private static boolean force = false;
    private static String version = "1.0";
    private static boolean verbose = false;
            
    public static void main(String[] args) {
        boolean bOk = parseCmdLine(args);

        if (bOk) {

            CertUtil.init();
            
            // Get default location of cert.
            CoGProperties props = CoGProperties.getDefault();

            // If cert file loc not specified use default.
            if (userCertFile.length() == 0){
                userCertFile = props.getUserCertFile();
            }
            // If key file loc not specified use default.
            if (userKeyFile.length() == 0){
                userKeyFile = props.getUserKeyFile();
            }
            
            // If renwal cert loc not specified
            if (userCertRenewFile.length() == 0){
                userCertRenewFile = props.getUserCertFile().substring(0, props.getUserCertFile().length() - 4) + "_renew_request.pem";
            }    
            
            // If new key loc not specified
            if (newPrivKeyLoc.length() == 0){
                newPrivKeyLoc = props.getUserKeyFile().substring(0, props.getUserKeyFile().length() - 4) + "_new.pem";
            }

            // Check not to overwrite any of these files.
            if (force == false) {
                boolean bFileExists = false;
                File f = new File(userCertRenewFile);
                if (f.exists()) {
                    System.out.println(userCertRenewFile + " exists");
                    bFileExists = true;
                }
                f = new File(newPrivKeyLoc);
                if (f.exists()) {
                    System.out.println(newPrivKeyLoc + " exists");
                    bFileExists = true;
                }

                if (bFileExists) {
                    System.out.println(
                        "If you wish to overwrite, run the script again with -force.");
                    bOk = false;
                }
            }
        }

        GlobusGSSCredentialImpl cred = null;
        if (bOk){
            // Get password from user.                
            String oldPassword = Util.getInput("Enter private key pass phrase: ");
            
            // Generate a proxy, and keypair from current cert and key
            int credLifetimeSeconds = 300; // life time of proxy 5 min.
            cred = createNewProxy(oldPassword, credLifetimeSeconds, 1024);
            if (cred == null){
                bOk = false;
            }
        }
        
        String newPrivateKeyPassword = "";
        if (bOk && !noPswd) {
            // Get password from user.                
            bOk = false;
            int attempts = 0;

            while (bOk == false && attempts < 3) {
                newPrivateKeyPassword = Util.getInput("Enter new private key pass phrase: ");
                String password2 =
                    Util.getInput("Verify password enter new private key pass phrase: ");
                if (newPrivateKeyPassword.compareTo(password2) != 0) {
                    System.out.println("Verify failure");
                } else {
                    if (newPrivateKeyPassword.length() < 4) {
                        System.out.println(
                            "phrase is too short, needs to be at least 4 chars");
                    } else {
                        bOk = true;
                    }
                }
                attempts++;
            }
        }

        if (bOk){        
                try {
                    genRenewRequest( cred, newPrivateKeyPassword, chalenge, newPrivKeyLoc, userCertRenewFile);
                }
                catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    bOk = false;
                }
                catch (IOException e) {
                    e.printStackTrace();
                    bOk = false;
                }
        }
    }
    
    protected static boolean parseCmdLine(String[] args) {
        boolean bOk = true;
        if (args.length == 0) {
            System.out.println(usage);
            bOk = false;
        } else {
            for (int i = 0; i < args.length && bOk; i++) {
                if (args[i].equalsIgnoreCase("-version")) {
                    System.out.println(version);
                } else if (
                    args[i].equalsIgnoreCase("-help")
                        || args[i].equalsIgnoreCase("-h")
                        || args[i].equalsIgnoreCase("-?")) {
                    System.out.println(usage);
                    bOk = false;
                } else if (
                    args[i].equalsIgnoreCase("-nopw")
                        || args[i].equalsIgnoreCase("-nodes")
                        || args[i].equalsIgnoreCase("-nopassphrase")) {
                    // no password
                    noPswd = true;
                } else if (args[i].equalsIgnoreCase("-verbose")) {
                    verbose = true;
                }
                else if (args[i].equalsIgnoreCase("-oldcert")){
                    userCertFile = args[++i];
                }
                else if (args[i].equalsIgnoreCase("-oldkey")){
                    userKeyFile = args[++i];
                }
                else if (args[i].equalsIgnoreCase("-newkey")){
                    newPrivKeyLoc = args[++i];
                }
                else if (args[i].equalsIgnoreCase("-newcertreq")){
                    userCertRenewFile = args[++i];
                }
                else if (args[i].equalsIgnoreCase("-force")) {
                    // overwrite existing credentials
                    force = true;
                } else {
                    // if last arg
                    if (i == args.length - 1){
                        chalenge = args[i];
                    }
                    else{                               
                        System.out.println(
                            "Error: argument #"
                                + i
                                + "("
                                + args[i]
                                + ") : unknown");
                   }
                }
            }
        }
        return bOk;
    }

    protected static GlobusGSSCredentialImpl createNewProxy(String keyPassword, int lifetime, int bits) {
    
        X509Certificate userCert = null;
        PrivateKey userKey       = null;  
        CertUtil.init();
        
        try {
            OpenSSLKey key = new BouncyCastleOpenSSLKey(userKeyFile);
    
            if (key.isEncrypted()) {
                key.decrypt(keyPassword);
            }
    
            userKey = key.getPrivateKey();
        } catch(IOException e) {
            System.out.println("Error: Failed to load key: " + userKeyFile);
            System.out.println("Make sure you have a valide private key installed.");
            e.printStackTrace();
            return null;
        } catch(GeneralSecurityException e) {
            System.out.println("Error: Wrong grid pass phrase!");
            e.printStackTrace();
            return null;
        }
        
        try {
            userCert = CertUtil.loadCertificate(userCertFile);
        } catch(IOException e) {
            System.out.println("Error: Failed to load cert: " + userCertFile);
            System.out.println("Make sure you have a valide certificate installed.");
            e.printStackTrace();
            return null;
        } catch(GeneralSecurityException e) {
            System.out.println("Error: Unable to load user certificate: " +
                       e.getMessage());
            e.printStackTrace();
            return null;
        }
        
        BouncyCastleCertProcessingFactory factory =
            BouncyCastleCertProcessingFactory.getDefault();
    
        boolean limited = false;
    
        int proxyType = (limited) ? 
            GSIConstants.DELEGATION_LIMITED :
            GSIConstants.DELEGATION_FULL;
        
        try {
            GlobusCredential proxy = 
            factory.createCredential(new X509Certificate[] {userCert},
                         userKey,
                         bits,
                         lifetime,
                         proxyType);
    
            return new GlobusGSSCredentialImpl(proxy,
                               GSSCredential.INITIATE_ONLY);
    
        } catch (Exception e) {
            System.out.println("Failed to create a proxy: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    
    /**
     * The renewal request method is based on the Grid Canada's renew.sh script. 
     * 
     * @param newPrivateKeyPassword
     * @param chalenge
     * @param newPrivKeyLoc
     * @param userCertRenewFile
     * @return
     */
    public static void genRenewRequest( GlobusGSSCredentialImpl cred, String newPrivateKeyPassword, String chalenge, String newPrivKeyLoc, String userCertRenewFile) throws GeneralSecurityException, IOException {
            
        File fTempDigest = null;
        try {
            // Extract the private key, encrypt it in new passphrase and save it as new user key
            // $OPENSSL rsa -des3 -in $TMPPROXY -out $RENEWALKEY
            OpenSSLKey key = new BouncyCastleOpenSSLKey(cred.getPrivateKey());
            if (newPrivateKeyPassword.length() != 0) {
                key.encrypt(newPrivateKeyPassword);
            }
            key.writeTo(new File(newPrivKeyLoc).getAbsolutePath());
            // set read only permissions
            Util.setFilePermissions(newPrivKeyLoc, 600);
            
            // copy proxy cert signed by user
            // $OPENSSL x509 -in $TMPPROXY >> $RENEWALREQ
            PrintStream ps = null;
            X509Certificate cert = null;
            byte[] data = null;
            X509Certificate[] certs = cred.getCertificateChain();
            cert = certs[0];
            data = cert.getEncoded();
            ps = new PrintStream(new FileOutputStream(userCertRenewFile));
            ////// part 1 /////
            ps.print(toCertPEM(data));
            
            // generate a digest which can not be copied 
            // $OPENSSL x509 -in $TMPPROXY > $TMPPROXY.d
            // echo X$1 >> $TMPPROXY.d
            // $OPENSSL dgst < $TMPPROXY.d >> $RENEWALREQ
            fTempDigest = File.createTempFile("digest-", ".pem");
            PrintStream psDigest = new PrintStream(new FileOutputStream(fTempDigest));
            psDigest.print(toCertPEM(data));
            psDigest.println("X" + chalenge);
            psDigest.close();

            FileInputStream inDigest = null;
            inDigest = new FileInputStream(fTempDigest);
            int digestSize = inDigest.available();
            byte[] digestData = new byte[digestSize];
            inDigest.read(digestData, 0, digestSize);
            MessageDigest md = MessageDigest.getInstance("MD5");
            int le = md.getDigestLength();
            byte[] digest = md.digest(digestData);
            /////// part 2 ///////
            ps.println(Hex.toString(digest).toLowerCase());
            
            // generate a cert req signed by the new key. 
            // $OPENSSL x509 -in $TMPPROXY -x509toreq -signkey $RENEWALKEY  >> $RENEWALREQ
            // Generate a certificate request.        
            X509Name name = new X509Name(cert.getIssuerDN().getName());
            DERConstructedSet derSet = new DERConstructedSet();
            PKCS10CertificationRequest request = null;
            request = new PKCS10CertificationRequest("MD5WithRSA", name, cert.getPublicKey(), derSet, key.getPrivateKey());
            /////// part 3 /////
            ps.println("Certificate Request:");
            ps.println("    Data:");
            ps.print(cert.toString());
            ps.print(toCertReqPEM(request.getEncoded()));
            ps.close();
        }
        finally{
            if(fTempDigest != null){
                fTempDigest.delete();
            }
        }
    }

    


    /**
     * Converts to PEM encoding.
     */
    static private String toCertPEM(byte[] data) {
        byte[] enc_data = Base64.encode(data);
        String header = "-----BEGIN CERTIFICATE-----";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PEMUtils.writeBase64(
                out,
                header,
                enc_data,
                "-----END CERTIFICATE-----");
        } catch (IOException e) {
        }
        return new String(out.toByteArray());
    }
    
    /**
     * Converts to PEM encoding.
     */
    static private String toCertReqPEM(byte[] data) {
        byte[] enc_data = Base64.encode(data);
        String header = "-----BEGIN CERTIFICATE REQUEST-----";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PEMUtils.writeBase64(
                out,
                header,
                enc_data,
                "-----END CERTIFICATE REQUEST-----");
        } catch (IOException e) {
        }
        return new String(out.toByteArray());
    }
}
