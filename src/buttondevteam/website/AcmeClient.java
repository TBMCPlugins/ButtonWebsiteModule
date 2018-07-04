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

import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.website.page.AcmeChallengePage;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collection;

/**
 * A simple client test tool.
 * <p>
 * Pass the names of the domains as parameters.
 */
public class AcmeClient {
	// File name of the User Key Pair
	public static final File USER_KEY_FILE = new File("user.key");

	// File name of the Domain Key Pair
	public static final File DOMAIN_KEY_FILE = new File("domain.key");

	// File name of the CSR
	public static final File DOMAIN_CSR_FILE = new File("domain.csr");

	// File name of the signed certificate
	public static final File DOMAIN_CHAIN_FILE = new File("domain-chain.crt");

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
		Session session = new Session("acme://letsencrypt.org" + (TBMCCoreAPI.IsTestServer() ? "/staging" : ""));

		// Get the Registration to the account.
		// If there is no account yet, create a new one.
		Account acc = findOrRegisterAccount(session, userKeyPair);

		Order order = acc.newOrder().domains(domains).create();

		// Separately authorize every requested domain.
		for (Authorization auth : order.getAuthorizations()) {
			authorize(auth);
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

		LOG.info("Ordering certificate...");
		// Order the certificate
		order.execute(csrb.getEncoded());

		// Wait for the order to complete
		try {
			int attempts = 10;
			while (order.getStatus() != Status.VALID && attempts-- > 0) {
				// Did the order fail?
				if (order.getStatus() == Status.INVALID) {
					throw new AcmeException("Order failed... Giving up.");
				}

				// Wait for a few seconds
				Thread.sleep(3000L);

				// Then update the status
				order.update();
				if (order.getStatus() != Status.VALID)
					LOG.info("Not yet...");
			}
		} catch (InterruptedException ex) {
			LOG.error("interrupted", ex);
			Thread.currentThread().interrupt();
		}

		// Get the certificate
		Certificate certificate = order.getCertificate();

		if (certificate == null)
			throw new AcmeException("Certificate is null. Wot.");

		LOG.info("Success! The certificate for domains " + domains + " has been generated!");
		LOG.info("Certificate URL: " + certificate.getLocation());

		// Write a combined file containing the certificate and chain.
		try (FileWriter fw = new FileWriter(DOMAIN_CHAIN_FILE)) {
			certificate.writeCertificate(fw);
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
	 * Finds your {@link Account} at the ACME server. It will be found by your user's public key. If your key is not known to the server yet, a new registration will be created.
	 *
	 * @param session
	 *            {@link Session} to bind with
	 * @param kp The user keypair
	 * @return {@link Account} connected to your account
	 */
	private Account findOrRegisterAccount(Session session, KeyPair kp) throws AcmeException, IOException {
		Account acc;

		URI loc = ButtonWebsiteModule.getRegistration();
		if (loc != null) {
			LOG.info("Loading account from file");
			return new Login(loc.toURL(), kp, session).getAccount();
		}

		// Try to create a new Registration.
		AccountBuilder ab = new AccountBuilder().useKeyPair(kp);

		// This is a new account. Let the user accept the Terms of Service.
		// We won't be able to authorize domains until the ToS is accepted.
		URI agreement = session.getMetadata().getTermsOfService();
		acceptAgreement(ab, agreement);
		acc = ab.create(session);
		LOG.info("Registered a new user, URI: " + acc.getLocation());
		ButtonWebsiteModule.storeRegistration(acc.getLocation());

		return acc;
	}

	/**
	 * Authorize a domain. It will be associated with your account, so you will be able to retrieve a signed certificate for the domain later.
	 * <p>
	 * You need separate authorizations for subdomains (e.g. "www" subdomain). Wildcard certificates are not currently supported.
	 *
	 * @param auth
	 *            {@link Authorization} for the domain
	 */
	private void authorize(Authorization auth) throws AcmeException {
		LOG.info("Authorization for domain " + auth.getDomain());

		// The authorization is already valid. No need to process a challenge.
		if (auth.getStatus() == Status.VALID) {
			return;
		}

		Challenge challenge = httpChallenge(auth);

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
			throw new AcmeException("Failed to pass the challenge for domain " + auth.getDomain() + ", ... Giving up.");
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
	 * @return {@link Challenge} to verify
	 */
	public Challenge httpChallenge(Authorization auth) throws AcmeException {
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
		LOG.info("It should be reachable at: http://" + auth.getDomain() + "/.well-known/acme-challenge/" + challenge.getToken());
		// LOG.info("File name: " + challenge.getToken());
		// LOG.info("Content: " + challenge.getAuthorization());
		/*
		 * LOG.info("Press any key to continue..."); if (ButtonWebsiteModule.PORT != 443) try { System.in.read(); } catch (IOException e) { e.printStackTrace(); }
		 */
		ButtonWebsiteModule.addHttpPage(new AcmeChallengePage(challenge.getToken(), challenge.getAuthorization()));
		ButtonWebsiteModule.startHttp();
		try {
			Thread.sleep(1000); // Just to make sure
		} catch (InterruptedException e) {
		}
		return challenge;
	}

	/**
	 * Presents the user a link to the Terms of Service, and asks for confirmation. If the user denies confirmation, an exception is thrown.
	 *
	 * @param ab
	 *            {@link AccountBuilder} for the user
	 * @param agreement
	 *            {@link URI} of the Terms of Service
	 */
	public void acceptAgreement(AccountBuilder ab, URI agreement) throws AcmeException, IOException {
		LOG.info("Terms of Service: " + agreement);
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Do you accept the terms? (y/n)");
		if (br.readLine().equalsIgnoreCase("y\n")) {
			throw new AcmeException("User did not accept Terms of Service");
		}

		// Motify the Registration and accept the agreement
		ab.agreeToTermsOfService();
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
