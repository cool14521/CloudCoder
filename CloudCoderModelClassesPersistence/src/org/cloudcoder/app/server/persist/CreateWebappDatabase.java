// CloudCoder - a web-based pedagogical programming environment
// Copyright (C) 2011-2012, Jaime Spacco <jspacco@knox.edu>
// Copyright (C) 2011-2012, David H. Hovemeyer <david.hovemeyer@gmail.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.cloudcoder.app.server.persist;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Scanner;

import org.cloudcoder.app.shared.model.Change;
import org.cloudcoder.app.shared.model.ConfigurationSetting;
import org.cloudcoder.app.shared.model.ConfigurationSettingName;
import org.cloudcoder.app.shared.model.Course;
import org.cloudcoder.app.shared.model.CourseRegistration;
import org.cloudcoder.app.shared.model.CourseRegistrationType;
import org.cloudcoder.app.shared.model.Event;
import org.cloudcoder.app.shared.model.ModelObjectSchema;
import org.cloudcoder.app.shared.model.Problem;
import org.cloudcoder.app.shared.model.ProblemLicense;
import org.cloudcoder.app.shared.model.ProblemType;
import org.cloudcoder.app.shared.model.SubmissionReceipt;
import org.cloudcoder.app.shared.model.Term;
import org.cloudcoder.app.shared.model.TestCase;
import org.cloudcoder.app.shared.model.TestResult;
import org.cloudcoder.app.shared.model.User;

/**
 * Create the webapp database, using the metadata information
 * specified by the model classes.
 * 
 * @author David Hovemeyer
 */
public class CreateWebappDatabase {
	static final boolean DEBUG = false;
	
	public static void main(String[] args) throws Exception {
		try {
			createWebappDatabase();
		} catch (SQLException e) {
			// Handle SQLException by printing an error message:
			// these are likely to be meaningful to the user
			// (for example, can't create the database because
			// it already exists.)
		    e.printStackTrace(System.err);
			System.err.println("Database error: " + e.getMessage());
		}
	}

