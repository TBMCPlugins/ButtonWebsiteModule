/*
 * acme4j - Java ACME client
 *
 * Copyright (C) 2015 Richard "Shred" Körber
 *   http://acme4j.shredzone.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */ //Modified
package buttondevteam.website;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.website.page.AcmeChallengePage;

/**
 * A simple client test tool.
 * <p>
 * Pass the names of the domains as parameters.
 */
public class AcmeClient {
	// File name of the User Key Pair
	private static final File USER_KEY_FILE = new File("user.key");

	// File name of the Domain Key Pair
	private static final File DOMAIN_KEY_FILE = new File("domain.key");

	// File name of the CSR
	private static final File DOMAIN_CSR_FILE = new File("domain.csr");

	// File name of the signed certificate
	private static final File DOMAIN_CHAIN_FILE = new File("domain-chain.crt");

	// RSA key size of generated key pairs
	private static final int KEY_SIZE = 2048;

	private static final Logger LOG = LoggerFactory.getLogger(AcmeClient.class);

	/**
	 * Generates a certificate for the given domains. Also takes care for the registration process.
	 *
	 * @param domains
	 *            Domains to get a common certificate for
	 */
	public void fetchCertificate(Collection<String> domains) throws IOException, AcmeException {
		// Load the user key file. If there is no key file, create a new one.
		// Keep this key pair in a safe place! In a production environment, you will not be
		// able to access your account again if you should lose the key pair.
		KeyPair userKeyPair = loadOrCreateKeyPair(USER_KEY_FILE);

		// Create a session for Let's Encrypt.
		// Use "acme://letsencrypt.org" for production server
		Session session = new Session("acme://letsencrypt.org" + (TBMCCoreAPI.IsTestServer() ? "/staging" : ""),
				userKeyPair);

		// Get the Registration to the account.
		// If there is no account yet, create a new one.
		Registration reg = findOrRegisterAccount(session);

		// Separately authorize every requested domain.
		for (String domain : domains) {
			authorize(reg, domain);
		}

		// Load or create a key pair for the domains. This should not be the userKeyPair!
		KeyPair domainKeyPair = loadOrCreateKeyPair(DOMAIN_KEY_FILE);

		// Generate a CSR for all of the domains, and sign it with the domain key pair.
		CSRBuilder csrb = new CSRBuilder();
		csrb.addDomains(domains);
		csrb.sign(domainKeyPair);

		// Write the CSR to a file, for later use.
		try (Writer out = new FileWriter(DOMAIN_CSR_FILE)) {
			csrb.write(out);
		}

		// Now request a signed certificate.
		Certificate certificate = reg.requestCertificate(csrb.getEncoded());

		LOG.info("Success! The certificate for domains " + domains + " has been generated!");
		LOG.info("Certificate URI: " + certificate.getLocation());

		// Download the leaf certificate and certificate chain.
		X509Certificate cert = certificate.download();
		X509Certificate[] chain = certificate.downloadChain();

		// Write a combined file containing the certificate and chain.
		try (FileWriter fw = new FileWriter(DOMAIN_CHAIN_FILE)) {
			CertificateUtils.writeX509CertificateChain(fw, cert, chain);
		}

		// That's all! Configure your web server to use the DOMAIN_KEY_FILE and
		// DOMAIN_CHAIN_FILE for the requested domans.
	}

	/**
	 * Loads a key pair from specified file. If the file does not exist, a new key pair is generated and saved.
	 *
	 * @return {@link KeyPair}.
	 */
	private KeyPair loadOrCreateKeyPair(File file) throws IOException {
		if (file.exists()) {
			try (FileReader fr = new FileReader(file)) {
				return KeyPairUtils.readKeyPair(fr);
			}
		} else {
			KeyPair domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
			try (FileWriter fw = new FileWriter(file)) {
				KeyPairUtils.writeKeyPair(domainKeyPair, fw);
			}
			return domainKeyPair;
		}
	}

	/**
	 * Finds your {@link Registration} at the ACME server. It will be found by your user's public key. If your key is not known to the server yet, a new registration will be created.
	 * <p>
	 * This is a simple way of finding your {@link Registration}. A better way is to get the URI of your new registration with {@link Registration#getLocation()} and store it somewhere. If you need to
	 * get access to your account later, reconnect to it via {@link Registration#bind(Session, URI)} by using the stored location.
	 *
	 * @param session
	 *            {@link Session} to bind with
	 * @return {@link Registration} connected to your account
	 */
	private Registration findOrRegisterAccount(Session session) throws AcmeException, IOException {
		Registration reg;

		URI loc = ButtonWebsiteModule.getRegistration();
		if (loc != null) {
			LOG.info("Loading account from file");
			return Registration.bind(session, loc);
		}

		try {
			// Try to create a new Registration.
			reg = new RegistrationBuilder().create(session);
			LOG.info("Registered a new user, URI: " + reg.getLocation());

			// This is a new account. Let the user accept the Terms of Service.
			// We won't be able to authorize domains until the ToS is accepted.
			URI agreement = reg.getAgreement();
			LOG.info("Terms of Service: " + agreement);
			acceptAgreement(reg, agreement);

		} catch (AcmeConflictException ex) {
			// The Key Pair is already registered. getLocation() contains the
			// URL of the existing registration's location. Bind it to the session.
			reg = Registration.bind(session, ex.getLocation());
			LOG.info("Account does already exist, URI: " + reg.getLocation(), ex);
			ButtonWebsiteModule.storeRegistration(ex.getLocation());
		}

		return reg;
	}

