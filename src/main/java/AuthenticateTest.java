/*
 * Copyright 2012-2015 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements WHICH ARE COMPATIBLE WITH THE APACHE LICENSE, VERSION 2.0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Language;
import com.aerospike.client.Record;
import com.aerospike.client.Value;
import com.aerospike.client.lua.LuaConfig;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.ResultSet;
import com.aerospike.client.query.Statement;
import com.aerospike.client.task.IndexTask;
import com.aerospike.client.task.RegisterTask;

public class AuthenticateTest {
	private static Logger log = Logger.getLogger(AuthenticateTest.class);
	private AerospikeClient client;
	@SuppressWarnings("unused")
	private String seedHost;
	@SuppressWarnings("unused")
	private int port = 3000;
	private Policy policy;
	private WritePolicy writePolicy;

	public AuthenticateTest(String seedHost, int port) throws AerospikeException {
		this.policy = new Policy();
		this.writePolicy = new WritePolicy();
		this.seedHost = seedHost;
		this.port = port;
		this.client = new AerospikeClient(seedHost, port);
	}

	public static void main(String[] args) throws AerospikeException, ParseException {
		Options options = new Options();
		options.addOption("h", "host", true, "Server hostname (default: localhost)");
		options.addOption("p", "port", true, "Server port (default: 3000)");
		options.addOption("u", "usage", false, "Print usage");
		CommandLineParser parser = new PosixParser();
		CommandLine cl = parser.parse(options, args, false);

		if (cl.hasOption("u")) {
			logUsage(options);
			return;
		}

		String host = cl.getOptionValue("h", "127.0.0.1");
		String portString = cl.getOptionValue("p", "3000");
		int port = Integer.parseInt(portString);
		log.info(" Host: " + host);
		log.info(" Port: " + port);
		AuthenticateTest at = new AuthenticateTest(host, port);
		at.run();
	}
	private static void logUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		String syntax = AuthenticateTest.class.getName() + " [<options>]";
		formatter.printHelp(pw, 100, syntax, "options:", options, 0, 2, null);
		log.info(" " + sw.toString());
	}

	public void run() throws AerospikeException {
		@SuppressWarnings("unused")
		Key key = null; 
		Record record = null;
		RecordSet recordSet = null;
		ResultSet resultSet = null;
		Statement stmt = null;	
		File udfFile = null;
		RegisterTask task =	null;
		IndexTask indexTask = null;
		// Change this to match your UDF directory. 
		LuaConfig.SourceDirectory = "udf";
		
		// Print 'register udf/profile.lua'.
		System.out.println("register udf/profile.lua");

		// REGISTER module 'udf/profile.lua'
		udfFile = new File("udf/profile.lua");
		task = this.client.register(null, udfFile.getPath(), udfFile.getName(), Language.LUA); 
		task.waitTillComplete();

		// Print 'create index profileindex'.
		System.out.println("create index profileindex");

		// CREATE INDEX profileindex ON test.profile (username) STRING
		indexTask = this.client.createIndex(this.policy, "test", "profile", "profileindex", "username", IndexType.STRING);
		indexTask.waitTillComplete();
						
		// Print 'add records'.
		System.out.println("add records");

		// INSERT INTO test.profile (PK, username, password) VALUES ('1', 'Charlie', 'cpass')
		this.client.put(this.writePolicy, new Key("test", "profile", Value.get("1")), 
			new Bin("username", Value.get("Charlie")),
			new Bin("password", Value.get("cpass"))
			);
					
		// INSERT INTO test.profile (PK, username, password) VALUES ('2', 'Bill', 'hknfpkj')
		this.client.put(this.writePolicy, new Key("test", "profile", Value.get("2")), 
			new Bin("username", Value.get("Bill")),
			new Bin("password", Value.get("hknfpkj"))
			);
					
		// INSERT INTO test.profile (PK, username, password) VALUES ('3', 'Doug', 'dj6554')
		this.client.put(this.writePolicy, new Key("test", "profile", Value.get("3")), 
			new Bin("username", Value.get("Doug")),
			new Bin("password", Value.get("dj6554"))
			);
					
		// INSERT INTO test.profile (PK, username, password) VALUES ('4', 'Mary', 'ghjks')
		this.client.put(this.writePolicy, new Key("test", "profile", Value.get("4")), 
			new Bin("username", Value.get("Mary")),
			new Bin("password", Value.get("ghjks"))
			);
					
		// INSERT INTO test.profile (PK, username, password) VALUES ('5', 'Julie', 'zzxzxvv')
		this.client.put(this.writePolicy, new Key("test", "profile", Value.get("5")), 
			new Bin("username", Value.get("Julie")),
			new Bin("password", Value.get("zzxzxvv"))
			);
					
		// Print 'query on username'.
		System.out.println("query on username");

		// SELECT * FROM test.profile WHERE username = 'Mary'
		stmt = new Statement();
		stmt.setNamespace("test");
		stmt.setSetName("profile");
		stmt.setFilters(Filter.equal("username", Value.get("Mary")));
		
		// Execute the query.
		recordSet = client.query(null, stmt);

		// Process the record set.
		try {
			while (recordSet != null && recordSet.next()) {
				key = recordSet.getKey();
				record = recordSet.getRecord();
				System.out.println("Record: " + record);
			}
		} 
		finally {
			recordSet.close();
		}

		// Print 'query for Mary'.
		System.out.println("query for Mary");

		// AGGREGATE profile.check_password('ghjks') ON test.profile WHERE username = 'Mary'
		stmt = new Statement();
		stmt.setNamespace("test");
		stmt.setSetName("profile");
		stmt.setFilters(Filter.equal("username", Value.get("Mary")));
		resultSet = client.queryAggregate(null, stmt, "profile", "check_password", Value.get("ghjks"));
				
		try {
			int count = 0;
			
			while (resultSet.next()) {
				Object object = resultSet.getObject();
				System.out.println("Result: " + object);
				count++;
			}
			
			if (count == 0) {
				System.out.println("No results returned");			
			}
		}
		finally {
			resultSet.close();
		}
		// Total AQL statements: 14.
	}
	
	protected void finalize() throws Throwable {
	    if (this.client != null) {
	    	this.client.close();
	        this.client = null;
	    }
	}
	
	protected void printInfo(String title, String infoString) {
		String[] outerParts = infoString.split(";");
		System.out.println(title);
		
		for (String s : outerParts) {
			String[] innerParts = s.split(":");
			
			for (String parts : innerParts) {
				System.out.println("\t" + parts);
			}
			
			System.out.println();
		}
	}
}