	private static void createWebappDatabase() throws ClassNotFoundException,
			FileNotFoundException, IOException, SQLException {
		Scanner keyboard = new Scanner(System.in);
		
		System.out.print("Enter a username for your CloudCoder account: ");
		String ccUserName = keyboard.nextLine();
		
		System.out.print("Enter a password for your CloudCoder account: ");
		String ccPassword = keyboard.nextLine();
		
		System.out.print("What is your institution name (e.g, 'Unseen University')? ");
		String ccInstitutionName = keyboard.nextLine();
		
		Class.forName("com.mysql.jdbc.Driver");

		Properties config = DBUtil.getConfigProperties();
		
		Connection conn = DBUtil.connectToDatabaseServer(config, "cloudcoder.db");
		
		String dbName = config.getProperty("cloudcoder.db.databaseName");
		if (dbName == null) {
			throw new IllegalStateException("configuration properties do not define cloudcoder.db.databaseName");
		}
		
		System.out.println("Creating database");
		DBUtil.createDatabase(conn, dbName);
		
		conn.close();
		
		// Reconnect to the newly-created database
		conn = DBUtil.connectToDatabase(config, "cloudcoder.db");
		
		// Create tables and indexes
		createTable(conn, JDBCDatabase.CHANGES, Change.SCHEMA);
		createTable(conn, JDBCDatabase.CONFIGURATION_SETTINGS, ConfigurationSetting.SCHEMA);
		createTable(conn, JDBCDatabase.COURSES, Course.SCHEMA);
		createTable(conn, JDBCDatabase.COURSE_REGISTRATIONS, CourseRegistration.SCHEMA);
		createTable(conn, JDBCDatabase.EVENTS, Event.SCHEMA);
		createTable(conn, JDBCDatabase.PROBLEMS, Problem.SCHEMA);
		createTable(conn, JDBCDatabase.SUBMISSION_RECEIPTS, SubmissionReceipt.SCHEMA);
		createTable(conn, JDBCDatabase.TERMS, Term.SCHEMA);
		createTable(conn, JDBCDatabase.TEST_CASES, TestCase.SCHEMA);
		createTable(conn, JDBCDatabase.TEST_RESULTS, TestResult.SCHEMA);
		createTable(conn, JDBCDatabase.USERS, User.SCHEMA);
		
		// Create initial database contents
		
		// Set institution name (and any other configuration settings)
		System.out.println("Adding configuration settings...");
		ConfigurationSetting instName = new ConfigurationSetting();
		instName.setName(ConfigurationSettingName.PUB_TEXT_INSTITUTION);
		instName.setValue(ccInstitutionName);
		DBUtil.storeBean(conn, instName, ConfigurationSetting.SCHEMA, JDBCDatabase.CONFIGURATION_SETTINGS);
		
		// Terms
		System.out.println("Creating terms...");
		CreateWebappDatabase.storeTerm(conn, "Winter", 0);
		CreateWebappDatabase.storeTerm(conn, "Spring", 1);
		CreateWebappDatabase.storeTerm(conn, "Summer", 2);
		CreateWebappDatabase.storeTerm(conn, "Summer 1", 3);
		CreateWebappDatabase.storeTerm(conn, "Summer 2", 4);
		Term fall = CreateWebappDatabase.storeTerm(conn, "Fall", 5);
		
		// Create an initial demo course
		System.out.println("Creating demo course...");
		Course course = new Course();
		course.setName("CCDemo");
		course.setTitle("CloudCoder demo course");
		course.setTermId(fall.getId());
		course.setTerm(fall);
		course.setYear(2012);
		course.setUrl("http://cloudcoder.org/");
		DBUtil.storeBean(conn, course, Course.SCHEMA, JDBCDatabase.COURSES);
		
		// Create an initial user
		System.out.println("Creating initial user...");
		User user = new User();
		user.setUsername(ccUserName);
		user.setPasswordHash(BCrypt.hashpw(ccPassword, BCrypt.gensalt(12)));
		DBUtil.storeBean(conn, user, User.SCHEMA, JDBCDatabase.USERS);
		
		// Register the user as an instructor in the demo course
		System.out.println("Registering initial user for demo course...");
		CourseRegistration courseReg = new CourseRegistration();
		courseReg.setCourseId(course.getId());
		courseReg.setUserId(user.getId());
		courseReg.setRegistrationType(CourseRegistrationType.INSTRUCTOR);
		courseReg.setSection(101);
		DBUtil.storeBean(conn, courseReg, CourseRegistration.SCHEMA, JDBCDatabase.COURSE_REGISTRATIONS);
		
		// Create a Problem
		System.out.println("Creating hello, world problem in demo course...");
		Problem problem = new Problem();
		problem.setCourseId(course.getId());
		problem.setWhenAssigned(System.currentTimeMillis());
		problem.setWhenDue(problem.getWhenAssigned() + (24L*60*60*1000));
		problem.setVisible(true);
		problem.setProblemType(ProblemType.C_PROGRAM);
		problem.setTestname("hello");
		problem.setBriefDescription("Print hello, world");
		problem.setDescription(
				"<p>Print a line with the following text:</p>\n" +
				"<blockquote><pre>Hello, world</pre></blockquote>\n"
		);

		problem.setSkeleton(
				"#include <stdio.h>\n\n" +
				"int main(void) {\n" +
				"\t// TODO - add your code here\n\n" +
				"\treturn 0;\n" +
				"}\n"
				);
		problem.setSchemaVersion(Problem.CURRENT_SCHEMA_VERSION);
		problem.setAuthorName("A. User");
		problem.setAuthorEmail("auser@cs.unseen.edu");
		problem.setAuthorWebsite("http://cs.unseen.edu/~auser");
		problem.setTimestampUtc(System.currentTimeMillis());
		problem.setLicense(ProblemLicense.CC_ATTRIB_SHAREALIKE_3_0);
		
		DBUtil.storeBean(conn, problem, Problem.SCHEMA, JDBCDatabase.PROBLEMS);
		
		// Add a TestCase
		System.out.println("Creating test case for hello, world problem...");
		TestCase testCase = new TestCase();
		testCase.setProblemId(problem.getProblemId());
		testCase.setTestCaseName("hello");
		testCase.setInput("");
		testCase.setOutput("^\\s*Hello\\s*,\\s*world\\s*$i");
		testCase.setSecret(false);
		
		DBUtil.storeBean(conn, testCase, TestCase.SCHEMA, JDBCDatabase.TEST_CASES);
		
		conn.close();
		
		System.out.println("Success!");
	}

	private static void createTable(Connection conn, String tableName, ModelObjectSchema schema) throws SQLException {
		System.out.println("Creating table " + tableName);
		DBUtil.createTable(conn, tableName, schema);
	}

	private static Term storeTerm(Connection conn, String name, int seq) throws SQLException {
		Term term = new Term();
		term.setName(name);
		term.setSeq(seq);
		DBUtil.storeBean(conn, term, Term.SCHEMA, JDBCDatabase.TERMS);
		return term;
	}
}