	/**
	 * Authorize a domain. It will be associated with your account, so you will be able to retrieve a signed certificate for the domain later.
	 * <p>
	 * You need separate authorizations for subdomains (e.g. "www" subdomain). Wildcard certificates are not currently supported.
	 *
	 * @param reg
	 *            {@link Registration} of your account
	 * @param domain
	 *            Name of the domain to authorize
	 */
	private void authorize(Registration reg, String domain) throws AcmeException {
		// Authorize the domain.
		Authorization auth = reg.authorizeDomain(domain);
		LOG.info("Authorization for domain " + domain);

		// Find the desired challenge and prepare it.
		Challenge challenge = httpChallenge(auth, domain);

		if (challenge == null) {
			throw new AcmeException("No challenge found");
		}

		// If the challenge is already verified, there's no need to execute it again.
		if (challenge.getStatus() == Status.VALID) {
			return;
		}

		// Now trigger the challenge.
		challenge.trigger();

		// Poll for the challenge to complete.
		try {
			int attempts = 10;
			while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
				// Did the authorization fail?
				if (challenge.getStatus() == Status.INVALID) {
					throw new AcmeException("Challenge failed... Giving up.");
				}

				// Wait for a few seconds
				Thread.sleep(3000L);

				// Then update the status
				challenge.update();
			}
		} catch (InterruptedException ex) {
			LOG.error("interrupted", ex);
			Thread.currentThread().interrupt();
		}

		// All reattempts are used up and there is still no valid authorization?
		if (challenge.getStatus() != Status.VALID) {
			throw new AcmeException("Failed to pass the challenge for domain " + domain + ", ... Giving up.");
		}
	}

	/**
	 * Prepares a HTTP challenge.
	 * <p>
	 * The verification of this challenge expects a file with a certain content to be reachable at a given path under the domain to be tested.
	 * <p>
	 * This example outputs instructions that need to be executed manually. In a production environment, you would rather generate this file automatically, or maybe use a servlet that returns
	 * {@link Http01Challenge#getAuthorization()}.
	 *
	 * @param auth
	 *            {@link Authorization} to find the challenge in
	 * @param domain
	 *            Domain name to be authorized
	 * @return {@link Challenge} to verify
	 */
	public Challenge httpChallenge(Authorization auth, String domain) throws AcmeException {
		// Find a single http-01 challenge
		Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
		if (challenge == null) {
			throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
		}
		// if (ButtonWebsiteModule.PORT == 443)
		LOG.info("Storing the challenge data.");
		/*
		 * else LOG.info("Store the challenge data! Can't do automatically.");
		 */
		LOG.info("It should be reachable at: http://" + domain + "/.well-known/acme-challenge/" + challenge.getToken());
		// LOG.info("File name: " + challenge.getToken());
		// LOG.info("Content: " + challenge.getAuthorization());
		/*
		 * LOG.info("Press any key to continue..."); if (ButtonWebsiteModule.PORT != 443) try { System.in.read(); } catch (IOException e) { e.printStackTrace(); }
		 */
		ButtonWebsiteModule.addHttpPage(new AcmeChallengePage(challenge.getToken(), challenge.getAuthorization()));
		return challenge;
	}

	/**
	 * Presents the user a link to the Terms of Service, and asks for confirmation. If the user denies confirmation, an exception is thrown.
	 *
	 * @param reg
	 *            {@link Registration} User's registration
	 * @param agreement
	 *            {@link URI} of the Terms of Service
	 */
	public void acceptAgreement(Registration reg, URI agreement) throws AcmeException, IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Do you accept the terms? (y/n)");
		if (br.readLine().equalsIgnoreCase("y\n")) {
			throw new AcmeException("User did not accept Terms of Service");
		}

		// Motify the Registration and accept the agreement
		reg.modify().setAgreement(agreement).commit();
		LOG.info("Updated user's ToS");
	}

	/**
	 * Invokes this example.
	 *
	 * @param args
	 *            Domains to get a certificate for
	 */
	public static void main(String... args) {
		if (args.length == 0) {
			TBMCCoreAPI.SendException("Error while doing ACME!", new Exception("No domains given"));
			return;
		}

		LOG.info("Starting up...");

		Collection<String> domains = Arrays.asList(args);
		try {
			AcmeClient ct = new AcmeClient();
			ct.fetchCertificate(domains);
		} catch (Exception ex) {
			LOG.error("Failed to get a certificate for domains " + domains, ex);
		}
	}
}
